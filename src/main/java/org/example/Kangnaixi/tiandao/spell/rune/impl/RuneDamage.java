package org.example.Kangnaixi.tiandao.spell.rune.impl;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.example.Kangnaixi.tiandao.spell.rune.Rune;
import org.example.Kangnaixi.tiandao.spell.rune.RuneContext;

/**
 * 天阶符文 - 伤害效果
 */
public class RuneDamage extends Rune {

    public RuneDamage() {
        super(new Builder("damage", "伤害")
            .tier(RuneTier.TIAN)
            .category(RuneCategory.EFFECT)
            .description("造成物理伤害，3-10点")
            .spiritCost(7.0)
            .cooldown(1.0)
            .inputs(1)
            .outputs(0)  // 效果符文通常没有输出
        );
    }

    @Override
    public void execute(RuneContext context) {
        double damage = 5.0; // 默认伤害值

        // 对所有受影响的实体造成伤害
        for (Entity entity : context.getAffectedEntities()) {
            DamageSource damageSource = context.getLevel().damageSources().playerAttack(context.getCaster());
            entity.hurt(damageSource, (float) damage);
        }

        // TODO: 添加伤害粒子效果
    }
}
