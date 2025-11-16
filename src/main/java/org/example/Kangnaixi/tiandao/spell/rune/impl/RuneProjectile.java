package org.example.Kangnaixi.tiandao.spell.rune.impl;

import org.example.Kangnaixi.tiandao.spell.rune.Rune;
import org.example.Kangnaixi.tiandao.spell.rune.RuneContext;

/**
 * 天阶符文 - 弹道触发
 */
public class RuneProjectile extends Rune {

    public RuneProjectile() {
        super(new Builder("projectile", "弹道")
            .tier(RuneTier.TIAN)
            .category(RuneCategory.TRIGGER)
            .description("发射弹丸到目标位置")
            .spiritCost(8.0)
            .cooldown(2.0)
            .inputs(0)  // 触发符文没有输入
            .outputs(1)
        );
    }

    @Override
    public void execute(RuneContext context) {
        // 设置起始位置为施法者位置
        context.setPosition(context.getCaster().position());
        context.setDirection(context.getCaster().getLookAngle());

        // TODO: 实际发射弹丸逻辑
        // 存储弹丸速度信息供后续符文使用
        context.setVariable("projectile_speed", 1.5);
    }
}
