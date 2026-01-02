package org.example.Kangnaixi.tiandao.handwheel;

import net.minecraft.nbt.CompoundTag;
import org.example.Kangnaixi.tiandao.starchart.StarNode;
import org.example.Kangnaixi.tiandao.starchart.StarNodeCategory;
import org.example.Kangnaixi.tiandao.starchart.StarTestNodes;

import javax.annotation.Nullable;

/**
 * 手盘槽位 - 存储单个组合节点
 */
public class HandWheelSlot {
    private final StarNodeCategory acceptedCategory;
    private String nodeId;
    private boolean locked;

    public HandWheelSlot(StarNodeCategory acceptedCategory) {
        this.acceptedCategory = acceptedCategory;
        this.nodeId = null;
        this.locked = false;
    }

    public StarNodeCategory getAcceptedCategory() {
        return acceptedCategory;
    }

    public boolean isEmpty() {
        return nodeId == null || nodeId.isEmpty();
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    @Nullable
    public String getNodeId() {
        return nodeId;
    }

    @Nullable
    public StarNode getNode() {
        if (nodeId == null) return null;
        return StarTestNodes.getNodeById(nodeId);
    }

    /**
     * 设置节点（需要验证类别匹配）
     * @return true if set successfully
     */
    public boolean setNode(String nodeId) {
        if (locked) return false;
        if (nodeId == null) {
            this.nodeId = null;
            return true;
        }

        StarNode node = StarTestNodes.getNodeById(nodeId);
        if (node == null) return false;
        if (node.getCategory() != acceptedCategory) return false;

        this.nodeId = nodeId;
        return true;
    }

    public void clear() {
        if (!locked) {
            this.nodeId = null;
        }
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("category", acceptedCategory.name());
        tag.putString("nodeId", nodeId != null ? nodeId : "");
        tag.putBoolean("locked", locked);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        String id = tag.getString("nodeId");
        this.nodeId = id.isEmpty() ? null : id;
        this.locked = tag.getBoolean("locked");
    }

    @Override
    public String toString() {
        StarNode node = getNode();
        return acceptedCategory.getDisplayName() + ": " + (node != null ? node.getName() : "空");
    }
}
