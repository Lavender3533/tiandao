package org.example.Kangnaixi.tiandao.network.packet;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.example.Kangnaixi.tiandao.spell.SpellRegistry;
import org.example.Kangnaixi.tiandao.spell.definition.SpellDefinitionExporter;

import java.util.function.Supplier;

public class SpellEditorSavePacket {

    private final String spellId;
    private final String json;

    public SpellEditorSavePacket(String spellId, String json) {
        this.spellId = spellId;
        this.json = json;
    }

    public SpellEditorSavePacket(FriendlyByteBuf buf) {
        this.spellId = buf.readUtf();
        this.json = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(spellId);
        buf.writeUtf(json);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            try {
                JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
                SpellDefinitionExporter.saveToConfig(spellId, jsonObject);
                SpellRegistry.getInstance().reload();
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a已保存术法 " + spellId));
            } catch (Exception ex) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c保存失败: " + ex.getMessage()));
            }
        });
        context.setPacketHandled(true);
    }
}
