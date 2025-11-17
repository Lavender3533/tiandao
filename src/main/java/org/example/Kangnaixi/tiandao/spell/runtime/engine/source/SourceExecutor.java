package org.example.Kangnaixi.tiandao.spell.runtime.engine.source;

import org.example.Kangnaixi.tiandao.spell.runtime.engine.SpellContext;

@FunctionalInterface
public interface SourceExecutor {

    SourceExecutor NO_OP = ctx -> {};

    void beginCast(SpellContext ctx);
}
