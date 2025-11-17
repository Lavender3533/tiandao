package org.example.Kangnaixi.tiandao.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.network.NetworkHandler;

import java.util.function.Supplier;

/**
 * 客户端 -> 服务器：将术法绑定到快捷栏槽位
 */
public class C2SHotbarBindPacket {

    private final int slotIndex;
    private final String spellId;

    public C2SHotbarBindPacket(int slotIndex, String spellId) {
        this.slotIndex = slotIndex;
        this.spellId = spellId;
    }

    public C2SHotbarBindPacket(FriendlyByteBuf buf) {
        this.slotIndex = buf.readInt();
        this.spellId = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(slotIndex);
        buf.writeUtf(spellId);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            player.getCapability(Tiandao.SPELL_HOTBAR_CAP).ifPresent(hotbar -> {
                // 绑定术法到槽位
                hotbar.setSlot(slotIndex, spellId);

                if (spellId == null || spellId.isEmpty()) {
                    player.sendSystemMessage(Component.literal(
                        "§7已清空槽位 " + (slotIndex + 1)
                    ));
                } else {
                    player.sendSystemMessage(Component.literal(
                        "§a已将 §e" + spellId + " §a绑定到槽位 " + (slotIndex + 1)
                    ));
                }

                NetworkHandler.sendSpellHotbarSyncToPlayer(hotbar, player);
            });
        });
        context.setPacketHandled(true);
    }
}
