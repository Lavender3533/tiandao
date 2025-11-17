package org.example.Kangnaixi.tiandao.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.network.NetworkHandler;

import java.util.function.Supplier;

/**
 * 客户端 -> 服务器：选择快捷栏槽位
 * 玩家按数字键 1-9 时发送
 */
public class C2SHotbarSelectPacket {

    private final int slotIndex;

    public C2SHotbarSelectPacket(int slotIndex) {
        this.slotIndex = slotIndex;
    }

    public C2SHotbarSelectPacket(FriendlyByteBuf buf) {
        this.slotIndex = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(slotIndex);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            player.getCapability(Tiandao.SPELL_HOTBAR_CAP).ifPresent(hotbar -> {
                // 设置激活槽位
                hotbar.setActiveIndex(slotIndex);

                // 获取该槽位的术法ID
                String spellId = hotbar.getSlot(slotIndex);
                if (spellId != null && !spellId.isEmpty()) {
                    player.sendSystemMessage(Component.literal(
                        "§a已切换到槽位 " + (slotIndex + 1) + ": §e" + spellId
                    ));
                } else {
                    player.sendSystemMessage(Component.literal(
                        "§7已切换到槽位 " + (slotIndex + 1) + " §c(空)"
                    ));
                }

                NetworkHandler.sendSpellHotbarSyncToPlayer(hotbar, player);
            });
        });
        context.setPacketHandled(true);
    }
}
