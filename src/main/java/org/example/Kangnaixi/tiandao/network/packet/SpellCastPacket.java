package org.example.Kangnaixi.tiandao.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.network.NetworkHandler;
import org.example.Kangnaixi.tiandao.spell.SpellData;
import org.example.Kangnaixi.tiandao.spell.SpellRegistry;
import org.example.Kangnaixi.tiandao.cultivation.ExperienceConversionSystem;

import java.util.function.Supplier;

/**
 * 术法施放请求数据包
 * 客户端 -> 服务器：玩家请求施放术法
 */
public class SpellCastPacket {
    
    private final String spellId;
    
    public SpellCastPacket(String spellId) {
        this.spellId = spellId;
    }
    
    public SpellCastPacket(FriendlyByteBuf buf) {
        this.spellId = buf.readUtf();
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(spellId);
    }
    
    /**
     * 处理数据包（服务器端）
     */
    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            
            player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
                // 检查术法是否已解锁
                if (!cultivation.hasSpell(spellId)) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c你还没有解锁该术法！"));
                    return;
                }
                
                // 获取术法数据
                SpellData spell = SpellRegistry.getInstance().getSpellById(spellId);
                if (spell == null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c术法不存在: " + spellId));
                    return;
                }
                
                // 尝试施放术法
                boolean success = spell.cast(player, cultivation);
                
                if (success) {
                    // 触发经验转化（消耗的灵气转化为经验）
                    ExperienceConversionSystem.onSpiritConsumed(player, spell.getSpiritCost());
                    
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§a成功施放术法：" + spell.getName() + " §7(消耗 " + spell.getSpiritCost() + " 灵力)"
                    ));
                } else {
                    // 检查失败原因
                    if (spell.isOnCooldown()) {
                        int remaining = spell.getCooldownRemaining();
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§c术法冷却中，剩余 " + remaining + " 秒"
                        ));
                    } else if (cultivation.getSpiritPower() < spell.getSpiritCost()) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§c灵力不足！需要 " + spell.getSpiritCost() + " 点灵力"
                        ));
                    } else if (cultivation.getRealm().ordinal() < spell.getRequiredRealm().ordinal()) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§c境界不足！需要 " + spell.getRequiredRealm().getDisplayName()
                        ));
                    } else {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§c无法施放该术法"
                        ));
                    }
                }
                
                // 同步数据到客户端
                NetworkHandler.sendSpellDataToPlayer(new SpellDataSyncPacket(cultivation), player);
            });
        });
        context.setPacketHandled(true);
    }
}

