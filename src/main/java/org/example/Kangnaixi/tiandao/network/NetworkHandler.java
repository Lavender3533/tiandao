package org.example.Kangnaixi.tiandao.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.network.packet.SpellCastPacket;
import org.example.Kangnaixi.tiandao.network.packet.SpellDataSyncPacket;
import org.example.Kangnaixi.tiandao.network.packet.SpellHotbarSetPacket;

/**
 * 网络处理器
 * 用于注册和发送网络数据包
 */
public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
        ResourceLocation.fromNamespaceAndPath(Tiandao.MOD_ID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );
    
    private static int packetId = 0;
    
    private static int id() {
        return packetId++;
    }
    
    /**
     * 注册所有网络数据包
     */
    public static void register() {
        // 修仙数据同步包（服务器 -> 客户端）
        INSTANCE.messageBuilder(CultivationDataSyncPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
            .decoder(CultivationDataSyncPacket::new)
            .encoder(CultivationDataSyncPacket::encode)
            .consumerMainThread(CultivationDataSyncPacket::handle)
            .add();
        
        // 术法数据同步包（服务器 -> 客户端）
        INSTANCE.messageBuilder(SpellDataSyncPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
            .decoder(SpellDataSyncPacket::new)
            .encoder(SpellDataSyncPacket::encode)
            .consumerMainThread(SpellDataSyncPacket::handle)
            .add();
        
        // 术法施放请求包（客户端 -> 服务器）
        INSTANCE.messageBuilder(SpellCastPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
            .decoder(SpellCastPacket::new)
            .encoder(SpellCastPacket::encode)
            .consumerMainThread(SpellCastPacket::handle)
            .add();
        
        // 术法快捷栏设置包（客户端 -> 服务器）
        INSTANCE.messageBuilder(SpellHotbarSetPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
            .decoder(SpellHotbarSetPacket::new)
            .encoder(SpellHotbarSetPacket::encode)
            .consumerMainThread(SpellHotbarSetPacket::handle)
            .add();
        
        Tiandao.LOGGER.info("网络数据包已注册（修仙数据 + 术法系统）");
    }
    
    /**
     * 向指定玩家发送修仙数据同步包
     */
    public static void sendToPlayer(CultivationDataSyncPacket packet, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
    
    /**
     * 向指定玩家发送术法数据同步包
     */
    public static void sendSpellDataToPlayer(SpellDataSyncPacket packet, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
    
    /**
     * 向服务器发送术法施放请求
     */
    public static void sendSpellCastToServer(SpellCastPacket packet) {
        INSTANCE.sendToServer(packet);
    }
    
    /**
     * 向服务器发送术法快捷栏设置请求
     */
    public static void sendSpellHotbarSetToServer(SpellHotbarSetPacket packet) {
        INSTANCE.sendToServer(packet);
    }
}

