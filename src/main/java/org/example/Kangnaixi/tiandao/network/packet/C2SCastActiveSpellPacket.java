package org.example.Kangnaixi.tiandao.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.spell.runtime.engine.SpellRuntimeEngine;

import java.util.function.Supplier;

/**
 * 客户端 -> 服务器：施放术法
 * 玩家按 R 键时发送
 * 
 * 支持两种模式：
 * 1. 无参数 - 施放快捷栏当前选中的术法
 * 2. 带spellId - 施放指定ID的术法（蓝图）
 */
public class C2SCastActiveSpellPacket {

    private final String spellId;  // 为空时使用快捷栏

    public C2SCastActiveSpellPacket() {
        this.spellId = "";
    }

    public C2SCastActiveSpellPacket(String spellId) {
        this.spellId = spellId != null ? spellId : "";
    }

    public C2SCastActiveSpellPacket(FriendlyByteBuf buf) {
        this.spellId = buf.readUtf(256);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(spellId, 256);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            if (spellId != null && !spellId.isEmpty()) {
                // 施放指定ID的术法（蓝图）
                Tiandao.LOGGER.info("收到玩家 {} 的施法请求: {}", player.getScoreboardName(), spellId);
                SpellRuntimeEngine.castById(player, spellId);
            } else {
                // 施放快捷栏当前选中的术法
                Tiandao.LOGGER.info("收到玩家 {} 的R键施法请求", player.getScoreboardName());
                SpellRuntimeEngine.castFromHotbar(player);
            }
        });
        context.setPacketHandled(true);
    }
}
