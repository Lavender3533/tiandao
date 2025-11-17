package org.example.Kangnaixi.tiandao.spell.runtime.engine;

/**
 * 术法效果执行器接口
 * 负责处理术法的特殊效果（如AOE、护盾、治疗、破甲等）
 */
@FunctionalInterface
public interface EffectExecutor {

    /**
     * 空操作执行器
     */
    EffectExecutor NO_OP = ctx -> {};

    /**
     * 应用效果到上下文
     * @param ctx 术法上下文
     */
    void apply(SpellContext ctx);
}
