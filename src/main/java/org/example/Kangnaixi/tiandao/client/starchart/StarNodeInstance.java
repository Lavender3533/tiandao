package org.example.Kangnaixi.tiandao.client.starchart;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.example.Kangnaixi.tiandao.starchart.StarNode;
import org.example.Kangnaixi.tiandao.starchart.StarNodeCategory;
import org.joml.Vector3f;

/**
 * 星盘节点运行时实例 - V3.0 聚焦展开系统
 */
public class StarNodeInstance {

    // ========== 动画配置 ==========
    private static final float FLOAT_AMPLITUDE = 0.015f;
    private static final float FLOAT_SPEED = 0.04f;
    private static final float LERP_ALPHA = 0.2f;  // 阻尼插值系数（每帧靠近目标20%）
    private static final float LERP_THRESHOLD = 0.001f;  // 插值阈值（足够接近时直接设置）

    // ========== 静态数据 ==========
    private final StarNode node;
    private final ItemStack displayItem;
    private boolean isMasterNode = false;
    private int indexInGroup = 0;

    // ========== 位置 ==========
    private Vec3 localPos = Vec3.ZERO;
    private Vec3 targetOffset = Vec3.ZERO;    // 动画偏移目标
    private Vec3 currentOffset = Vec3.ZERO;   // 当前动画偏移
    private final Vector3f worldPos = new Vector3f();

    // ========== 可见性 ==========
    private boolean visible = true;

    // ========== 渲染状态 ==========
    private float currentScale = 1.0f;
    private float targetScale = 1.0f;
    private float currentAlpha = 1.0f;
    private float targetAlpha = 1.0f;

    // ========== 注视系统 ==========
    private float gazeTime = 0f;
    private boolean isFocused = false;
    public static final float GAZE_THRESHOLD = 2.0f;
    private static final float GAZE_DECAY_RATE = 2.0f;
    public static final float HIT_RADIUS = 0.45f;  // 大碰撞半径，容易选中

    // ========== 展开动画 ==========
    private float expandProgress = 0f;      // 0=收缩, 1=展开
    private float expandDelay = 0f;         // 展开延迟
    private boolean expanding = false;
    private boolean collapsing = false;
    private static final float EXPAND_SPEED = 5.0f;  // 展开速度（舒适的速度）

    // ========== 脉冲高亮 ==========
    private float pulsePhase = 0f;
    private boolean isHighlighted = false;

    public StarNodeInstance(StarNode node) {
        this.node = node;
        this.displayItem = createDisplayItem(node);
    }

    private ItemStack createDisplayItem(StarNode node) {
        StarNodeCategory category = node.getCategory();
        return switch (category) {
            case EFFECT -> new ItemStack(Items.BLAZE_POWDER);
            case FORM -> new ItemStack(Items.ARROW);
            case MODIFIER -> new ItemStack(Items.GLOWSTONE_DUST);
            case BLUEPRINT -> new ItemStack(Items.ENCHANTED_BOOK);
        };
    }

    /**
     * 更新世界坐标
     */
    public void updateWorldPosition(Vec3 origin, Vec3 right, Vec3 up, Vec3 forward, long gameTime) {
        // 轻微浮动
        float idleOffset = (float) Math.sin((gameTime + indexInGroup * 9) * FLOAT_SPEED) * FLOAT_AMPLITUDE;

        // 计算最终本地坐标（基础 + 偏移 + 浮动）
        double lx = localPos.x + currentOffset.x;
        double ly = localPos.y + currentOffset.y + idleOffset;
        double lz = localPos.z + currentOffset.z;

        // 转换到世界坐标
        double wx = origin.x + right.x * lx + up.x * ly - forward.x * lz;
        double wy = origin.y + right.y * lx + up.y * ly - forward.y * lz;
        double wz = origin.z + right.z * lx + up.z * ly - forward.z * lz;

        worldPos.set((float) wx, (float) wy, (float) wz);
    }

    /**
     * 更新动画状态（阻尼插值，参考手盘的舒适缓动）
     */
    public void updateAnimation(float deltaTime) {
        // 阻尼插值：每帧靠近目标一定比例（与帧率无关的平滑效果）
        // 参考手盘的 ROTATION_SPEED = 0.15f

        // 平滑插值缩放
        float scaleDiff = targetScale - currentScale;
        if (Math.abs(scaleDiff) < LERP_THRESHOLD) {
            currentScale = targetScale;
        } else {
            currentScale = Mth.lerp(LERP_ALPHA, currentScale, targetScale);
        }

        // 平滑插值透明度（减少时更快）
        float alphaDiff = targetAlpha - currentAlpha;
        if (Math.abs(alphaDiff) < LERP_THRESHOLD) {
            currentAlpha = targetAlpha;
        } else {
            // 消失（减少）比出现（增加）快3倍
            float alphaSpeed = (alphaDiff < 0) ? LERP_ALPHA * 3.0f : LERP_ALPHA;
            currentAlpha = Mth.lerp(alphaSpeed, currentAlpha, targetAlpha);
        }

        // 平滑插值偏移
        double offsetDiffX = targetOffset.x - currentOffset.x;
        double offsetDiffY = targetOffset.y - currentOffset.y;
        double offsetDiffZ = targetOffset.z - currentOffset.z;
        double offsetDistSq = offsetDiffX * offsetDiffX + offsetDiffY * offsetDiffY + offsetDiffZ * offsetDiffZ;

        if (offsetDistSq < LERP_THRESHOLD * LERP_THRESHOLD) {
            currentOffset = targetOffset;
        } else {
            currentOffset = new Vec3(
                Mth.lerp(LERP_ALPHA, currentOffset.x, targetOffset.x),
                Mth.lerp(LERP_ALPHA, currentOffset.y, targetOffset.y),
                Mth.lerp(LERP_ALPHA, currentOffset.z, targetOffset.z)
            );
        }

        // 展开动画
        if (expanding) {
            if (expandDelay > 0) {
                expandDelay -= deltaTime;
            } else {
                expandProgress = Math.min(1f, expandProgress + deltaTime * EXPAND_SPEED);
                if (expandProgress >= 1f) {
                    expanding = false;
                }
            }
        }

        // 收缩动画
        if (collapsing) {
            expandProgress = Math.max(0f, expandProgress - deltaTime * EXPAND_SPEED * 1.5f);
            if (expandProgress <= 0f) {
                collapsing = false;
            }
        }

        // 脉冲高亮动画
        if (isHighlighted) {
            pulsePhase += deltaTime * 6.0f;  // 脉冲速度
            if (pulsePhase > Math.PI * 2) {
                pulsePhase -= (float)(Math.PI * 2);
            }
        }
    }

    /**
     * 更新注视状态（兼容旧接口）
     */
    public void updateGaze(boolean isGazed, boolean isSameCategory, float dimStrength, float deltaTime) {
        if (isGazed) {
            gazeTime = Math.min(gazeTime + deltaTime, GAZE_THRESHOLD);
        } else {
            gazeTime = Math.max(gazeTime - deltaTime * GAZE_DECAY_RATE, 0f);
        }

        isFocused = gazeTime >= GAZE_THRESHOLD;
    }

    /**
     * 开始展开动画
     */
    public void startExpandAnimation(float delay) {
        this.expandDelay = delay;
        this.expanding = true;
        this.collapsing = false;
        this.expandProgress = 0f;
    }

    /**
     * 开始收缩动画
     */
    public void startCollapseAnimation() {
        this.collapsing = true;
        this.expanding = false;
    }

    /**
     * 获取节点的碰撞包围盒（AABB）
     * 用于准星射线检测
     */
    public AABB getBoundingBox() {
        // 碰撞盒大小和渲染大小匹配
        // 渲染时: BASE_SCALE(0.55) * currentScale * 物品模型(~0.25) ≈ 0.14 * currentScale
        double baseRadius = 0.12;  // 基础半径
        double r = baseRadius * currentScale;
        
        // 子节点需要考虑展开进度
        if (!isMasterNode) {
            r *= Math.max(0.3, expandProgress);
        }
        
        // 最小碰撞半径
        r = Math.max(r, 0.08);
        
        return new AABB(
            worldPos.x - r, worldPos.y - r, worldPos.z - r,
            worldPos.x + r, worldPos.y + r, worldPos.z + r
        );
    }

    /**
     * @deprecated 使用 getBoundingBox() + AABB.clip() 代替
     */
    @Deprecated
    public boolean raycast(Vector3f rayOrigin, Vector3f rayDir, float maxDistance) {
        return false;  // 不再使用
    }

    // ========== Getters & Setters ==========

    public StarNode getNode() { return node; }
    public ItemStack getDisplayItem() { return displayItem; }
    public boolean isMasterNode() { return isMasterNode; }
    public int getIndexInGroup() { return indexInGroup; }
    public Vec3 getLocalPos() { return localPos; }
    public Vector3f getWorldPos() { return worldPos; }
    public boolean isVisible() { return visible; }
    public float getCurrentScale() { return currentScale; }
    public float getCurrentAlpha() { return currentAlpha; }
    public float getGazeTime() { return gazeTime; }
    public boolean isFocused() { return isFocused; }
    public float getExpandProgress() { return expandProgress; }
    public float getPulseValue() { return isHighlighted ? (float)(0.5 + 0.5 * Math.sin(pulsePhase)) : 0f; }
    public boolean isHighlighted() { return isHighlighted; }

    public void setMasterNode(boolean master) { this.isMasterNode = master; }
    public void setHighlighted(boolean highlighted) { this.isHighlighted = highlighted; if (!highlighted) pulsePhase = 0f; }
    public void setIndexInGroup(int index) { this.indexInGroup = index; }
    public void setLocalPos(Vec3 pos) { this.localPos = pos; }
    public void setVisible(boolean visible) { this.visible = visible; }
    public void setTargetScale(float scale) { this.targetScale = scale; }
    public void setTargetAlpha(float alpha) { this.targetAlpha = alpha; }
    public void setTargetOffset(Vec3 offset) { this.targetOffset = offset; }

    public void setWorldPos(float x, float y, float z) { worldPos.set(x, y, z); }
    public void setWorldPos(Vector3f pos) { worldPos.set(pos); }

    public void update(float deltaTime, int tickCount) {
        // 预留
    }

    // 兼容旧接口
    public float getPullProgress() { return 0f; }
}
