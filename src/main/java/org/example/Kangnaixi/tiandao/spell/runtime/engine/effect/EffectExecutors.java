package org.example.Kangnaixi.tiandao.spell.runtime.engine.effect;

import org.example.Kangnaixi.tiandao.spell.runtime.EffectType;
import org.example.Kangnaixi.tiandao.spell.runtime.engine.SpellContext;

import java.util.EnumMap;
import java.util.Map;

public final class EffectExecutors {

    private static final Map<EffectType, EffectExecutor> EXECUTORS = new EnumMap<>(EffectType.class);

    static {
        register(EffectType.ARMOR_BREAK, EffectExecutors::applyArmorBreak);
        register(EffectType.AOE_UP, EffectExecutors::applyAreaBoost);
        register(EffectType.KNOCKBACK, ctx -> ctx.put("effect.knockback", Boolean.TRUE));
        register(EffectType.MOVE_SPEED, ctx -> ctx.put("effect.speed_boost", Boolean.TRUE));
        register(EffectType.SHIELD, ctx -> ctx.put("effect.shield", Boolean.TRUE));
    }

    public static void register(EffectType type, EffectExecutor executor) {
        EXECUTORS.put(type, executor);
    }

    public static EffectExecutor get(EffectType type) {
        return EXECUTORS.getOrDefault(type, EffectExecutor.NO_OP);
    }

    private static void applyArmorBreak(SpellContext ctx) {
        ctx.put("effect.armor_break", Boolean.TRUE);
    }

    private static void applyAreaBoost(SpellContext ctx) {
        ctx.setRange(ctx.range() * 1.2);
    }

    private EffectExecutors() {}
}
