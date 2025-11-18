package org.example.Kangnaixi.tiandao.network.packet;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.spell.SpellRegistry;
import org.example.Kangnaixi.tiandao.spell.definition.SpellDefinition;
import org.example.Kangnaixi.tiandao.spell.definition.SpellDefinitionExporter;
import org.example.Kangnaixi.tiandao.spell.definition.SpellDefinitionLoader;

import java.util.function.Supplier;

/**
 * 客户端在术法编辑器中点击"保存"时发送 SpellDefinition JSON,
 * 由服务器保存到配置文件、注册到 SpellRegistry 并添加到玩家的 Cultivation capability.
 */
public class SpellEditorLearnPacket {

    private final String definitionJson;

    public SpellEditorLearnPacket(String definitionJson) {
        this.definitionJson = definitionJson;
    }

    public SpellEditorLearnPacket(FriendlyByteBuf buf) {
        this.definitionJson = buf.readUtf(32767); // Max string length
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(definitionJson, 32767);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            try {
                // 解析 JSON
                JsonObject json = JsonParser.parseString(definitionJson).getAsJsonObject();

                // 使用 SpellDefinitionLoader 解析为 SpellDefinition
                SpellDefinitionLoader loader = new SpellDefinitionLoader();
                java.util.Optional<SpellDefinition> definitionOpt = loader.parseDefinition(json, "player_created");

                if (definitionOpt.isEmpty()) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c术法定义解析失败"));
                    return;
                }

                SpellDefinition definition = definitionOpt.get();
                String spellId = definition.getId().toString();

                // 保存到配置目录
                try {
                    SpellDefinitionExporter.saveToConfig(spellId, json);
                } catch (Exception e) {
                    Tiandao.LOGGER.warn("保存术法定义到配置文件失败: {}", e.getMessage());
                }

                // 注册到 SpellRegistry
                SpellRegistry.getInstance().registerSpell(definition);

                // 添加到玩家的 Cultivation capability
                player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
                    if (!cultivation.hasSpell(spellId)) {
                        cultivation.unlockSpell(spellId);
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§a成功创建并学习术法: §e" + definition.getMetadata().displayName()
                        ));
                    } else {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§a术法已更新: §e" + definition.getMetadata().displayName()
                        ));
                    }
                });

            } catch (Exception ex) {
                Tiandao.LOGGER.error("处理术法保存失败", ex);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c术法保存失败: " + ex.getMessage()));
            }
        });
        context.setPacketHandled(true);
    }
}
