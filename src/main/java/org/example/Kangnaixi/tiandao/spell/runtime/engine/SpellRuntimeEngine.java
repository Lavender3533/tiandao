package org.example.Kangnaixi.tiandao.spell.runtime.engine;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.spell.runtime.AttributeType;
import org.example.Kangnaixi.tiandao.spell.runtime.EffectType;
import org.example.Kangnaixi.tiandao.spell.runtime.Spell;

/**
 * 新版模块化术法运行时引擎
 *
 * 执行流程：
 * 1. Source（来源）→ 2. Carrier（载体）→ 3. Form（形式）
 * 4. Attributes（属性）→ 5. Effects（效果）
 */
public final class SpellRuntimeEngine {

    private SpellRuntimeEngine() {}

    /**
     * 检查玩家是否可以施放该术法
     *
     * @param player 玩家
     * @param spell 术法
     * @return 如果可以施法返回true，否则返回false
     */
    public static boolean canCast(ServerPlayer player, Spell spell) {
        // TODO: 后续添加检查逻辑：
        // - 灵力是否足够（spell.getBaseSpiritCost()）
        // - 冷却时间是否完成
        // - 境界是否满足要求
        // 目前始终返回true
        return true;
    }

    /**
     * 执行术法
     *
     * @param player 施法者
     * @param spell 术法定义
     */
    public static void execute(ServerPlayer player, Spell spell) {
        if (player == null || spell == null) {
            return;
        }

        try {
            // 创建术法上下文
            SpellContext ctx = new SpellContext(player.serverLevel(), player, spell);

            // 1. 应用来源效果
            SourceExecutors.get(spell.getSource()).apply(ctx);

            // 2. 应用载体效果
            CarrierExecutors.get(spell.getCarrier()).apply(ctx);

            // 3. 应用形式效果
            FormExecutors.get(spell.getForm()).apply(ctx);

            // 4. 应用属性效果
            for (AttributeType attribute : spell.getAttributes()) {
                AttributeExecutors.get(attribute).apply(ctx);
            }

            // 5. 应用特殊效果
            for (EffectType effect : spell.getEffects()) {
                EffectExecutors.get(effect).apply(ctx);
            }

            // 记录调试信息
            Tiandao.LOGGER.debug("Spell [{}] executed by {} -> damage={}, range={}, speed={}",
                spell.getId(), player.getScoreboardName(), ctx.getDamage(), ctx.getRange(), ctx.getSpeed());

        } catch (Exception e) {
            Tiandao.LOGGER.error("术法执行失败: " + spell.getId(), e);
            player.sendSystemMessage(Component.literal("§c术法执行出错: " + e.getMessage()));
        }
    }

    /**
     * 从快捷栏施放术法
     *
     * @param player 施法者
     * @return 如果成功施法返回true，否则返回false
     */
    public static boolean castFromHotbar(ServerPlayer player) {
        if (player == null) {
            return false;
        }

        // 从快捷栏能力中获取激活的术法ID
        return player.getCapability(Tiandao.SPELL_HOTBAR_CAP).map(hotbar -> {
            String spellId = hotbar.getActiveSpellId();

            if (spellId == null || spellId.isEmpty()) {
                player.sendSystemMessage(Component.literal("§c当前槽位没有绑定术法！"));
                return false;
            }

            // 从术法库中查找术法
            return player.getCapability(Tiandao.PLAYER_SPELLS_CAP).map(spells -> {
                Spell spell = spells.getSpells().stream()
                    .filter(s -> s.getId().equals(spellId))
                    .findFirst()
                    .orElse(null);

                if (spell == null) {
                    player.sendSystemMessage(Component.literal(
                        "§c未找到术法: " + spellId + " §7(请先学习该术法)"
                    ));
                    return false;
                }

                // 检查是否可以施法
                if (!canCast(player, spell)) {
                    player.sendSystemMessage(Component.literal("§c当前无法施放该术法"));
                    return false;
                }

                // 执行术法
                execute(player, spell);
                player.sendSystemMessage(Component.literal("§a施放术法: §e" + spell.getName()));
                Tiandao.LOGGER.info("成功施放术法: {}", spell.getName());
                return true;

            }).orElse(false);

        }).orElse(false);
    }
}
