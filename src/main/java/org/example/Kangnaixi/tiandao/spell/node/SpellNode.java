package org.example.Kangnaixi.tiandao.spell.node;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 术法节点 - 包含多个槽位的容器
 *
 * 节点内的执行逻辑：
 * 1. 形状组件先执行，筛选出目标实体
 * 2. 效果组件对筛选出的目标施加效果
 * 3. 修饰组件修改整个节点的行为
 */
public class SpellNode {

    private final int index;  // 节点编号（1, 2, 3, ...）
    private int maxSlots;     // 最大槽位数
    private final List<NodeComponent> components;  // 槽位中的组件

    // 槽位扩展消耗
    private static final double SLOT_EXPANSION_COST = 5.0;

    public SpellNode(int index, int maxSlots) {
        this.index = index;
        this.maxSlots = maxSlots;
        this.components = new ArrayList<>();
    }

    // ===== Getters & Setters =====

    public int getIndex() {
        return index;
    }

    public int getMaxSlots() {
        return maxSlots;
    }

    public void setMaxSlots(int maxSlots) {
        this.maxSlots = Math.max(1, maxSlots);  // 至少1个槽位
    }

    public List<NodeComponent> getComponents() {
        return components;
    }

    /**
     * 添加组件到槽位
     */
    public boolean addComponent(NodeComponent component) {
        if (components.size() >= maxSlots) {
            return false;  // 槽位已满
        }
        if (component.getType() == ComponentType.PROJECTILE && hasProjectileComponent()) {
            return false;
        }
        components.add(component.copy());  // 添加副本，避免共享引用
        return true;
    }

    /**
     * 移除槽位中的组件
     */
    public boolean removeComponent(int slotIndex) {
        if (slotIndex >= 0 && slotIndex < components.size()) {
            components.remove(slotIndex);
            return true;
        }
        return false;
    }

    /**
     * 获取指定槽位的组件
     */
    @Nullable
    public NodeComponent getComponent(int slotIndex) {
        if (slotIndex >= 0 && slotIndex < components.size()) {
            return components.get(slotIndex);
        }
        return null;
    }

    /**
     * 清空所有槽位
     */
    public void clear() {
        components.clear();
    }

    /**
     * 是否为空节点
     */
    public boolean isEmpty() {
        return components.isEmpty();
    }

    // ===== 计算 =====

    /**
     * 计算节点的灵力消耗
     */
    public double calculateSpiritCost() {
        // 组件消耗
        double componentCost = components.stream()
            .mapToDouble(NodeComponent::calculateSpiritCost)
            .sum();

        // 槽位扩展惩罚
        double slotPenalty = (maxSlots - 1) * SLOT_EXPANSION_COST;

        return componentCost + slotPenalty;
    }

    /**
     * 计算节点的冷却时间
     */
    public double calculateCooldown() {
        return components.stream()
            .mapToDouble(NodeComponent::getBaseCooldown)
            .max()
            .orElse(0);
    }

    private boolean hasProjectileComponent() {
        return components.stream().anyMatch(c -> c.getType() == ComponentType.PROJECTILE);
    }

    /**
     * 获取投射组件列表
     */
    public List<NodeComponent> getProjectileComponents() {
        return components.stream()
            .filter(c -> c.getType() == ComponentType.PROJECTILE)
            .toList();
    }

    /**
     * @deprecated 形状组件已被投射组件替代
     */
    @Deprecated
    public List<NodeComponent> getShapeComponents() {
        return getProjectileComponents();
    }

    /**
     * 获取效果组件列表
     */
    public List<NodeComponent> getEffectComponents() {
        return components.stream()
            .filter(c -> c.getType() == ComponentType.EFFECT)
            .toList();
    }

    /**
     * 获取修饰组件列表
     */
    public List<NodeComponent> getModifierComponents() {
        return components.stream()
            .filter(c -> c.getType() == ComponentType.MODIFIER)
            .toList();
    }

    // ===== 序列化 =====

    /**
     * 序列化到NBT
     */
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("index", index);
        tag.putInt("maxSlots", maxSlots);

        // 保存组件列表
        ListTag componentsList = new ListTag();
        for (NodeComponent component : components) {
            componentsList.add(component.toNBT());
        }
        tag.put("components", componentsList);

        return tag;
    }

    /**
     * 从NBT反序列化
     */
    public static SpellNode fromNBT(CompoundTag tag) {
        int index = tag.getInt("index");
        int maxSlots = tag.getInt("maxSlots");

        SpellNode node = new SpellNode(index, maxSlots);

        // 恢复组件（需要ComponentRegistry支持）
        ListTag componentsList = tag.getList("components", 10);  // 10 = COMPOUND
        for (int i = 0; i < componentsList.size(); i++) {
            CompoundTag compTag = componentsList.getCompound(i);
            NodeComponent component = NodeComponent.fromNBT(compTag);
            if (component != null) {
                node.components.add(component);
            }
        }

        return node;
    }

    /**
     * 复制节点
     */
    public SpellNode copy() {
        SpellNode copy = new SpellNode(index, maxSlots);
        for (NodeComponent component : components) {
            copy.components.add(component.copy());
        }
        return copy;
    }

    @Override
    public String toString() {
        return "节点" + index + " [" + components.size() + "/" + maxSlots + " 槽位]";
    }
}
