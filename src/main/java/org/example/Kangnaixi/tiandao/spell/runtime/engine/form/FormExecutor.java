package org.example.Kangnaixi.tiandao.spell.runtime.engine.form;

import org.example.Kangnaixi.tiandao.spell.runtime.engine.SpellContext;

@FunctionalInterface
public interface FormExecutor {

    FormExecutor NO_OP = new FormExecutor() {
        @Override
        public void applyFormBehavior(SpellContext ctx) {}

        @Override
        public boolean tick(SpellContext ctx) {
            return false;
        }
    };

    void applyFormBehavior(SpellContext ctx);

    /**
     * 每 Tick 回调, 返回 true 表示继续运行.
     */
    default boolean tick(SpellContext ctx) {
        return false;
    }
}
