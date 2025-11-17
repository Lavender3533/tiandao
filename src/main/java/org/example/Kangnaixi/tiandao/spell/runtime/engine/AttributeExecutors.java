package org.example.Kangnaixi.tiandao.spell.runtime.engine;

import org.example.Kangnaixi.tiandao.spell.runtime.AttributeType;

import java.util.EnumMap;
import java.util.Map;

/**
 * 术法属性执行器注册器
 */
public final class AttributeExecutors {

    private static final Map<AttributeType, AttributeExecutor> EXECUTORS = new EnumMap<>(AttributeType.class);

    static {
        // 注册基础实现
        register(AttributeType.SWORD_INTENT, AttributeExecutors::applySwordIntent);
        register(AttributeType.METAL, AttributeExecutors::applyMetal);
        register(AttributeType.WOOD, AttributeExecutor.NO_OP);
        register(AttributeType.WATER, AttributeExecutor.NO_OP);
        register(AttributeType.FIRE, AttributeExecutor.NO_OP);
        register(AttributeType.EARTH, AttributeExecutor.NO_OP);
        register(AttributeType.YIN, AttributeExecutor.NO_OP);
        register(AttributeType.YANG, AttributeExecutor.NO_OP);
    }

    /**
     * 注册属性执行器
     */
    public static void register(AttributeType type, AttributeExecutor executor) {
        EXECUTORS.put(type, executor);
    }

    /**
     * 获取属性执行器
     */
    public static AttributeExecutor get(AttributeType type) {
        return EXECUTORS.getOrDefault(type, AttributeExecutor.NO_OP);
    }

    // ========== 基础实现 ==========

    /**
     * 剑意属性：增加伤害25%
     */
    private static void applySwordIntent(SpellContext ctx) {
        ctx.setDamage(ctx.getDamage() * 1.25);
        ctx.putData("attribute.sword_intent", true);
    }

    /**
     * 金属性：增加伤害15%
     */
    private static void applyMetal(SpellContext ctx) {
        ctx.setDamage(ctx.getDamage() * 1.15);
        ctx.putData("attribute.metal", true);
    }

    private AttributeExecutors() {}
}
