package org.example.Kangnaixi.tiandao.spell.runtime.engine.carrier;

import org.example.Kangnaixi.tiandao.spell.runtime.engine.SpellContext;

@FunctionalInterface
public interface CarrierExecutor {

    CarrierExecutor NO_OP = ctx -> {};

    void createCarrier(SpellContext ctx);
}
