package org.example.Kangnaixi.tiandao.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.capability.StarChartCapability;
import org.example.Kangnaixi.tiandao.network.NetworkHandler;
import org.example.Kangnaixi.tiandao.starchart.StarTestNodes;

import java.util.function.Supplier;

/**
 * 客户端 -> 服务端：请求解锁星盘节点
 * 建议位置: org.example.Kangnaixi.tiandao.network.packet
 */
public class C2SUnlockStarNodePacket {
    private final String nodeId;

    public C2SUnlockStarNodePacket(String nodeId) {
        this.nodeId = nodeId;
    }

    public C2SUnlockStarNodePacket(FriendlyByteBuf buf) {
        this.nodeId = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(nodeId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // 验证节点是否存在
            if (StarTestNodes.getNodeById(nodeId) == null) {
                Tiandao.LOGGER.warn("Player {} tried to unlock invalid node: {}", player.getName().getString(), nodeId);
                return;
            }

            // 获取Capability并解锁
            player.getCapability(Tiandao.STAR_CHART_CAP).ifPresent(data -> {
                if (data instanceof StarChartCapability capability) {
                    boolean unlocked = capability.unlockNode(nodeId);
                    if (unlocked) {
                        // 播放音效
                        player.level().playSound(
                            null,
                            player.blockPosition(),
                            SoundEvents.EXPERIENCE_ORB_PICKUP,
                            SoundSource.PLAYERS,
                            0.8f,
                            1.2f
                        );

                        // 同步给客户端
                        NetworkHandler.sendToPlayer(new S2CSyncStarChartPacket(capability), player);
                    }
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
