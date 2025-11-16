package org.example.Kangnaixi.tiandao.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.Kangnaixi.tiandao.network.NetworkHandler;
import org.example.Kangnaixi.tiandao.network.OpenRuneEditorPacket;

/**
 * 打开符文编辑器GUI的命令
 * /runeeditor - 打开符文编辑器
 */
public class OpenRuneEditorCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("runeeditor")
                .executes(OpenRuneEditorCommand::openEditor)
        );
    }

    private static int openEditor(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            // 发送数据包到客户端打开GUI
            NetworkHandler.sendOpenRuneEditorToPlayer(new OpenRuneEditorPacket(), player);

            player.sendSystemMessage(Component.literal("§a正在打开符文编辑器..."));
            return 1;
        }
        return 0;
    }
}
