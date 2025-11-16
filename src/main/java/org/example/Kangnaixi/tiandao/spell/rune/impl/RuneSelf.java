package org.example.Kangnaixi.tiandao.spell.rune.impl;

import org.example.Kangnaixi.tiandao.spell.rune.Rune;
import org.example.Kangnaixi.tiandao.spell.rune.RuneContext;

/**
 * 天阶符文 - 自身触发
 */
public class RuneSelf extends Rune {

    public RuneSelf() {
        super(new Builder("self", "自身")
            .tier(RuneTier.TIAN)
            .category(RuneCategory.TRIGGER)
            .description("以施法者为起点")
            .spiritCost(5.0)
            .cooldown(1.0)
            .inputs(0)  // 触发符文没有输入
            .outputs(1)
        );
    }

    @Override
    public void execute(RuneContext context) {
        // 设置起始位置为施法者位置
        context.setPosition(context.getCaster().position());
        context.setDirection(context.getCaster().getLookAngle());
    }
}
