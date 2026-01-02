package org.example.Kangnaixi.tiandao.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.spell.blueprint.SpellBlueprint;
import org.example.Kangnaixi.tiandao.spell.blueprint.SpellBlueprintLibrary;

import java.util.function.Supplier;

/**
 * 服务端→客户端：通知玩家学会新术法
 */
public class S2CSpellLearnedPacket {
    private final CompoundTag blueprintNbt;

    public S2CSpellLearnedPacket(SpellBlueprint blueprint) {
        this.blueprintNbt = blueprint.toNBT();
    }

    public S2CSpellLearnedPacket(FriendlyByteBuf buf) {
        this.blueprintNbt = buf.readNbt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(blueprintNbt);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            handleOnClient();
        });
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private void handleOnClient() {
        try {
            if (blueprintNbt == null) return;

            SpellBlueprint blueprint = SpellBlueprint.fromNBT(blueprintNbt);

            // 注册到客户端蓝图库
            SpellBlueprintLibrary.register(blueprint);

            Tiandao.LOGGER.info("客户端收到新术法: {} ({})",
                blueprint.getName(), blueprint.getId());

            // 可以在这里触发UI通知
            // ClientNotificationManager.showSpellLearned(blueprint);

        } catch (Exception e) {
            Tiandao.LOGGER.error("处理术法学习同步包失败", e);
        }
    }
}
