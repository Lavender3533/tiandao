package org.example.Kangnaixi.tiandao.spell.node.execution.shape;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.example.Kangnaixi.tiandao.spell.node.NodeComponent;
import org.example.Kangnaixi.tiandao.spell.node.execution.ComponentExecutor;
import org.example.Kangnaixi.tiandao.spell.node.execution.NodeSpellContext;

import java.util.List;

/**
 * 直线范围执行器
 * 检测从当前位置向前方直线上的实体
 */
public class LineShapeExecutor implements ComponentExecutor {

    @Override
    public void execute(NodeComponent component, NodeSpellContext context) {
        // 获取距离参数
        Double distanceParam = component.getParameter("distance", Double.class);
        if (distanceParam == null) distanceParam = 15.0;
        final double distance = distanceParam;

        Vec3 start = context.getPosition();
        Vec3 direction = context.getDirection();
        Vec3 end = start.add(direction.scale(distance));

        // 使用射线检测方块碰撞
        ClipContext clipContext = new ClipContext(
            start,
            end,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            context.getCaster()
        );

        var hitResult = context.getLevel().clip(clipContext);
        if (hitResult.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
            // 如果碰到方块，缩短距离
            end = hitResult.getLocation();
        }

        // 使用包围盒筛选实体
        AABB searchBox = new AABB(start, end).inflate(1.0);  // 膨胀1格宽度
        final Vec3 finalEnd = end;

        // 获取射线附近的实体
        List<LivingEntity> entities = context.getLevel().getEntitiesOfClass(
            LivingEntity.class,
            searchBox,
            entity -> {
                if (entity == context.getCaster()) {
                    return false;
                }

                // 检查实体是否在射线附近（点到线段的距离）
                Vec3 entityPos = entity.position();
                double distToLine = distanceToLineSegment(start, finalEnd, entityPos);
                return distToLine <= 1.5;  // 1.5格容差
            }
        );

        // 添加到上下文
        context.addAffectedEntities(entities);
    }

    /**
     * 计算点到线段的距离
     */
    private double distanceToLineSegment(Vec3 lineStart, Vec3 lineEnd, Vec3 point) {
        Vec3 line = lineEnd.subtract(lineStart);
        double lineLength = line.length();
        if (lineLength == 0) {
            return point.distanceTo(lineStart);
        }

        Vec3 lineNorm = line.normalize();
        Vec3 toPoint = point.subtract(lineStart);

        // 投影到线段上
        double projection = toPoint.dot(lineNorm);

        // 限制在线段范围内
        projection = Math.max(0, Math.min(lineLength, projection));

        // 最近点
        Vec3 closestPoint = lineStart.add(lineNorm.scale(projection));

        return point.distanceTo(closestPoint);
    }

    @Override
    public String getId() {
        return "line";
    }
}
