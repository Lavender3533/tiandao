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
 * 圆形范围执行器
 * 检测以当前位置为中心的球形范围内的实体
 */
public class CircleShapeExecutor implements ComponentExecutor {

    @Override
    public void execute(NodeComponent component, NodeSpellContext context) {
        // 获取半径参数
        Double radiusParam = component.getParameter("radius", Double.class);
        if (radiusParam == null) {
            radiusParam = 5.0;  // 默认半径
        }
        final double radius = radiusParam;  // lambda需要final

        Vec3 center = context.getPosition();

        // 使用AABB快速筛选
        AABB searchBox = new AABB(
            center.x - radius, center.y - radius, center.z - radius,
            center.x + radius, center.y + radius, center.z + radius
        );

        // 获取范围内的实体
        List<LivingEntity> entities = context.getLevel().getEntitiesOfClass(
            LivingEntity.class,
            searchBox,
            entity -> {
                // 过滤条件
                if (entity == context.getCaster()) {
                    return false;  // 排除施法者
                }

                // 精确距离检查
                double distance = entity.position().distanceTo(center);
                return distance <= radius;
            }
        );

        // 添加到上下文（并集模式）
        context.addAffectedEntities(entities);
    }

    @Override
    public String getId() {
        return "circle";
    }
}
