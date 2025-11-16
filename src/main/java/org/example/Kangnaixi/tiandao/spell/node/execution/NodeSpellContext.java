package org.example.Kangnaixi.tiandao.spell.node.execution;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.example.Kangnaixi.tiandao.spell.node.target.SpellTargetKind;
import org.example.Kangnaixi.tiandao.spell.node.target.SpellTargetSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 节点术法执行上下文
 *
 * 在术法执行过程中传递状态和数据
 */
public class NodeSpellContext {

    private final Player caster;      // 施法者
    private final Level level;        // 世界

    // 当前状态
    private Vec3 position;            // 当前位置
    private Vec3 direction;           // 当前方向

    // 统一目标集
    private final SpellTargetSet currentTargets;
    private final SpellExecutionModifiers modifiers;

    // 变量存储（节点间传递数据）
    private final Map<String, Object> variables;

    // 执行统计
    private int nodesExecuted;
    private double totalSpiritCost;

    public NodeSpellContext(Player caster) {
        this.caster = caster;
        this.level = caster.level();
        this.position = caster.position();
        this.direction = caster.getLookAngle();
        this.currentTargets = new SpellTargetSet();
        this.variables = new HashMap<>();
        this.nodesExecuted = 0;
        this.totalSpiritCost = 0;
        this.modifiers = new SpellExecutionModifiers();
    }

    // ===== Getters & Setters =====

    public Player getCaster() {
        return caster;
    }

    public Level getLevel() {
        return level;
    }

    public Vec3 getPosition() {
        return position;
    }

    public void setPosition(Vec3 position) {
        this.position = position;
    }

    public Vec3 getDirection() {
        return direction;
    }

    public void setDirection(Vec3 direction) {
        this.direction = direction.normalize();
    }

    public SpellTargetSet getCurrentTargets() {
        return currentTargets;
    }

    public void replaceTargets(SpellTargetSet newTargets) {
        this.currentTargets.replaceWith(newTargets);
    }

    public void initializeWith(SpellTargetSet newTargets) {
        replaceTargets(newTargets);
    }

    public SpellExecutionModifiers getModifiers() {
        return modifiers;
    }

    public List<Entity> getAffectedEntities() {
        return new ArrayList<>(currentTargets.toLivingEntities());
    }

    /**
     * 设置受影响的实体（替换）
     */
    public void setAffectedEntities(List<Entity> entities) {
        currentTargets.setEntityTargets(entities.stream()
            .filter(entity -> entity instanceof LivingEntity)
            .map(entity -> (LivingEntity) entity)
            .toList());
    }

    /**
     * 添加受影响的实体
     */
    public void addAffectedEntity(Entity entity) {
        if (entity instanceof LivingEntity livingEntity) {
            currentTargets.addEntityTarget(livingEntity, 1.0);
        }
    }

    /**
     * 添加多个实体
     */
    public void addAffectedEntities(Collection<? extends Entity> entities) {
        for (Entity entity : entities) {
            addAffectedEntity(entity);
        }
    }

    /**
     * 清空受影响的实体
     */
    public void clearAffectedEntities() {
        currentTargets.clear();
    }

    /**
     * 获取受影响的实体数量
     */
    public int getAffectedEntityCount() {
        return currentTargets.getEntityTargets().size();
    }

    // ===== 变量系统 =====

    /**
     * 设置变量
     */
    public void setVariable(String key, Object value) {
        variables.put(key, value);
    }

    /**
     * 获取变量
     */
    @SuppressWarnings("unchecked")
    public <T> T getVariable(String key, Class<T> type) {
        Object value = variables.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * 获取变量（原始类型）
     */
    public Object getVariable(String key) {
        return variables.get(key);
    }

    /**
     * 检查变量是否存在
     */
    public boolean hasVariable(String key) {
        return variables.containsKey(key);
    }

    /**
     * 移除变量
     */
    public void removeVariable(String key) {
        variables.remove(key);
    }

    // ===== 统计 =====

    public int getNodesExecuted() {
        return nodesExecuted;
    }

    public void incrementNodesExecuted() {
        this.nodesExecuted++;
    }

    public double getTotalSpiritCost() {
        return totalSpiritCost;
    }

    public void addSpiritCost(double cost) {
        this.totalSpiritCost += cost;
    }

    public void setNodesExecuted(int count) {
        this.nodesExecuted = count;
    }

    // ===== 工具方法 =====

    /**
     * 重置上下文（用于重新执行）
     */
    public void reset() {
        this.position = caster.position();
        this.direction = caster.getLookAngle();
        this.currentTargets.clear();
        this.variables.clear();
        this.nodesExecuted = 0;
        this.totalSpiritCost = 0;
        this.modifiers.reset();
    }

    /**
     * 调试信息
     */
    public String getDebugInfo() {
        return String.format(
            "位置: (%.1f, %.1f, %.1f), 方向: (%.2f, %.2f, %.2f), 目标: %d, 节点: %d, 消耗: %.1f",
            position.x, position.y, position.z,
            direction.x, direction.y, direction.z,
            getAffectedEntityCount(),
            nodesExecuted,
            totalSpiritCost
        );
    }
}
