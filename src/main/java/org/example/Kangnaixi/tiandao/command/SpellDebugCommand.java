package org.example.Kangnaixi.tiandao.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.example.Kangnaixi.tiandao.spell.debug.SpellDebugConfig;

public class SpellDebugCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("spelldebug")
                .requires(stack -> stack.hasPermission(2))
                .then(Commands.literal("targets")
                    .then(Commands.literal("on").executes(ctx -> setTargets(ctx, true)))
                    .then(Commands.literal("off").executes(ctx -> setTargets(ctx, false)))
                    .then(Commands.literal("toggle").executes(SpellDebugCommand::toggleTargets))
                    .executes(ctx -> report(ctx)))
        );
    }

    private static int setTargets(CommandContext<CommandSourceStack> ctx, boolean value) {
        SpellDebugConfig.setShowTargets(value);
        ctx.getSource().sendSuccess(() ->
            Component.literal("Target debug particles " + (value ? "enabled" : "disabled")), false);
        return 1;
    }

    private static int toggleTargets(CommandContext<CommandSourceStack> ctx) {
        SpellDebugConfig.toggleTargets();
        ctx.getSource().sendSuccess(() ->
            Component.literal("Target debug particles " +
                (SpellDebugConfig.isShowTargets() ? "enabled" : "disabled")), false);
        return 1;
    }

    private static int report(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() ->
            Component.literal("Target debug particles currently " +
                (SpellDebugConfig.isShowTargets() ? "ON" : "OFF")), false);
        return 1;
    }
}

