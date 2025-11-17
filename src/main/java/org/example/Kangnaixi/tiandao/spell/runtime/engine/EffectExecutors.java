package org.example.Kangnaixi.tiandao.spell.runtime.engine;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.example.Kangnaixi.tiandao.spell.runtime.EffectType;

import java.util.EnumMap;
import java.util.Map;

/**
 * 术法效果执行器注册器
 */
public final class EffectExecutors {

    private static final Map<EffectType, EffectExecutor> EXECUTORS = new EnumMap<>(EffectType.class);

    static {
        // 注册基础实现
        register(EffectType.AOE_UP, EffectExecutors::applyAoeUp);
        register(EffectType.SHIELD, EffectExecutors::applyShield);
        register(EffectType.HEAL_UP, EffectExecutors::applyHealUp);

        // 暂未实现的效果
        register(EffectType.ARMOR_BREAK, EffectExecutor.NO_OP);
        register(EffectType.KNOCKBACK, EffectExecutor.NO_OP);
        register(EffectType.DOT, EffectExecutor.NO_OP);
        register(EffectType.PENETRATE, EffectExecutor.NO_OP);
        register(EffectType.EXPLODE, EffectExecutor.NO_OP);
        register(EffectType.SLOW, EffectExecutor.NO_OP);
        register(EffectType.TRACK, EffectExecutor.NO_OP);
        register(EffectType.BOOMERANG, EffectExecutor.NO_OP);
        register(EffectType.SPREAD, EffectExecutor.NO_OP);
        register(EffectType.MOVE_SPEED, EffectExecutor.NO_OP);
        register(EffectType.LIFESTEAL, EffectExecutor.NO_OP);
    }

    /**
     * 注册效果执行器
     */
    public static void register(EffectType type, EffectExecutor executor) {
        EXECUTORS.put(type, executor);
    }

    /**
     * 获取效果执行器
     */
    public static EffectExecutor get(EffectType type) {
        return EXECUTORS.getOrDefault(type, EffectExecutor.NO_OP);
    }

    // ========== 基础实现 ==========

    /**
     * AOE提升：增加范围50%
     */
    private static void applyAoeUp(SpellContext ctx) {
        ctx.setRange(ctx.getRange() * 1.5);
        ctx.setRadius(ctx.getRadius() * 1.5);
        ctx.putData("effect.aoe_up", true);
    }

    /**
     * 护盾：给施法者添加吸收伤害效果
     */
    private static void applyShield(SpellContext ctx) {
        // 如果是增益型载体，直接应用到施法者
        if (ctx.hasData("carrier.buff")) {
            ctx.getCaster().addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1));
        }
        ctx.putData("effect.shield", true);
    }

    /**
     * 治疗提升：给施法者添加生命恢复效果
     */
    private static void applyHealUp(SpellContext ctx) {
        if (ctx.hasData("carrier.buff")) {
            ctx.getCaster().addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
        }
        ctx.putData("effect.heal_up", true);
    }

    private EffectExecutors() {}
}
