package org.example.Kangnaixi.tiandao.spell.rune.impl;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.example.Kangnaixi.tiandao.spell.rune.Rune;
import org.example.Kangnaixi.tiandao.spell.rune.RuneContext;

import java.util.List;

/**
 * 天阶符文 - 扇形区域
 */
public class RuneCone extends Rune {

    public RuneCone() {
        super(new Builder("cone", "扇形")
            .tier(RuneTier.TIAN)
            .category(RuneCategory.SHAPE)
            .description("锥形扇面，角度30-90°，距离5-12格")
            .spiritCost(7.0)
            .cooldown(1.0)
            .inputs(1)
            .outputs(1)
        );
    }

    @Override
    public void execute(RuneContext context) {
        Vec3 position = context.getPosition();
        Vec3 direction = context.getDirection();

        // 从上下文获取参数
        double distance = 8.0;  // 默认距离
        double angle = 60.0;    // 默认角度60°

        if (context.hasVariable("cone_distance")) {
            Object distObj = context.getVariable("cone_distance");
            if (distObj instanceof Number) {
                distance = ((Number) distObj).doubleValue();
            }
        }

        if (context.hasVariable("cone_angle")) {
            Object angleObj = context.getVariable("cone_angle");
            if (angleObj instanceof Number) {
                angle = ((Number) angleObj).doubleValue();
            }
        }

        // 获取扇形范围内的所有实体
        AABB box = new AABB(
            position.x - distance, position.y - distance, position.z - distance,
            position.x + distance, position.y + distance, position.z + distance
        );

        double finalDistance = distance;
        double finalAngle = angle;
        List<Entity> entities = context.getLevel().getEntities(
            context.getCaster(),
            box,
            entity -> isInCone(position, direction, entity.position(), finalDistance, finalAngle)
        );

        // 将受影响的实体添加到上下文
        context.getAffectedEntities().clear();
        context.getAffectedEntities().addAll(entities);

        // 添加扇形粒子效果
        spawnConeParticles(context, position, direction, distance, angle);
    }

    /**
     * 生成扇形粒子效果
     */
    private void spawnConeParticles(RuneContext context, Vec3 origin, Vec3 direction, double distance, double angle) {
        // TODO: 使用Minecraft粒子系统显示扇形效果
        // 暂时留空，后续添加粒子效果
    }

    /**
     * 检查一个点是否在扇形范围内
     */
    private boolean isInCone(Vec3 origin, Vec3 direction, Vec3 point, double distance, double angleDegrees) {
        Vec3 toPoint = point.subtract(origin);
        double dist = toPoint.length();

        // 距离检查
        if (dist > distance || dist < 0.1) {
            return false;
        }

        // 角度检查
        Vec3 normalizedToPoint = toPoint.normalize();
        double dotProduct = direction.dot(normalizedToPoint);
        double angleRad = Math.acos(dotProduct);
        double angleInDegrees = Math.toDegrees(angleRad);

        return angleInDegrees <= angleDegrees / 2.0;
    }
}
