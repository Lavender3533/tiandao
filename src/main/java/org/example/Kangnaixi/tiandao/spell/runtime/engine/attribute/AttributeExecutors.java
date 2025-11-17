package org.example.Kangnaixi.tiandao.spell.runtime.engine.attribute;

import org.example.Kangnaixi.tiandao.spell.runtime.AttributeType;
import org.example.Kangnaixi.tiandao.spell.runtime.engine.SpellContext;

import java.util.EnumMap;
import java.util.Map;

public final class AttributeExecutors {

    private static final Map<AttributeType, AttributeExecutor> EXECUTORS = new EnumMap<>(AttributeType.class);

    static {
        register(AttributeType.FIRE, AttributeExecutors::applyFire);
        register(AttributeType.SWORD_INTENT, AttributeExecutors::applySwordIntent);
        register(AttributeType.METAL, ctx -> ctx.setBaseDamage(ctx.baseDamage() + 1.0));
        register(AttributeType.WOOD, ctx -> ctx.put("attribute.wood", Boolean.TRUE));
        register(AttributeType.THUNDER_INTENT, ctx -> ctx.put("attribute.thunder", Boolean.TRUE));
    }

    public static void register(AttributeType type, AttributeExecutor executor) {
        EXECUTORS.put(type, executor);
    }

    public static AttributeExecutor get(AttributeType type) {
        return EXECUTORS.getOrDefault(type, AttributeExecutor.NO_OP);
    }

    private static void applyFire(SpellContext ctx) {
        ctx.put("attribute.fire.dot", Boolean.TRUE);
        ctx.setBaseDamage(ctx.baseDamage() + 2.0);
    }

    private static void applySwordIntent(SpellContext ctx) {
        ctx.setBaseDamage(ctx.baseDamage() * 1.15);
        ctx.put("attribute.sword_intent", Boolean.TRUE);
    }

    private AttributeExecutors() {}
}
