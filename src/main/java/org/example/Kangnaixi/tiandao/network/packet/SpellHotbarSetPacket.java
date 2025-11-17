package org.example.Kangnaixi.tiandao.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.network.NetworkHandler;
import org.example.Kangnaixi.tiandao.spell.SpellRegistry;
import org.example.Kangnaixi.tiandao.spell.definition.SpellDefinition;

import java.util.function.Supplier;

/**
 * 客户端 -> 服务器：更新术法快捷栏。
 */
public class SpellHotbarSetPacket {

    private final int slot;
    private final String spellId;

    public SpellHotbarSetPacket(int slot, String spellId) {
        this.slot = slot;
        this.spellId = spellId;
    }

    public SpellHotbarSetPacket(FriendlyByteBuf buf) {
        this.slot = buf.readInt();
        this.spellId = buf.readBoolean() ? buf.readUtf() : null;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(slot);
        if (spellId != null) {
            buf.writeBoolean(true);
            buf.writeUtf(spellId);
        } else {
            buf.writeBoolean(false);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            if (slot < 0 || slot >= 4) {
                Tiandao.LOGGER.warn("玩家 {} 尝试写入非法术法槽: {}", player.getName().getString(), slot);
                return;
            }

            player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
                if (spellId != null && !cultivation.hasSpell(spellId)) {
                    player.sendSystemMessage(Component.literal("§c你还没有解锁该术法"));
                    return;
                }

                cultivation.setSpellHotbar(slot, spellId);
                if (spellId != null) {
                    SpellDefinition spell = SpellRegistry.getInstance().getSpellById(spellId);
                    if (spell != null) {
                        Tiandao.LOGGER.debug("玩家 {} 设置术法槽 [{}] -> {}", player.getName().getString(),
                            slot, spell.getMetadata().displayName());
                    }
                } else {
                    Tiandao.LOGGER.debug("玩家 {} 清空术法槽 [{}]", player.getName().getString(), slot);
                }

                NetworkHandler.sendSpellDataToPlayer(new SpellDataSyncPacket(cultivation), player);
            });
        });
        context.setPacketHandled(true);
    }
}
