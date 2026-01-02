package org.example.Kangnaixi.tiandao.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.network.packet.*;
import org.example.Kangnaixi.tiandao.spell.runtime.hotbar.ISpellHotbar;

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

        INSTANCE.messageBuilder(SpellEditorLearnPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
            .decoder(SpellEditorLearnPacket::new)
            .encoder(SpellEditorLearnPacket::encode)
            .consumerMainThread(SpellEditorLearnPacket::handle)
            .add();

        INSTANCE.messageBuilder(SpellHotbarSyncPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
            .decoder(SpellHotbarSyncPacket::new)
            .encoder(SpellHotbarSyncPacket::encode)
            .consumerMainThread(SpellHotbarSyncPacket::handle)
            .add();

        INSTANCE.messageBuilder(SpellBlueprintCreatePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
            .decoder(SpellBlueprintCreatePacket::new)
            .encoder(SpellBlueprintCreatePacket::encode)
            .consumerMainThread(SpellBlueprintCreatePacket::handle)
            .add();

        INSTANCE.messageBuilder(OpenSpellEditorPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
            .decoder(OpenSpellEditorPacket::new)
            .encoder(OpenSpellEditorPacket::encode)
            .consumerMainThread(OpenSpellEditorPacket::handle)
            .add();

        // 术法快捷栏相关数据包
        INSTANCE.messageBuilder(C2SHotbarSelectPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
            .decoder(C2SHotbarSelectPacket::new)
            .encoder(C2SHotbarSelectPacket::encode)
            .consumerMainThread(C2SHotbarSelectPacket::handle)
            .add();

        INSTANCE.messageBuilder(C2SCastActiveSpellPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
            .decoder(C2SCastActiveSpellPacket::new)
            .encoder(C2SCastActiveSpellPacket::encode)
            .consumerMainThread(C2SCastActiveSpellPacket::handle)
            .add();

        INSTANCE.messageBuilder(C2SHotbarBindPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
            .decoder(C2SHotbarBindPacket::new)
            .encoder(C2SHotbarBindPacket::encode)
            .consumerMainThread(C2SHotbarBindPacket::handle)
            .add();

        // 星盘相关数据包
        INSTANCE.messageBuilder(C2SUnlockStarNodePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
            .decoder(C2SUnlockStarNodePacket::new)
            .encoder(C2SUnlockStarNodePacket::encode)
            .consumerMainThread(C2SUnlockStarNodePacket::handle)
            .add();

        INSTANCE.messageBuilder(S2CSyncStarChartPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
            .decoder(S2CSyncStarChartPacket::new)
            .encoder(S2CSyncStarChartPacket::encode)
            .consumerMainThread(S2CSyncStarChartPacket::handle)
            .add();

        // 手盘组合相关数据包
        INSTANCE.messageBuilder(C2SHandWheelCompilePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
            .decoder(C2SHandWheelCompilePacket::new)
            .encoder(C2SHandWheelCompilePacket::encode)
            .consumerMainThread(C2SHandWheelCompilePacket::handle)
            .add();

        INSTANCE.messageBuilder(S2CSpellLearnedPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
            .decoder(S2CSpellLearnedPacket::new)
            .encoder(S2CSpellLearnedPacket::encode)
            .consumerMainThread(S2CSpellLearnedPacket::handle)
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

    public static void sendSpellEditorLearnToServer(SpellEditorLearnPacket packet) {
        INSTANCE.sendToServer(packet);
    }

    public static void sendBlueprintCreateToServer(SpellBlueprintCreatePacket packet) {
        INSTANCE.sendToServer(packet);
    }

    public static void sendOpenSpellEditorToPlayer(OpenSpellEditorPacket packet, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendSpellHotbarSyncToPlayer(ISpellHotbar hotbar, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new SpellHotbarSyncPacket(hotbar));
    }

    // 术法快捷栏相关方法
    public static void sendHotbarSelectToServer(C2SHotbarSelectPacket packet) {
        INSTANCE.sendToServer(packet);
    }

    public static void sendCastActiveSpellToServer(C2SCastActiveSpellPacket packet) {
        INSTANCE.sendToServer(packet);
    }

    public static void sendHotbarBindToServer(C2SHotbarBindPacket packet) {
        INSTANCE.sendToServer(packet);
    }

    // 通用发送方法
    public static <T> void sendToServer(T packet) {
        INSTANCE.sendToServer(packet);
    }

    public static <T> void sendToPlayer(T packet, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
