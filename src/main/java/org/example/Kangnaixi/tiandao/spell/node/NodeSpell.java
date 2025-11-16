package org.example.Kangnaixi.tiandao.spell.node;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 节点术法 - 完整的术法定义
 *
 * 结构：
 * [触发方式] → 节点1 → 节点2 → ... → 节点N
 *
 * 执行流程（流水线模式）：
 * 1. 触发方式决定初始位置和方向
 * 2. 节点按顺序执行，每个节点处理上一个节点的结果
 * 3. 节点内：形状筛选目标 → 效果作用于目标
 */
public class NodeSpell {

    private final String id;  // 唯一ID
    private String name;      // 术法名称
    private String description;  // 描述

    private TriggerType triggerType;  // 触发方式
    private final List<SpellNode> nodes;  // 节点序列

    // 创建信息
    private UUID creatorId;
    private long createdTime;

    public NodeSpell(String id, String name) {
        this.id = id;
        this.name = name;
        this.description = "";
        this.triggerType = TriggerType.SELF;
        this.nodes = new ArrayList<>();
        this.createdTime = System.currentTimeMillis();
    }

    // ===== Getters & Setters =====

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TriggerType getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(TriggerType triggerType) {
        this.triggerType = triggerType;
    }

    public List<SpellNode> getNodes() {
        return nodes;
    }

    public UUID getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(UUID creatorId) {
        this.creatorId = creatorId;
    }

    /**
     * 添加节点
     */
    public void addNode(SpellNode node) {
        nodes.add(node);
    }

    /**
     * 移除节点
     */
    public boolean removeNode(int index) {
        if (index >= 0 && index < nodes.size()) {
            nodes.remove(index);
            // 重新编号
            reindexNodes();
            return true;
        }
        return false;
    }

    /**
     * 获取节点
     */
    public SpellNode getNode(int index) {
        if (index >= 0 && index < nodes.size()) {
            return nodes.get(index);
        }
        return null;
    }

    /**
     * 清空所有节点
     */
    public void clearNodes() {
        nodes.clear();
    }

    /**
     * 重新编号所有节点
     */
    private void reindexNodes() {
        for (int i = 0; i < nodes.size(); i++) {
            // 节点的index字段是final的，这里需要重新创建节点
            // 或者改成非final
        }
    }

    // ===== 验证 =====

    /**
     * 验证术法是否有效
     */
    public ValidationResult validate() {
        if (nodes.isEmpty()) {
            return new ValidationResult(false, "术法至少需要一个节点");
        }

        // 检查每个节点
        for (SpellNode node : nodes) {
            if (node.isEmpty()) {
                return new ValidationResult(false, "节点" + node.getIndex() + "为空");
            }

            boolean hasProjectile = !node.getProjectileComponents().isEmpty();
            boolean hasEffect = !node.getEffectComponents().isEmpty();
            if (!hasProjectile && !hasEffect) {
                return new ValidationResult(false, "节点" + node.getIndex() + "缺少投射或效果组件");
            }
        }

        return new ValidationResult(true, "验证通过");
    }

    /**
     * 验证结果
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }

    // ===== 计算 =====

    /**
     * 计算总灵力消耗
     */
    public double calculateTotalSpiritCost() {
        double cost = triggerType.getSpiritCost();  // 触发方式消耗

        for (SpellNode node : nodes) {
            cost += node.calculateSpiritCost();
        }

        return cost;
    }

    /**
     * 计算考虑玩家属性后的实际消耗
     */
    public double calculateActualCost(Player player) {
        // TODO: 获取玩家的灵根属性
        // 根据组件的属性匹配情况调整消耗
        return calculateTotalSpiritCost();
    }

    /**
     * 计算冷却时间
     */
    public double calculateCooldown() {
        return nodes.stream()
            .mapToDouble(SpellNode::calculateCooldown)
            .max()
            .orElse(0);
    }

    /**
     * 获取术法复杂度（用于界面显示）
     */
    public int getComplexity() {
        int complexity = 0;
        for (SpellNode node : nodes) {
            complexity += node.getComponents().size();
        }
        return complexity;
    }

    // ===== 序列化 =====

    /**
     * 序列化到NBT
     */
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        tag.putString("name", name);
        tag.putString("description", description);
        tag.putString("trigger", triggerType.name());

        if (creatorId != null) {
            tag.putUUID("creator", creatorId);
        }
        tag.putLong("created", createdTime);

        // 保存节点列表
        ListTag nodesList = new ListTag();
        for (SpellNode node : nodes) {
            nodesList.add(node.toNBT());
        }
        tag.put("nodes", nodesList);

        return tag;
    }

    /**
     * 从NBT反序列化
     */
    public static NodeSpell fromNBT(CompoundTag tag) {
        String id = tag.getString("id");
        String name = tag.getString("name");

        NodeSpell spell = new NodeSpell(id, name);
        spell.description = tag.getString("description");

        try {
            spell.triggerType = TriggerType.valueOf(tag.getString("trigger"));
        } catch (IllegalArgumentException e) {
            spell.triggerType = TriggerType.SELF;
        }

        if (tag.hasUUID("creator")) {
            spell.creatorId = tag.getUUID("creator");
        }
        spell.createdTime = tag.getLong("created");

        // 恢复节点
        ListTag nodesList = tag.getList("nodes", 10);  // 10 = COMPOUND
        for (int i = 0; i < nodesList.size(); i++) {
            CompoundTag nodeTag = nodesList.getCompound(i);
            SpellNode node = SpellNode.fromNBT(nodeTag);
            spell.nodes.add(node);
        }

        return spell;
    }

    /**
     * 复制术法
     */
    public NodeSpell copy() {
        NodeSpell copy = new NodeSpell(UUID.randomUUID().toString(), name + " (副本)");
        copy.description = this.description;
        copy.triggerType = this.triggerType;
        copy.creatorId = this.creatorId;

        for (SpellNode node : nodes) {
            copy.nodes.add(node.copy());
        }

        return copy;
    }

    @Override
    public String toString() {
        return name + " [" + triggerType.getDisplayName() + ", " + nodes.size() + "节点]";
    }
}
