package org.example.Kangnaixi.tiandao.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.item.SpellJadeSlipItem;
import org.example.Kangnaixi.tiandao.spell.SpellLocalization;
import org.example.Kangnaixi.tiandao.spell.blueprint.SpellBlueprint;
import org.example.Kangnaixi.tiandao.spell.blueprint.SpellBlueprintLibrary;

/**
 * 单独的术法蓝图命令注册器，避免修改大型 TiandaoCommand。
 */
public class SpellBlueprintCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        SpellBlueprintLibrary.init();
        dispatcher.register(Commands.literal("tiandao")
            .then(Commands.literal("spell")
                .then(Commands.literal("blueprint")
                    .then(Commands.literal("give")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                            .then(Commands.argument("template", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    SpellBlueprintLibrary.getIds().forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> giveBlueprintSlip(
                                    ctx,
                                    EntityArgument.getPlayer(ctx, "player"),
                                    StringArgumentType.getString(ctx, "template")
                                ))))))
                .then(Commands.literal("list")
                    .executes(ctx -> listBlueprints(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> listBlueprints(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))));
    }

    private static int giveBlueprintSlip(CommandContext<CommandSourceStack> context,
                                         ServerPlayer target,
                                         String templateId) throws CommandSyntaxException {
        SpellBlueprint blueprint = SpellBlueprintLibrary.get(templateId);
        if (blueprint == null) {
            context.getSource().sendFailure(SpellLocalization.message("command.unknown_blueprint", templateId));
            return 0;
        }
        var stack = SpellJadeSlipItem.createSlip(blueprint);
        if (!target.addItem(stack)) {
            target.drop(stack, false);
        }
        context.getSource().sendSuccess(
            () -> SpellLocalization.message("command.give_success", target.getName().getString(), blueprint.getName()),
            true
        );
        return 1;
    }

    private static int listBlueprints(CommandSourceStack source, ServerPlayer target) {
        return target.getCapability(Tiandao.CULTIVATION_CAPABILITY).map(cultivation -> {
            java.util.List<SpellBlueprint> list = cultivation.getKnownBlueprints();
            if (list.isEmpty()) {
                source.sendSuccess(() -> SpellLocalization.message("command.list_empty", target.getName().getString()), false);
                return 0;
            }
            source.sendSuccess(() -> SpellLocalization.message("command.list_header", target.getName().getString()), false);
            for (SpellBlueprint blueprint : list) {
                source.sendSuccess(blueprint::getFormattedSummary, false);
            }
            return list.size();
        }).orElse(0);
    }
}
