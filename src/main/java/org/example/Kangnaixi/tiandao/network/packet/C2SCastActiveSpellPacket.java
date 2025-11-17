package org.example.Kangnaixi.tiandao.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.spell.runtime.engine.SpellRuntimeEngine;

import java.util.function.Supplier;

/**
 * 客户端 -> 服务器：施放当前激活槽位的术法
 * 玩家按 R 键时发送
 */
public class C2SCastActiveSpellPacket {

    public C2SCastActiveSpellPacket() {
    }

    public C2SCastActiveSpellPacket(FriendlyByteBuf buf) {
        // 无需额外数据
    }

    public void encode(FriendlyByteBuf buf) {
        // 无需编码数据
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            Tiandao.LOGGER.info("收到玩家 {} 的R键施法请求", player.getScoreboardName());

            // 使用新的模块化引擎执行术法
            SpellRuntimeEngine.castFromHotbar(player);
        });
        context.setPacketHandled(true);
    }
}
