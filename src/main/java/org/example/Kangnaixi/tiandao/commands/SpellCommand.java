package org.example.Kangnaixi.tiandao.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.cultivation.ExperienceConversionSystem;
import org.example.Kangnaixi.tiandao.network.CultivationDataSyncPacket;
import org.example.Kangnaixi.tiandao.network.NetworkHandler;
import org.example.Kangnaixi.tiandao.spell.SpellRegistry;
import org.example.Kangnaixi.tiandao.spell.definition.SpellDefinition;
import org.example.Kangnaixi.tiandao.spell.runtime.SpellCastingService;

/**
 * /spell 快捷命令 - `/tiandao spell` 的简化版本
 * 提供常用术法操作的快捷方式
 */
public class SpellCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("spell")
                // /spell list
                .then(Commands.literal("list")
                    .executes(SpellCommand::spellList))
                // /spell cast <id>
                .then(Commands.literal("cast")
                    .then(Commands.argument("spellId", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            try {
                                ServerPlayer player = context.getSource().getPlayerOrException();
                                player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
                                    cultivation.getUnlockedSpells().forEach(builder::suggest);
                                });
                            } catch (Exception ignored) {
                            }
                            return builder.buildFuture();
                        })
                        .executes(SpellCommand::spellCast)))
                // /spell info <id>
                .then(Commands.literal("info")
                    .then(Commands.argument("spellId", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            SpellRegistry.getInstance().getAllSpellIds().forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(SpellCommand::spellInfo)))
        );
    }

    /**
     * 列出已解锁的术法
     */
    private static int spellList(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();

        player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            player.sendSystemMessage(Component.literal("§6§l=== 已解锁的术法 ==="));

            if (cultivation.getUnlockedSpells().isEmpty()) {
                player.sendSystemMessage(Component.literal("§7你还没有学习任何术法。"));
                player.sendSystemMessage(Component.literal("§7按 B 键打开术法编辑器创建术法。"));
                return;
            }

            int count = 0;
            for (String spellId : cultivation.getUnlockedSpells()) {
                SpellDefinition spell = SpellRegistry.getInstance().getSpellById(spellId);
                if (spell != null) {
                    count++;
                    player.sendSystemMessage(Component.literal(
                        String.format("§e%d. %s §7(%s)",
                            count,
                            spell.getMetadata().displayName(),
                            spellId)
                    ));
                    player.sendSystemMessage(Component.literal(
                        String.format("   §7伤害: §f%.1f §7| 灵力: §f%.1f §7| 冷却: §f%.1fs",
                            spell.getBaseStats().baseDamage(),
                            spell.getBaseStats().spiritCost(),
                            spell.getBaseStats().cooldownSeconds())
                    ));
                }
            }

            player.sendSystemMessage(Component.literal(""));
            player.sendSystemMessage(Component.literal(
                String.format("§7共 §e%d §7个术法 | 使用 §f/spell cast <id> §7施放", count)
            ));
        });

        return 1;
    }

    /**
     * 施放术法
     */
    private static int spellCast(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String spellId = StringArgumentType.getString(context, "spellId");

        player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            if (!cultivation.hasSpell(spellId)) {
                player.sendSystemMessage(Component.literal("§c你还没有解锁该术法"));
                player.sendSystemMessage(Component.literal("§7使用 §f/spell list §7查看已解锁的术法"));
                return;
            }

            SpellDefinition spell = SpellRegistry.getInstance().getSpellById(spellId);

            if (spell == null) {
                player.sendSystemMessage(Component.literal("§c术法不存在: " + spellId));
                return;
            }

            SpellCastingService.CastResult castResult =
                SpellCastingService.cast(player, cultivation, spell);

            if (castResult.success()) {
                double cost = castResult.runtimeResult().numbers().spiritCost();
                player.sendSystemMessage(Component.literal(
                    "§a施放术法: §e" + spell.getMetadata().displayName()
                        + " §7(消耗 " + String.format("%.1f", cost) + " 灵力)"
                ));
                ExperienceConversionSystem.onSpiritConsumed(player, cost);
                NetworkHandler.sendToPlayer(new CultivationDataSyncPacket(cultivation), player);
            } else if (castResult.failureReason() == SpellCastingService.CastResult.FailureReason.COOLDOWN) {
                player.sendSystemMessage(Component.literal(
                    "§c术法冷却中，剩余 " + castResult.cooldownRemaining() + " 秒"
                ));
            } else if (castResult.failureReason() == SpellCastingService.CastResult.FailureReason.SPIRIT) {
                player.sendSystemMessage(Component.literal(
                    "§c灵力不足！需要 " + String.format("%.1f", castResult.expectedSpirit())
                        + " 当前 " + String.format("%.1f", castResult.currentSpirit())
                ));
            } else {
                player.sendSystemMessage(Component.literal("§c术法施放失败"));
            }
        });

        return 1;
    }

    /**
     * 查看术法信息
     */
    private static int spellInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String spellId = StringArgumentType.getString(context, "spellId");

        SpellDefinition spell = SpellRegistry.getInstance().getSpellById(spellId);

        if (spell == null) {
            player.sendSystemMessage(Component.literal("§c术法不存在: " + spellId));
            return 0;
        }

        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§6§l=== 术法详情 ==="));
        player.sendSystemMessage(Component.literal("§e" + spell.getMetadata().displayName()));
        player.sendSystemMessage(Component.literal("§7" + spellId));
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§7" + spell.getMetadata().description()));
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§6基础属性:"));
        player.sendSystemMessage(Component.literal(
            String.format("  §7伤害: §f%.1f", spell.getBaseStats().baseDamage())
        ));
        player.sendSystemMessage(Component.literal(
            String.format("  §7灵力消耗: §f%.1f", spell.getBaseStats().spiritCost())
        ));
        player.sendSystemMessage(Component.literal(
            String.format("  §7冷却时间: §f%.1fs", spell.getBaseStats().cooldownSeconds())
        ));
        player.sendSystemMessage(Component.literal(
            String.format("  §7速度: §f%.1f", spell.getBaseStats().projectileSpeed())
        ));
        player.sendSystemMessage(Component.literal(
            String.format("  §7范围: §f%.1f", spell.getBaseStats().areaRange())
        ));
        player.sendSystemMessage(Component.literal(""));

        // 检查玩家是否已解锁
        player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            if (cultivation.hasSpell(spellId)) {
                player.sendSystemMessage(Component.literal("§a✓ 已解锁 §7- 使用 §f/spell cast " + spellId + " §7施放"));
            } else {
                player.sendSystemMessage(Component.literal("§c✗ 未解锁 §7- 需要先学习该术法"));
            }
        });

        return 1;
    }
}
