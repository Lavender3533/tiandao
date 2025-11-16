package org.example.Kangnaixi.tiandao.spell.rune.impl;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.example.Kangnaixi.tiandao.spell.rune.Rune;
import org.example.Kangnaixi.tiandao.spell.rune.RuneContext;

import java.util.List;

/**
 * 天阶符文 - 触摸触发
 */
public class RuneTouch extends Rune {

    public RuneTouch() {
        super(new Builder("touch", "触摸")
            .tier(RuneTier.TIAN)
            .category(RuneCategory.TRIGGER)
            .description("触摸目标触发，近距离接触")
            .spiritCost(3.0)
            .cooldown(0.5)
            .inputs(0)  // 触发符文没有输入
            .outputs(1)
        );
    }

    @Override
    public void execute(RuneContext context) {
        // 设置起始位置为施法者位置
        Vec3 casterPos = context.getCaster().position();
        Vec3 lookAngle = context.getCaster().getLookAngle();

        context.setPosition(casterPos);
        context.setDirection(lookAngle);

        // 检测近距离接触（2格范围内）
        double touchRange = 2.0;
        Vec3 targetPos = casterPos.add(lookAngle.scale(touchRange));

        AABB box = new AABB(
            targetPos.x - 0.5, targetPos.y - 0.5, targetPos.z - 0.5,
            targetPos.x + 0.5, targetPos.y + 0.5, targetPos.z + 0.5
        );

        List<Entity> nearbyEntities = context.getLevel().getEntities(
            context.getCaster(),
            box,
            entity -> entity.position().distanceTo(targetPos) <= 1.5
        );

        // 如果检测到目标，设置第一个为目标实体
        if (!nearbyEntities.isEmpty()) {
            context.setTarget(nearbyEntities.get(0));
            context.setPosition(nearbyEntities.get(0).position());
        }
    }
}
