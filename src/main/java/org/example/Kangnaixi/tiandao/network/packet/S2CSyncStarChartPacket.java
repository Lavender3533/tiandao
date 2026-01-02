package org.example.Kangnaixi.tiandao.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.capability.StarChartCapability;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 服务端 -> 客户端：同步星盘数据
 * 建议位置: org.example.Kangnaixi.tiandao.network.packet
 */
public class S2CSyncStarChartPacket {
    private final Set<String> unlockedNodes;

    public S2CSyncStarChartPacket(StarChartCapability data) {
        this.unlockedNodes = data.getUnlockedNodes();
    }

    public S2CSyncStarChartPacket(Set<String> unlockedNodes) {
        this.unlockedNodes = new HashSet<>(unlockedNodes);
    }

    public S2CSyncStarChartPacket(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        this.unlockedNodes = new HashSet<>();
        for (int i = 0; i < size; i++) {
            unlockedNodes.add(buf.readUtf());
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(unlockedNodes.size());
        for (String nodeId : unlockedNodes) {
            buf.writeUtf(nodeId);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            // 更新客户端的Capability数据
            mc.player.getCapability(Tiandao.STAR_CHART_CAP).ifPresent(data -> {
                if (data instanceof StarChartCapability capability) {
                    capability.clearAll();
                    for (String nodeId : unlockedNodes) {
                        capability.unlockNode(nodeId);
                    }
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
