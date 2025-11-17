package org.example.Kangnaixi.tiandao.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.cultivation.ExperienceConversionSystem;
import org.example.Kangnaixi.tiandao.network.NetworkHandler;
import org.example.Kangnaixi.tiandao.spell.SpellRegistry;
import org.example.Kangnaixi.tiandao.spell.definition.SpellDefinition;
import org.example.Kangnaixi.tiandao.spell.runtime.SpellCastingService;

import java.util.function.Supplier;

/**
 * 客户端 -> 服务器：施法请求。
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

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
                if (!cultivation.hasSpell(spellId)) {
                    player.sendSystemMessage(Component.literal("§c你还没有解锁该术法！"));
                    return;
                }

                SpellDefinition spell = SpellRegistry.getInstance().getSpellById(spellId);
                if (spell == null) {
                    player.sendSystemMessage(Component.literal("§c术法不存在: " + spellId));
                    return;
                }

                SpellCastingService.CastResult castResult = SpellCastingService.cast(player, cultivation, spell);
                if (castResult.success()) {
                    double cost = castResult.runtimeResult().numbers().spiritCost();
                    ExperienceConversionSystem.onSpiritConsumed(player, cost);
                    player.sendSystemMessage(Component.literal(
                        "§a成功施放术法 §e" + spell.getMetadata().displayName()
                            + " §7(消耗 " + String.format("%.1f", cost) + " 灵力)"
                    ));
                } else {
                    handleFailure(player, spell, castResult);
                }

                NetworkHandler.sendSpellDataToPlayer(new SpellDataSyncPacket(cultivation), player);
            });
        });
        context.setPacketHandled(true);
    }

    private void handleFailure(ServerPlayer player, SpellDefinition spell, SpellCastingService.CastResult result) {
        if (result.failureReason() == SpellCastingService.CastResult.FailureReason.COOLDOWN) {
            player.sendSystemMessage(Component.literal("§c术法冷却中，剩余 "
                + result.cooldownRemaining() + " 秒"));
            return;
        }
        if (result.failureReason() == SpellCastingService.CastResult.FailureReason.SPIRIT) {
            player.sendSystemMessage(Component.literal(
                "§c灵力不足！需要 " + String.format("%.1f", result.expectedSpirit())
                    + " 当前 " + String.format("%.1f", result.currentSpirit())
            ));
            return;
        }
        player.sendSystemMessage(Component.literal(
            "§c无法施放术法: " + spell.getMetadata().displayName()
        ));
    }
}
