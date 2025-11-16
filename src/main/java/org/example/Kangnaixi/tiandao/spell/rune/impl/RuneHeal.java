package org.example.Kangnaixi.tiandao.spell.rune.impl;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.example.Kangnaixi.tiandao.spell.rune.Rune;
import org.example.Kangnaixi.tiandao.spell.rune.RuneContext;

/**
 * 天阶符文 - 治疗效果
 */
public class RuneHeal extends Rune {

    public RuneHeal() {
        super(new Builder("heal", "治疗")
            .tier(RuneTier.TIAN)
            .category(RuneCategory.EFFECT)
            .description("恢复生命值，3-8点")
            .spiritCost(6.0)
            .cooldown(1.5)
            .inputs(1)
            .outputs(0)  // 效果符文通常没有输出
        );
    }

    @Override
    public void execute(RuneContext context) {
        double healAmount = 5.0; // 默认治疗量

        // 对所有受影响的实体进行治疗
        for (Entity entity : context.getAffectedEntities()) {
            if (entity instanceof LivingEntity living) {
                living.heal((float) healAmount);
            }
        }

        // TODO: 添加治疗粒子效果
    }
}
