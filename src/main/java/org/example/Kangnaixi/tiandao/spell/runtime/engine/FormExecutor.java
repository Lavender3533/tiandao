package org.example.Kangnaixi.tiandao.spell.runtime.engine;

/**
 * 术法形式执行器接口
 * 负责处理术法的释放形式（如瞬发、引导、持续等）
 */
@FunctionalInterface
public interface FormExecutor {

    /**
     * 空操作执行器
     */
    FormExecutor NO_OP = ctx -> {};

    /**
     * 应用形式效果到上下文
     * @param ctx 术法上下文
     */
    void apply(SpellContext ctx);
}
