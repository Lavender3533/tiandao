package org.example.Kangnaixi.tiandao.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.Kangnaixi.tiandao.network.NetworkHandler;
import org.example.Kangnaixi.tiandao.network.OpenNodeEditorPacket;

/**
 * 打开节点术法编辑器的命令
 * /nodeeditor - 打开编辑器
 */
public class OpenNodeEditorCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("nodeeditor")
                .executes(OpenNodeEditorCommand::openEditor)
        );
    }

    private static int openEditor(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        // 发送网络包给客户端打开GUI
        NetworkHandler.sendOpenNodeEditorToPlayer(new OpenNodeEditorPacket(), player);

        player.displayClientMessage(
            Component.literal("§a正在打开节点术法编辑器..."),
            false);

        return 1;
    }
}
