package org.example.Kangnaixi.tiandao.spell.rune.impl;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.example.Kangnaixi.tiandao.spell.rune.Rune;
import org.example.Kangnaixi.tiandao.spell.rune.RuneContext;

/**
 * 天阶符文 - 推动效果
 */
public class RunePush extends Rune {

    public RunePush() {
        super(new Builder("push", "推动")
            .tier(RuneTier.TIAN)
            .category(RuneCategory.EFFECT)
            .description("击退目标，力度1-3")
            .spiritCost(5.0)
            .cooldown(1.0)
            .inputs(1)
            .outputs(0)  // 效果符文通常没有输出
        );
    }

    @Override
    public void execute(RuneContext context) {
        double pushStrength = 1.5; // 默认推力强度

        Vec3 direction = context.getDirection().normalize();

        // 对所有受影响的实体施加推力
        for (Entity entity : context.getAffectedEntities()) {
            Vec3 pushVector = direction.scale(pushStrength);
            entity.setDeltaMovement(entity.getDeltaMovement().add(pushVector));
            entity.hurtMarked = true;
        }

        // TODO: 添加推力粒子效果
    }
}
