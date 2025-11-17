package org.example.Kangnaixi.tiandao.spell.runtime.engine;

import org.example.Kangnaixi.tiandao.spell.runtime.FormType;

import java.util.EnumMap;
import java.util.Map;

/**
 * 术法形式执行器注册器
 */
public final class FormExecutors {

    private static final Map<FormType, FormExecutor> EXECUTORS = new EnumMap<>(FormType.class);

    static {
        // 注册基础实现
        register(FormType.INSTANT, FormExecutors::applyInstant);
        register(FormType.DELAYED, FormExecutor.NO_OP); // 延迟施法暂未实现
        register(FormType.CHANNEL, FormExecutor.NO_OP); // 引导施法暂未实现
        register(FormType.DURATION, FormExecutor.NO_OP); // 持续施法暂未实现
        register(FormType.COMBO, FormExecutor.NO_OP); // 连击施法暂未实现
        register(FormType.MARK_DETONATE, FormExecutor.NO_OP); // 标记引爆暂未实现
    }

    /**
     * 注册形式执行器
     */
    public static void register(FormType type, FormExecutor executor) {
        EXECUTORS.put(type, executor);
    }

    /**
     * 获取形式执行器
     */
    public static FormExecutor get(FormType type) {
        return EXECUTORS.getOrDefault(type, FormExecutor.NO_OP);
    }

    // ========== 基础实现 ==========

    /**
     * 瞬发形式：立即执行，无额外效果
     */
    private static void applyInstant(SpellContext ctx) {
        ctx.putData("form.instant", true);
        // 瞬发术法，无需特殊处理
    }

    private FormExecutors() {}
}
