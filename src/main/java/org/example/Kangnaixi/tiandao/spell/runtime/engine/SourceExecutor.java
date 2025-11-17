package org.example.Kangnaixi.tiandao.spell.runtime.engine;

/**
 * 术法来源执行器接口
 * 负责处理术法的起始来源（如手印、武器、法器等）
 */
@FunctionalInterface
public interface SourceExecutor {

    /**
     * 空操作执行器
     */
    SourceExecutor NO_OP = ctx -> {};

    /**
     * 应用来源效果到上下文
     * @param ctx 术法上下文
     */
    void apply(SpellContext ctx);
}
