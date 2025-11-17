package org.example.Kangnaixi.tiandao.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import org.example.Kangnaixi.tiandao.network.NetworkHandler;
import org.example.Kangnaixi.tiandao.network.packet.OpenSpellEditorPacket;

public class SpellEditorCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("spell_editor")
            .requires(source -> source.hasPermission(0))
            .executes(SpellEditorCommand::openEditor));
    }

    private static int openEditor(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            NetworkHandler.sendOpenSpellEditorToPlayer(new OpenSpellEditorPacket("tiandao:custom_spell"), player);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }
}
