package org.example.Kangnaixi.tiandao.spell.runtime.engine.form;

import org.example.Kangnaixi.tiandao.spell.runtime.FormType;
import org.example.Kangnaixi.tiandao.spell.runtime.engine.SpellContext;

import java.util.EnumMap;
import java.util.Map;

public final class FormExecutors {

    private static final Map<FormType, FormExecutor> EXECUTORS = new EnumMap<>(FormType.class);

    static {
        register(FormType.INSTANT, new InstantForm());
        register(FormType.DELAYED, new DelayedForm());
        register(FormType.CHANNEL, new ChannelForm());
        register(FormType.DURATION, new DurationForm());
        register(FormType.COMBO, new ComboForm());
    }

    public static void register(FormType type, FormExecutor executor) {
        EXECUTORS.put(type, executor);
    }

    public static FormExecutor get(FormType type) {
        return EXECUTORS.getOrDefault(type, FormExecutor.NO_OP);
    }

    private FormExecutors() {}

    private static final class InstantForm implements FormExecutor {
        @Override
        public void applyFormBehavior(SpellContext ctx) {
            ctx.spawnCarrier();
            ctx.finish();
        }
    }

    private static final class DelayedForm implements FormExecutor {
        @Override
        public void applyFormBehavior(SpellContext ctx) {
            ctx.setDelayed(true);
            ctx.setDelayTicks(20);
        }

        @Override
        public boolean tick(SpellContext ctx) {
            ctx.incrementTicks();
            if (ctx.delayed() && ctx.ticksExisted() >= ctx.delayTicks()) {
                ctx.setDelayed(false);
                ctx.spawnCarrier();
                ctx.finish();
                return false;
            }
            return true;
        }
    }

    private static final class ChannelForm implements FormExecutor {
        @Override
        public void applyFormBehavior(SpellContext ctx) {
            ctx.setChanneling(true);
            ctx.setMaxDuration(100);
        }

        @Override
        public boolean tick(SpellContext ctx) {
            ctx.incrementTicks();
            if (ctx.ticksExisted() % 10 == 0) {
                ctx.spawnCarrier();
            }
            if (!ctx.caster().isUsingItem() || ctx.ticksExisted() >= ctx.maxDuration()) {
                ctx.finish();
                return false;
            }
            return true;
        }
    }

    private static final class DurationForm implements FormExecutor {
        @Override
        public void applyFormBehavior(SpellContext ctx) {
            ctx.setDurationSkill(true);
            ctx.setMaxDuration(200);
        }

        @Override
        public boolean tick(SpellContext ctx) {
            ctx.incrementTicks();
            ctx.spawnCarrier();
            if (ctx.ticksExisted() >= ctx.maxDuration()) {
                ctx.finish();
                return false;
            }
            return true;
        }
    }

    private static final class ComboForm implements FormExecutor {
        @Override
        public void applyFormBehavior(SpellContext ctx) {
            ctx.setMaxDuration(60);
        }

        @Override
        public boolean tick(SpellContext ctx) {
            ctx.incrementTicks();
            if (ctx.ticksExisted() % 15 == 0) {
                ctx.spawnCarrier();
            }
            if (ctx.ticksExisted() >= ctx.maxDuration()) {
                ctx.finish();
                return false;
            }
            return true;
        }
    }
}
