package org.example.Kangnaixi.tiandao.spell.node.execution.shape;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.example.Kangnaixi.tiandao.spell.node.NodeComponent;
import org.example.Kangnaixi.tiandao.spell.node.execution.ComponentExecutor;
import org.example.Kangnaixi.tiandao.spell.node.execution.NodeSpellContext;

import java.util.List;

/**
 * 扇形范围执行器
 * 检测前方扇形范围内的实体
 */
public class ConeShapeExecutor implements ComponentExecutor {

    @Override
    public void execute(NodeComponent component, NodeSpellContext context) {
        // 获取参数
        Double distanceParam = component.getParameter("distance", Double.class);
        if (distanceParam == null) distanceParam = 10.0;
        final double distance = distanceParam;

        Double angleParam = component.getParameter("angle", Double.class);
        if (angleParam == null) angleParam = 45.0;
        final double angle = angleParam;

        Vec3 center = context.getPosition();
        Vec3 direction = context.getDirection();

        // 使用AABB快速筛选
        AABB searchBox = new AABB(
            center.x - distance, center.y - distance, center.z - distance,
            center.x + distance, center.y + distance, center.z + distance
        );

        // 计算角度的余弦值（用于判断）
        double angleRad = Math.toRadians(angle);
        double cosAngle = Math.cos(angleRad);

        // 获取范围内的实体
        List<LivingEntity> entities = context.getLevel().getEntitiesOfClass(
            LivingEntity.class,
            searchBox,
            entity -> {
                // 过滤条件
                if (entity == context.getCaster()) {
                    return false;
                }

                // 距离检查
                Vec3 toEntity = entity.position().subtract(center);
                double dist = toEntity.length();
                if (dist > distance) {
                    return false;
                }

                // 角度检查
                Vec3 toEntityNorm = toEntity.normalize();
                double dotProduct = direction.dot(toEntityNorm);
                return dotProduct >= cosAngle;
            }
        );

        // 添加到上下文
        context.addAffectedEntities(entities);
    }

    @Override
    public String getId() {
        return "cone";
    }
}
