package org.example.Kangnaixi.tiandao.spell.runtime.engine.form;

import org.example.Kangnaixi.tiandao.spell.runtime.engine.SpellContext;

@FunctionalInterface
public interface FormExecutor {

    FormExecutor NO_OP = ctx -> {};

    void applyFormBehavior(SpellContext ctx);
}
