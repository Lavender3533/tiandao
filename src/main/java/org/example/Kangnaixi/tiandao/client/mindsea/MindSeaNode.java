package org.example.Kangnaixi.tiandao.client.mindsea;

/**
 * 识海节点数据
 * 表示识海中的一个功能节点（Source/Carrier/Form/Effect）
 */
public class MindSeaNode {

    /** 节点类型 */
    public enum NodeType {
        SOURCE("起手式", 0xFFE6A23C),      // 金色
        CARRIER("载体", 0xFF409EFF),       // 蓝色
        FORM("术式", 0xFF67C23A),          // 绿色
        EFFECT("效果", 0xFFC0392B);        // 朱砂色

        public final String displayName;
        public final int color;

        NodeType(String displayName, int color) {
            this.displayName = displayName;
            this.color = color;
        }
    }

    private final String id;
    private final String displayName;
    private final NodeType type;
    private final String description;

    // 3D位置（相对于玩家头部的偏移，会根据yaw/pitch/radius动态计算）
    private float relativeX;
    private float relativeY;
    private float relativeZ;

    // 状态标记
    private boolean selected = false;
    private boolean hovered = false;
    private float scaleAnimation = 1.0f;

    public MindSeaNode(String id, String displayName, NodeType type, String description) {
        this.id = id;
        this.displayName = displayName;
        this.type = type;
        this.description = description;
    }

    // ==================== Getters & Setters ====================

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public NodeType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public float getRelativeX() {
        return relativeX;
    }

    public float getRelativeY() {
        return relativeY;
    }

    public float getRelativeZ() {
        return relativeZ;
    }

    public void setRelativePosition(float x, float y, float z) {
        this.relativeX = x;
        this.relativeY = y;
        this.relativeZ = z;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isHovered() {
        return hovered;
    }

    public void setHovered(boolean hovered) {
        this.hovered = hovered;
    }

    public float getScaleAnimation() {
        return scaleAnimation;
    }

    public void setScaleAnimation(float scaleAnimation) {
        this.scaleAnimation = scaleAnimation;
    }

    /**
     * 更新动画状态（每帧调用）
     */
    public void updateAnimation(float partialTick) {
        float targetScale = hovered ? 1.1f : (selected ? 1.05f : 1.0f);
        float speed = 0.2f;
        scaleAnimation += (targetScale - scaleAnimation) * speed;
    }
}
