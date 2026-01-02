package org.example.Kangnaixi.tiandao.spell.runtime.engine;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.cultivation.ExperienceConversionSystem;
import org.example.Kangnaixi.tiandao.network.CultivationDataSyncPacket;
import org.example.Kangnaixi.tiandao.network.NetworkHandler;
import org.example.Kangnaixi.tiandao.spell.blueprint.SpellBlueprint;
import org.example.Kangnaixi.tiandao.spell.blueprint.SpellBlueprintLibrary;
import org.example.Kangnaixi.tiandao.spell.runtime.AttributeType;
import org.example.Kangnaixi.tiandao.spell.runtime.EffectType;
import org.example.Kangnaixi.tiandao.spell.runtime.Spell;
import org.example.Kangnaixi.tiandao.spell.runtime.SpellExecutor;

/**
 * 新版模块化术法运行时引擎。
 *
 * 支持两种术法格式：
 * 1. SpellBlueprint（推荐）- 统一的术法成品格式
 * 2. Spell（旧版）- 保留兼容性
 */
public final class SpellRuntimeEngine {

    private SpellRuntimeEngine() {}

    // ==================== Blueprint 执行入口（推荐） ====================

    /**
     * 施放Blueprint术法（主入口）
     *
     * @param player    施法者
     * @param blueprint 术法蓝图
     * @return 是否成功施放
     */
    public static boolean cast(ServerPlayer player, SpellBlueprint blueprint) {
        if (player == null || blueprint == null) {
            return false;
        }

        BlueprintExecutor.ExecutionResult result = BlueprintExecutor.execute(player, blueprint);

        if (result.isSuccess()) {
            // 同步修炼数据
            player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
                ExperienceConversionSystem.onSpiritConsumed(player, result.getSpiritCost());
                NetworkHandler.sendToPlayer(new CultivationDataSyncPacket(cultivation), player);
            });

            player.sendSystemMessage(Component.literal(
                "§a施放术法: §e" + blueprint.getName() +
                " §7(消耗 " + String.format("%.1f", result.getSpiritCost()) + " 灵力)"
            ));

            return true;
        } else {
            player.sendSystemMessage(Component.literal("§c" + result.getMessage()));
            return false;
        }
    }

    /**
     * 通过ID施放Blueprint术法
     *
     * @param player      施法者
     * @param blueprintId 术法蓝图ID
     * @return 是否成功施放
     */
    public static boolean castById(ServerPlayer player, String blueprintId) {
        if (player == null || blueprintId == null || blueprintId.isEmpty()) {
            return false;
        }

        // 1. 从蓝图库查找
        SpellBlueprint blueprint = SpellBlueprintLibrary.get(blueprintId);

        // 2. 从玩家已学蓝图查找
        if (blueprint == null) {
            blueprint = player.getCapability(Tiandao.CULTIVATION_CAPABILITY)
                .map(cultivation -> cultivation.getKnownBlueprints().stream()
                    .filter(bp -> bp.getId().equals(blueprintId))
                    .findFirst()
                    .orElse(null))
                .orElse(null);
        }

        if (blueprint == null) {
            player.sendSystemMessage(Component.literal("§c未找到术法: " + blueprintId));
            return false;
        }

        return cast(player, blueprint);
    }

    // ==================== 旧版 Spell 执行（兼容性） ====================

    public static boolean canCast(ServerPlayer player, Spell spell) {
        // TODO 施法前检查（灵力、冷却、境界等）
        return true;
    }

    public static void execute(ServerPlayer player, Spell spell) {
        if (player == null || spell == null) {
            return;
        }

        try {
            SpellContext ctx = new SpellContext(player.serverLevel(), player, spell);

            SourceExecutors.get(spell.getSource()).apply(ctx);
            CarrierExecutors.get(spell.getCarrier()).apply(ctx);
            FormExecutors.get(spell.getForm()).apply(ctx);

            for (AttributeType attribute : spell.getAttributes()) {
                AttributeExecutors.get(attribute).apply(ctx);
            }
            for (EffectType effect : spell.getEffects()) {
                EffectExecutors.get(effect).apply(ctx);
            }

            Tiandao.LOGGER.debug("Spell [{}] executed by {} -> damage={}, range={}, speed={}",
                spell.getId(), player.getScoreboardName(), ctx.getDamage(), ctx.getRange(), ctx.getSpeed());
        } catch (Exception e) {
            Tiandao.LOGGER.error("术法执行失败: " + spell.getId(), e);
            player.sendSystemMessage(Component.literal("§c术法执行出错: " + e.getMessage()));
        }
    }

    /**
     * 从快捷栏施法。
     * 优先使用新系统（SpellDefinition + SpellCastingService），
     * 回退到旧系统（Spell + SpellExecutor）保持兼容性。
     */
    public static boolean castFromHotbar(ServerPlayer player) {
        if (player == null) {
            return false;
        }

        return player.getCapability(Tiandao.SPELL_HOTBAR_CAP).map(hotbar -> {
            String spellId = hotbar.getActiveSpellId();
            if (spellId == null || spellId.isEmpty()) {
                player.sendSystemMessage(Component.literal("§c当前槽位没有绑定术法。"));
                return false;
            }

            return player.getCapability(Tiandao.CULTIVATION_CAPABILITY).map(cultivation -> {
                // ==================== 优先使用新系统 ====================
                // 从SpellRegistry获取SpellDefinition
                org.example.Kangnaixi.tiandao.spell.definition.SpellDefinition definition =
                    org.example.Kangnaixi.tiandao.spell.SpellRegistry.getInstance().getSpellById(spellId);

                // 如果找到SpellDefinition且玩家已解锁，使用新系统
                if (definition != null && cultivation.hasSpell(spellId)) {
                    org.example.Kangnaixi.tiandao.spell.runtime.SpellCastingService.CastResult castResult =
                        org.example.Kangnaixi.tiandao.spell.runtime.SpellCastingService.cast(player, cultivation, definition);

                    if (castResult.success()) {
                        double cost = castResult.runtimeResult().numbers().spiritCost();
                        player.sendSystemMessage(Component.literal(
                            "§a施放术法: §e" + definition.getMetadata().displayName()
                                + " §7(消耗 " + String.format("%.1f", cost) + " 灵力)"
                        ));
                        ExperienceConversionSystem.onSpiritConsumed(player, cost);
                        NetworkHandler.sendToPlayer(new CultivationDataSyncPacket(cultivation), player);
                        Tiandao.LOGGER.info("玩家 {} 通过快捷栏施放术法: {} ({})",
                            player.getScoreboardName(), definition.getMetadata().displayName(), spellId);
                        return true;
                    } else if (castResult.failureReason() == org.example.Kangnaixi.tiandao.spell.runtime.SpellCastingService.CastResult.FailureReason.COOLDOWN) {
                        player.sendSystemMessage(Component.literal(
                            "§c术法冷却中，剩余 " + castResult.cooldownRemaining() + " 秒"
                        ));
                        return false;
                    } else if (castResult.failureReason() == org.example.Kangnaixi.tiandao.spell.runtime.SpellCastingService.CastResult.FailureReason.SPIRIT) {
                        player.sendSystemMessage(Component.literal(
                            "§c灵力不足！需要 " + String.format("%.1f", castResult.expectedSpirit())
                                + " 当前 " + String.format("%.1f", castResult.currentSpirit())
                        ));
                        return false;
                    } else {
                        player.sendSystemMessage(Component.literal("§c术法施放失败"));
                        return false;
                    }
                }

                // ==================== 回退到旧系统（兼容性） ====================
                return player.getCapability(Tiandao.PLAYER_SPELLS_CAP).map(spells -> {
                    Spell spell = spells.getSpells().stream()
                        .filter(s -> s.getId().equals(spellId))
                        .findFirst()
                        .orElse(null);

                    if (spell == null) {
                        player.sendSystemMessage(Component.literal(
                            "§c未找到术法 " + spellId + " §7(请先学习该术法)"
                        ));
                        return false;
                    }

                    if (!canCast(player, spell)) {
                        player.sendSystemMessage(Component.literal("§c当前无法施放该术法。"));
                        return false;
                    }

                    boolean paid = cultivation.getSpiritPower() >= SpellExecutor.estimateSpiritCost(spell)
                        && cultivation.consumeSpiritPower(SpellExecutor.estimateSpiritCost(spell));

                    if (!paid) {
                        double spiritCost = SpellExecutor.estimateSpiritCost(spell);
                        player.sendSystemMessage(Component.literal(
                            "§c灵力不足！需要 " + String.format("%.1f", spiritCost)
                                + " 当前 " + String.format("%.1f", cultivation.getSpiritPower())
                        ));
                        return false;
                    }

                    double cost = SpellExecutor.estimateSpiritCost(spell);
                    ExperienceConversionSystem.onSpiritConsumed(player, cost);
                    NetworkHandler.sendToPlayer(new CultivationDataSyncPacket(cultivation), player);

                    execute(player, spell);
                    player.sendSystemMessage(Component.literal("§a施放术法: §e" + spell.getName()));
                    Tiandao.LOGGER.info("玩家 {} 通过快捷栏施放旧系统术法: {}", player.getScoreboardName(), spell.getName());
                    return true;

                }).orElse(false);

            }).orElse(false);

        }).orElse(false);
    }
}
