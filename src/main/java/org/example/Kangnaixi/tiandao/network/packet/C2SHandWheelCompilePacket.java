package org.example.Kangnaixi.tiandao.network.packet;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.handwheel.HandWheelCombination;
import org.example.Kangnaixi.tiandao.handwheel.HandWheelManager;
import org.example.Kangnaixi.tiandao.network.NetworkHandler;
import org.example.Kangnaixi.tiandao.spell.blueprint.SpellBlueprint;
import org.example.Kangnaixi.tiandao.spell.blueprint.SpellBlueprintLibrary;

import java.util.function.Supplier;

/**
 * 客户端→服务端：请求编译手盘组合为蓝图
 *
 * 流程：
 * 1. 客户端发送当前组合数据
 * 2. 服务端验证并编译
 * 3. 服务端保存蓝图到玩家数据
 * 4. 服务端同步结果给客户端
 */
public class C2SHandWheelCompilePacket {
    private final CompoundTag combinationData;
    private final String customName;

    public C2SHandWheelCompilePacket(HandWheelCombination combination, String customName) {
        this.combinationData = combination.serializeNBT();
        this.customName = customName != null ? customName : "";
    }

    public C2SHandWheelCompilePacket(FriendlyByteBuf buf) {
        this.combinationData = buf.readNbt();
        this.customName = buf.readUtf(256);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(combinationData);
        buf.writeUtf(customName, 256);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            try {
                // 1. 反序列化组合数据
                HandWheelCombination combination = new HandWheelCombination();
                combination.deserializeNBT(combinationData);

                // 2. 创建服务端Manager并编译
                HandWheelManager serverManager = new HandWheelManager();

                // 重建组合（需要验证节点解锁状态）
                var effectNode = combination.getEffectSlot().getNode();
                var formNode = combination.getFormSlot().getNode();

                if (effectNode == null || formNode == null) {
                    player.sendSystemMessage(Component.literal("§c组合无效：缺少必要节点"));
                    return;
                }

                // 验证解锁状态
                player.getCapability(Tiandao.STAR_CHART_CAP).ifPresent(starChart -> {
                    boolean allUnlocked =
                        starChart.isNodeUnlocked(effectNode.getId()) &&
                        starChart.isNodeUnlocked(formNode.getId());

                    if (!allUnlocked) {
                        player.sendSystemMessage(Component.literal("§c组合无效：包含未解锁节点"));
                        return;
                    }

                    // 3. 设置槽位并编译（只需要效果和形态）
                    serverManager.setSlot(effectNode.getCategory(), effectNode.getId(), player);
                    serverManager.setSlot(formNode.getCategory(), formNode.getId(), player);

                    // 添加调制节点
                    for (var mod : combination.getActiveModifiers()) {
                        if (starChart.isNodeUnlocked(mod.getId())) {
                            serverManager.setSlot(mod.getCategory(), mod.getId(), player);
                        }
                    }

                    // 4. 编译并保存
                    HandWheelManager.CompileResult result = serverManager.compileAndSave(player);

                    if (result.isSuccess()) {
                        SpellBlueprint blueprint = result.getBlueprint();
                        player.sendSystemMessage(Component.literal(
                            "§a术法组合成功！§e" + blueprint.getName() +
                            " §7(威力: " + String.format("%.1f", blueprint.getBasePower()) +
                            " | 消耗: " + String.format("%.1f", blueprint.getSpiritCost()) + ")"
                        ));

                        // 5. 同步蓝图到客户端
                        NetworkHandler.sendToPlayer(
                            new S2CSpellLearnedPacket(blueprint),
                            player
                        );
                    } else {
                        player.sendSystemMessage(Component.literal(
                            "§c术法组合失败：" + result.getMessage()
                        ));
                    }
                });

            } catch (Exception e) {
                Tiandao.LOGGER.error("处理手盘编译包时出错", e);
                player.sendSystemMessage(Component.literal("§c术法组合出错：" + e.getMessage()));
            }
        });

        ctx.get().setPacketHandled(true);
    }
}
