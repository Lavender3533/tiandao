package org.example.Kangnaixi.tiandao.spell.runtime.engine.form;

import org.example.Kangnaixi.tiandao.spell.runtime.FormType;
import org.example.Kangnaixi.tiandao.spell.runtime.engine.SpellContext;

import java.util.EnumMap;
import java.util.Map;

public final class FormExecutors {

    private static final Map<FormType, FormExecutor> EXECUTORS = new EnumMap<>(FormType.class);

    static {
        register(FormType.INSTANT, ctx -> {});
        register(FormType.DELAYED, ctx -> ctx.put("form.delay_ticks", 20));
        register(FormType.CHANNEL, FormExecutors::applyChannel);
        register(FormType.DURATION, ctx -> ctx.put("form.duration_ticks", 200));
        register(FormType.COMBO, ctx -> ctx.put("form.combo", Boolean.TRUE));
    }

    public static void register(FormType type, FormExecutor executor) {
        EXECUTORS.put(type, executor);
    }

    public static FormExecutor get(FormType type) {
        return EXECUTORS.getOrDefault(type, FormExecutor.NO_OP);
    }

    private static void applyChannel(SpellContext ctx) {
        ctx.put("form.channeling", Boolean.TRUE);
        ctx.setSpiritCost(ctx.spiritCost() * 1.2);
    }

    private FormExecutors() {}
}
