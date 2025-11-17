package org.example.Kangnaixi.tiandao.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.network.packet.*;

/**
 * Forge 网络通道注册与发送工具。
 */
public final class NetworkHandler {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
        ResourceLocation.fromNamespaceAndPath(Tiandao.MODID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    private NetworkHandler() {}

    private static int id() {
        return packetId++;
    }

    public static void register() {
        INSTANCE.messageBuilder(CultivationDataSyncPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
            .decoder(CultivationDataSyncPacket::new)
            .encoder(CultivationDataSyncPacket::encode)
            .consumerMainThread(CultivationDataSyncPacket::handle)
            .add();

        INSTANCE.messageBuilder(SpellDataSyncPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
            .decoder(SpellDataSyncPacket::new)
            .encoder(SpellDataSyncPacket::encode)
            .consumerMainThread(SpellDataSyncPacket::handle)
            .add();

        INSTANCE.messageBuilder(SpellCastPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
            .decoder(SpellCastPacket::new)
            .encoder(SpellCastPacket::encode)
            .consumerMainThread(SpellCastPacket::handle)
            .add();

        INSTANCE.messageBuilder(SpellHotbarSetPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
            .decoder(SpellHotbarSetPacket::new)
            .encoder(SpellHotbarSetPacket::encode)
            .consumerMainThread(SpellHotbarSetPacket::handle)
            .add();

        INSTANCE.messageBuilder(SpellEditorSavePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
            .decoder(SpellEditorSavePacket::new)
            .encoder(SpellEditorSavePacket::encode)
            .consumerMainThread(SpellEditorSavePacket::handle)
            .add();

        INSTANCE.messageBuilder(SpellBlueprintCreatePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
            .decoder(SpellBlueprintCreatePacket::new)
            .encoder(SpellBlueprintCreatePacket::encode)
            .consumerMainThread(SpellBlueprintCreatePacket::handle)
            .add();

        INSTANCE.messageBuilder(OpenRuneEditorPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
            .decoder(OpenRuneEditorPacket::new)
            .encoder(OpenRuneEditorPacket::encode)
            .consumerMainThread(OpenRuneEditorPacket::handle)
            .add();

        INSTANCE.messageBuilder(OpenNodeEditorPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
            .decoder(OpenNodeEditorPacket::new)
            .encoder(OpenNodeEditorPacket::encode)
            .consumerMainThread(OpenNodeEditorPacket::handle)
            .add();

        INSTANCE.messageBuilder(OpenSpellEditorPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
            .decoder(OpenSpellEditorPacket::new)
            .encoder(OpenSpellEditorPacket::encode)
            .consumerMainThread(OpenSpellEditorPacket::handle)
            .add();

        Tiandao.LOGGER.info("网络数据包注册完成");
    }

    public static void sendToPlayer(CultivationDataSyncPacket packet, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendSpellDataToPlayer(SpellDataSyncPacket packet, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendSpellCastToServer(SpellCastPacket packet) {
        INSTANCE.sendToServer(packet);
    }

    public static void sendSpellHotbarSetToServer(SpellHotbarSetPacket packet) {
        INSTANCE.sendToServer(packet);
    }

    public static void sendSpellEditorSaveToServer(SpellEditorSavePacket packet) {
        INSTANCE.sendToServer(packet);
    }

    public static void sendBlueprintCreateToServer(SpellBlueprintCreatePacket packet) {
        INSTANCE.sendToServer(packet);
    }

    public static void sendOpenRuneEditorToPlayer(OpenRuneEditorPacket packet, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendOpenNodeEditorToPlayer(OpenNodeEditorPacket packet, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendOpenSpellEditorToPlayer(OpenSpellEditorPacket packet, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
