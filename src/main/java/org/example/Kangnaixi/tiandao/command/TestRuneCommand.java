package org.example.Kangnaixi.tiandao.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.Kangnaixi.tiandao.spell.rune.Rune;
import org.example.Kangnaixi.tiandao.spell.rune.RuneChainExecutor;
import org.example.Kangnaixi.tiandao.spell.rune.RuneRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * 测试符文系统的命令
 */
public class TestRuneCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("testrune")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("list")
                    .executes(TestRuneCommand::listRunes))
                .then(Commands.literal("test1")
                    .executes(TestRuneCommand::testBasicDamage))
                .then(Commands.literal("test2")
                    .executes(TestRuneCommand::testBasicHeal))
                .then(Commands.literal("test3")
                    .executes(TestRuneCommand::testConeDamage))
        );
    }

    /**
     * 列出所有已注册的符文
     */
    private static int listRunes(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        source.sendSuccess(() -> Component.literal("§6===== 符文列表 ====="), false);

        RuneRegistry registry = RuneRegistry.getInstance();

        source.sendSuccess(() -> Component.literal("§b触发符文:"), false);
        for (Rune rune : registry.getTriggerRunes()) {
            source.sendSuccess(() -> Component.literal("  §7- §f" + rune.getName() + " §8(" + rune.getId() + ")"), false);
        }

        source.sendSuccess(() -> Component.literal("§b形状符文:"), false);
        for (Rune rune : registry.getShapeRunes()) {
            source.sendSuccess(() -> Component.literal("  §7- §f" + rune.getName() + " §8(" + rune.getId() + ")"), false);
        }

        source.sendSuccess(() -> Component.literal("§b效果符文:"), false);
        for (Rune rune : registry.getEffectRunes()) {
            source.sendSuccess(() -> Component.literal("  §7- §f" + rune.getName() + " §8(" + rune.getId() + ")"), false);
        }

        source.sendSuccess(() -> Component.literal("§6总计: §f" + registry.getRuneCount() + " 个符文"), false);

        return 1;
    }

    /**
     * 测试1: 自身 → 圆形 → 伤害
     */
    private static int testBasicDamage(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("§c只有玩家可以使用此命令"));
            return 0;
        }

        RuneRegistry registry = RuneRegistry.getInstance();

        // 构建符文链: 自身 → 圆形 → 伤害
        List<Rune> runeChain = new ArrayList<>();
        runeChain.add(registry.getRuneById("self"));
        runeChain.add(registry.getRuneById("circle"));
        runeChain.add(registry.getRuneById("damage"));

        // 验证符文链
        if (runeChain.contains(null)) {
            source.sendFailure(Component.literal("§c符文加载失败，请检查符文是否已注册"));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("§6===== 测试1: 基础伤害 ====="), false);
        source.sendSuccess(() -> Component.literal("§7符文链: 自身 → 圆形 → 伤害"), false);

        // 执行符文链
        RuneChainExecutor.ExecutionResult result = RuneChainExecutor.execute(runeChain, player);

        if (result.isSuccess()) {
            source.sendSuccess(() -> Component.literal("§a✓ 执行成功！"), false);
            if (result.getContext() != null) {
                int affected = result.getContext().getAffectedEntities().size();
                source.sendSuccess(() -> Component.literal("§7影响实体数: " + affected), false);
            }
        } else {
            source.sendFailure(Component.literal("§c✗ 执行失败: " + result.getMessage()));
        }

        return 1;
    }

    /**
     * 测试2: 自身 → 圆形 → 治疗
     */
    private static int testBasicHeal(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("§c只有玩家可以使用此命令"));
            return 0;
        }

        RuneRegistry registry = RuneRegistry.getInstance();

        // 构建符文链: 自身 → 圆形 → 治疗
        List<Rune> runeChain = new ArrayList<>();
        runeChain.add(registry.getRuneById("self"));
        runeChain.add(registry.getRuneById("circle"));
        runeChain.add(registry.getRuneById("heal"));

        if (runeChain.contains(null)) {
            source.sendFailure(Component.literal("§c符文加载失败"));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("§6===== 测试2: 基础治疗 ====="), false);
        source.sendSuccess(() -> Component.literal("§7符文链: 自身 → 圆形 → 治疗"), false);

        RuneChainExecutor.ExecutionResult result = RuneChainExecutor.execute(runeChain, player);

        if (result.isSuccess()) {
            source.sendSuccess(() -> Component.literal("§a✓ 执行成功！"), false);
        } else {
            source.sendFailure(Component.literal("§c✗ 执行失败: " + result.getMessage()));
        }

        return 1;
    }

    /**
     * 测试3: 自身 → 扇形 → 伤害
     */
    private static int testConeDamage(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("§c只有玩家可以使用此命令"));
            return 0;
        }

        RuneRegistry registry = RuneRegistry.getInstance();

        // 构建符文链: 自身 → 扇形 → 伤害
        List<Rune> runeChain = new ArrayList<>();
        runeChain.add(registry.getRuneById("self"));
        runeChain.add(registry.getRuneById("cone"));
        runeChain.add(registry.getRuneById("damage"));

        if (runeChain.contains(null)) {
            source.sendFailure(Component.literal("§c符文加载失败"));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("§6===== 测试3: 扇形伤害 ====="), false);
        source.sendSuccess(() -> Component.literal("§7符文链: 自身 → 扇形 → 伤害"), false);

        RuneChainExecutor.ExecutionResult result = RuneChainExecutor.execute(runeChain, player);

        if (result.isSuccess()) {
            source.sendSuccess(() -> Component.literal("§a✓ 执行成功！"), false);
        } else {
            source.sendFailure(Component.literal("§c✗ 执行失败: " + result.getMessage()));
        }

        return 1;
    }
}
