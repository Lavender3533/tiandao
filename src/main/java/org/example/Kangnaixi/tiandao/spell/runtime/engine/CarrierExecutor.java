package org.example.Kangnaixi.tiandao.spell.runtime.engine;

/**
 * 术法载体执行器接口
 * 负责生成术法的载体形态（如剑气、弹道、力场等）
 */
@FunctionalInterface
public interface CarrierExecutor {

    /**
     * 空操作执行器
     */
    CarrierExecutor NO_OP = ctx -> {};

    /**
     * 应用载体效果到上下文
     * @param ctx 术法上下文
     */
    void apply(SpellContext ctx);
}
