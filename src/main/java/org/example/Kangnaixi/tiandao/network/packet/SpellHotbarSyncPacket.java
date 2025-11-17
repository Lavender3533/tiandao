package org.example.Kangnaixi.tiandao.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.spell.runtime.hotbar.ISpellHotbar;

import java.util.function.Supplier;

/**
 * 服务器 -> 客户端: 同步术法快捷栏 Capability.
 */
public class SpellHotbarSyncPacket {

    private final String[] slots;
    private final int activeIndex;

    public SpellHotbarSyncPacket(ISpellHotbar hotbar) {
        this.slots = hotbar.getSlots();
        this.activeIndex = hotbar.getActiveIndex();
    }

    public SpellHotbarSyncPacket(FriendlyByteBuf buf) {
        int size = buf.readInt();
        this.slots = new String[size];
        for (int i = 0; i < size; i++) {
            this.slots[i] = buf.readBoolean() ? buf.readUtf() : null;
        }
        this.activeIndex = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(slots.length);
        for (String slot : slots) {
            if (slot != null && !slot.isEmpty()) {
                buf.writeBoolean(true);
                buf.writeUtf(slot);
            } else {
                buf.writeBoolean(false);
            }
        }
        buf.writeInt(activeIndex);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                return;
            }
            mc.player.getCapability(Tiandao.SPELL_HOTBAR_CAP).ifPresent(cap -> {
                for (int i = 0; i < slots.length; i++) {
                    cap.setSlot(i, slots[i]);
                }
                cap.setActiveIndex(activeIndex);
            });
        });
        context.setPacketHandled(true);
    }

}
