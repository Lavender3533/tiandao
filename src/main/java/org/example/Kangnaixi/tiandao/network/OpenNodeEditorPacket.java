package org.example.Kangnaixi.tiandao.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.Kangnaixi.tiandao.client.gui.NodeSpellEditorScreen;

import java.util.function.Supplier;

/**
 * 打开节点术法编辑器GUI的数据包
 * 服务器 -> 客户端
 */
public class OpenNodeEditorPacket {

    public OpenNodeEditorPacket() {
        // 空构造函数，不需要传递数据
    }

    public OpenNodeEditorPacket(FriendlyByteBuf buf) {
        // 空解码器，不需要读取数据
    }

    public void encode(FriendlyByteBuf buf) {
        // 空编码器，不需要写入数据
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // 在客户端打开GUI
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.setScreen(new NodeSpellEditorScreen());
        });
        context.setPacketHandled(true);
    }
}
