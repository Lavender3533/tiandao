package org.example.Kangnaixi.tiandao.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.Kangnaixi.tiandao.spell.runtime.*;
import org.example.Kangnaixi.tiandao.spell.runtime.engine.SpellRuntimeEngine;

import java.util.List;
import java.util.Optional;

public final class SpellRuntimeCommand {

    private SpellRuntimeCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("spellruntime")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("give_demo")
                .executes(ctx -> giveDemoSpell(ctx, ctx.getSource().getPlayerOrException()))
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(ctx -> giveDemoSpell(ctx, EntityArgument.getPlayer(ctx, "target")))))
            .then(Commands.literal("list")
                .executes(ctx -> listSpells(ctx, ctx.getSource().getPlayerOrException()))
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(ctx -> listSpells(ctx, EntityArgument.getPlayer(ctx, "target")))))
            .then(Commands.literal("set_active")
                .then(Commands.argument("spellId", StringArgumentType.word())
                    .executes(ctx -> setActiveSpell(ctx, ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "spellId")))
                    .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> setActiveSpell(ctx, EntityArgument.getPlayer(ctx, "target"), StringArgumentType.getString(ctx, "spellId"))))))
            .then(Commands.literal("cast_active")
                .executes(ctx -> castActive(ctx, ctx.getSource().getPlayerOrException()))
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(ctx -> castActive(ctx, EntityArgument.getPlayer(ctx, "target"))))));
    }

    private static int giveDemoSpell(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        Spell demo = createDemoSpell();
        boolean added = PlayerSpellsHelper.addSpell(target, demo);
        if (!added) {
            ctx.getSource().sendFailure(Component.literal("未找到玩家术法能力，无法发放演示术法。"));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal("已向 " + target.getScoreboardName() + " 添加演示术法 " + demo.getName()), true);
        target.sendSystemMessage(Component.literal("获得演示术法：" + demo.getName()));
        return 1;
    }

    private static int listSpells(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        Optional<IPlayerSpells> resolved = PlayerSpellsHelper.get(target).resolve();
        if (resolved.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("未找到玩家术法能力。"));
            return 0;
        }
        var capability = resolved.get();
        ctx.getSource().sendSuccess(() -> Component.literal("--- " + target.getScoreboardName() + " 的术法列表 ---"), false);
        capability.getSpells().forEach(spell ->
            ctx.getSource().sendSuccess(() -> Component.literal(" - " + spell.getId() + " / " + spell.getName()), false)
        );
        Spell active = capability.getActiveSpell();
        if (active != null) {
            ctx.getSource().sendSuccess(() -> Component.literal("当前已选： " + active.getId()), false);
        } else {
            ctx.getSource().sendSuccess(() -> Component.literal("尚未选择激活术法"), false);
        }
        return capability.getSpells().size();
    }

    private static int setActiveSpell(CommandContext<CommandSourceStack> ctx, ServerPlayer target, String spellId) {
        boolean updated = PlayerSpellsHelper.setActive(target, spellId);
        if (!updated) {
            ctx.getSource().sendFailure(Component.literal("未找到匹配的术法：" + spellId));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal("已为 " + target.getScoreboardName() + " 激活术法 " + spellId), true);
        target.sendSystemMessage(Component.literal("当前术法已切换为 " + spellId));
        return 1;
    }

    private static int castActive(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        Optional<Spell> active = PlayerSpellsHelper.getActive(target);
        if (active.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("目标没有激活的术法。"));
            return 0;
        }
        SpellRuntimeEngine.execute(target, active.get());
        ctx.getSource().sendSuccess(() -> Component.literal("已在 " + target.getScoreboardName() + " 身上释放术法 " + active.get().getName()), true);
        return 1;
    }

    private static Spell createDemoSpell() {
        return new Spell(
            "demo/sword_qi",
            "演示剑气",
            SourceType.WEAPON_SWORD,
            CarrierType.SWORD_QI,
            FormType.INSTANT,
            List.of(AttributeType.METAL, AttributeType.SWORD_INTENT),
            List.of(EffectType.AOE_UP, EffectType.ARMOR_BREAK),
            14.0,
            35.0,
            4.0,
            24.0
        );
    }
}
