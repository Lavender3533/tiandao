package org.example.Kangnaixi.tiandao.spell.rune.impl;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.example.Kangnaixi.tiandao.spell.rune.Rune;
import org.example.Kangnaixi.tiandao.spell.rune.RuneContext;

import java.util.List;

/**
 * 天阶符文 - 圆形区域
 */
public class RuneCircle extends Rune {

    public RuneCircle() {
        super(new Builder("circle", "圆形")
            .tier(RuneTier.TIAN)
            .category(RuneCategory.SHAPE)
            .description("球形区域效果，半径3-8格")
            .spiritCost(6.0)
            .cooldown(0.5)
            .inputs(1)
            .outputs(1)
        );
    }

    @Override
    public void execute(RuneContext context) {
        Vec3 position = context.getPosition();

        // 从上下文获取半径参数（如果有的话）
        double radius; // 默认半径
        if (context.hasVariable("circle_radius")) {
            Object radiusObj = context.getVariable("circle_radius");
            if (radiusObj instanceof Number) {
                radius = ((Number) radiusObj).doubleValue();
            } else {
                radius = 5.0;
            }
        } else {
            radius = 5.0;
        }

        // 获取范围内的所有实体
        AABB box = new AABB(
            position.x - radius, position.y - radius, position.z - radius,
            position.x + radius, position.y + radius, position.z + radius
        );

        List<Entity> entities = context.getLevel().getEntities(
            context.getCaster(),
            box,
            entity -> entity.position().distanceTo(position) <= radius
        );

        // 将受影响的实体添加到上下文
        context.getAffectedEntities().clear();
        context.getAffectedEntities().addAll(entities);

        // 添加粒子效果
        spawnCircleParticles(context, position, radius);
    }

    /**
     * 生成圆形粒子效果
     */
    private void spawnCircleParticles(RuneContext context, Vec3 center, double radius) {
        // TODO: 使用Minecraft粒子系统显示圆形效果
        // 暂时留空，后续添加粒子效果
    }
}
