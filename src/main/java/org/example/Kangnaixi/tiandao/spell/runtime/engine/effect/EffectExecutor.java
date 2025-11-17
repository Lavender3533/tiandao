package org.example.Kangnaixi.tiandao.spell.runtime.engine.effect;

import org.example.Kangnaixi.tiandao.spell.runtime.engine.SpellContext;

@FunctionalInterface
public interface EffectExecutor {

    EffectExecutor NO_OP = ctx -> {};

    void apply(SpellContext ctx);
}
