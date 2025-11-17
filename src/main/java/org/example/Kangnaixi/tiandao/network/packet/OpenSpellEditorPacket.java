package org.example.Kangnaixi.tiandao.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.Kangnaixi.tiandao.client.gui.editor.SpellEditorScreen;
import org.example.Kangnaixi.tiandao.client.gui.editor.SpellEditorViewModel;

import java.util.function.Supplier;

public class OpenSpellEditorPacket {

    private final String presetId;

    public OpenSpellEditorPacket(String presetId) {
        this.presetId = presetId;
    }

    public OpenSpellEditorPacket(FriendlyByteBuf buf) {
        this.presetId = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(presetId);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            SpellEditorViewModel model = new SpellEditorViewModel();
            model.setSpellId(presetId);
            Minecraft.getInstance().setScreen(new SpellEditorScreen(model));
        });
        context.setPacketHandled(true);
    }
}
