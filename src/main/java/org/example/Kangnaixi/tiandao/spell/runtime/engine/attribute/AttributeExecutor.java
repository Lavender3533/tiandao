package org.example.Kangnaixi.tiandao.spell.runtime.engine.attribute;

import org.example.Kangnaixi.tiandao.spell.runtime.engine.SpellContext;

@FunctionalInterface
public interface AttributeExecutor {

    AttributeExecutor NO_OP = ctx -> {};

    void apply(SpellContext ctx);
}
