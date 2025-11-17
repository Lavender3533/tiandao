package org.example.Kangnaixi.tiandao.spell.runtime.engine;

/**
 * 术法属性执行器接口
 * 负责处理术法的属性加成（如金、木、水、火、土、剑意等）
 */
@FunctionalInterface
public interface AttributeExecutor {

    /**
     * 空操作执行器
     */
    AttributeExecutor NO_OP = ctx -> {};

    /**
     * 应用属性效果到上下文
     * @param ctx 术法上下文
     */
    void apply(SpellContext ctx);
}
