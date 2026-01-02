package org.example.Kangnaixi.tiandao.client.starchart;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.starchart.StarNode;
import org.example.Kangnaixi.tiandao.starchart.StarNodeCategory;
import org.example.Kangnaixi.tiandao.starchart.StarTestNodes;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 星盘客户端管理器 - V3.0 星宫聚焦展开系统
 *
 * 状态：
 * - OVERVIEW: 总览状态，显示五大主节点（星宫）
 * - FOCUSED: 聚焦状态，展开某个星宫的子节点
 */
public class StarChartClientManager {
    private static StarChartClientManager instance;

    // ========== 状态枚举 ==========
    public enum State {
        OVERVIEW,   // 总览：显示所有主节点
        FOCUSED     // 聚焦：展开某个星宫
    }

    // ========== 基础状态 ==========
    private boolean enabled = false;
    private State currentState = State.OVERVIEW;

    // ========== 节点数据 ==========
    private final List<StarNodeInstance> allInstances = new ArrayList<>();
    private final Map<StarNodeCategory, StarNodeInstance> masterNodes = new HashMap<>();
    private final Map<StarNodeCategory, List<StarNodeInstance>> childNodes = new HashMap<>();

    // ========== 聚焦状态 ==========
    private StarNodeCategory focusedCategory = null;
    private float focusTransition = 0f;  // 0=总览, 1=完全聚焦
    private static final float FOCUS_LERP_ALPHA = 0.18f;  // 聚焦过渡阻尼系数
    private float focusedStateTime = 0f;  // 聚焦状态持续时间
    private static final float MIN_FOCUSED_DURATION = 1.5f;  // 最小聚焦持续时间（防止抖动）

    // ========== 延迟聚焦（手盘联动用） ==========
    private StarNodeCategory pendingFocusCategory = null;  // 等待聚焦的星宫
    private float pendingFocusDelay = 0f;  // 延迟计时器
    private static final float HANDWHEEL_LINK_DELAY = 0.15f;  // 手盘联动延迟（等手盘转一会儿）

    // ========== 注视系统 ==========
    private StarNodeInstance gazedNode = null;
    private float masterGazeTime = 0f;  // 注视主节点的时间
    private static final float MASTER_FOCUS_THRESHOLD = 0.8f;  // 聚焦阈值（秒）
    private static final float EXIT_FOCUS_THRESHOLD = 1.5f;  // 退出聚焦阈值（增加到1.5秒）

    // ========== 布局系统 ==========
    private final StarConstellationLayout layout = new StarConstellationLayout();

    // ========== 坐标系统 ==========
    private static final float STAR_CHART_DISTANCE = 2.5f;
    private static final float STAR_CHART_HEIGHT_OFFSET = 0.5f;
    private static final float GAZE_MAX_DISTANCE = 5.0f;

    private long lastUpdateTime = 0;
    private Vec3 baseForward = null;
    private Vec3 baseRight = null;
    private Vec3 baseUp = null;

    private StarChartClientManager() {}

    public static StarChartClientManager getInstance() {
        if (instance == null) {
            instance = new StarChartClientManager();
        }
        return instance;
    }

    public boolean toggle() {
        enabled = !enabled;

        if (enabled) {
            initializeInstances();
            cacheBaseOrientation();
            lastUpdateTime = System.currentTimeMillis();
            currentState = State.OVERVIEW;
            focusedCategory = null;
            focusTransition = 0f;
        } else {
            allInstances.clear();
            masterNodes.clear();
            childNodes.clear();
            gazedNode = null;
            currentState = State.OVERVIEW;
            focusedCategory = null;
            focusTransition = 0f;
            baseForward = null;
            baseRight = null;
            baseUp = null;
        }

        return enabled;
    }

    /**
     * 初始化所有节点实例
     */
    private void initializeInstances() {
        allInstances.clear();
        masterNodes.clear();
        childNodes.clear();

        List<StarNode> allNodes = StarTestNodes.getAllNodes();
        if (allNodes.isEmpty()) {
            Tiandao.LOGGER.warn("星盘节点列表为空");
            return;
        }

        // 按Category分组
        Map<StarNodeCategory, List<StarNode>> grouped = new HashMap<>();
        for (StarNode node : allNodes) {
            grouped.computeIfAbsent(node.getCategory(), k -> new ArrayList<>()).add(node);
        }

        // 创建节点实例
        for (StarNodeCategory category : StarNodeCategory.values()) {
            List<StarNode> nodesInCategory = grouped.get(category);
            if (nodesInCategory == null || nodesInCategory.isEmpty()) {
                continue;
            }

            // 创建主节点（显示类别名，如"效果"、"载体"等）
            StarNode masterNodeData = createMasterNode(category);
            StarNodeInstance masterInstance = new StarNodeInstance(masterNodeData);
            masterInstance.setMasterNode(true);
            masterInstance.setIndexInGroup(0);
            allInstances.add(masterInstance);
            masterNodes.put(category, masterInstance);

            // 所有具体节点都是子节点
            List<StarNodeInstance> children = new ArrayList<>();
            for (int j = 0; j < nodesInCategory.size(); j++) {
                StarNode node = nodesInCategory.get(j);
                StarNodeInstance instance = new StarNodeInstance(node);
                instance.setIndexInGroup(j + 1);  // 从1开始，0是主节点
                allInstances.add(instance);
                children.add(instance);
            }

            childNodes.put(category, children);
        }

        // 计算初始布局
        updateLayout();
    }

    /**
     * 创建主节点数据（显示类别名）
     */
    private StarNode createMasterNode(StarNodeCategory category) {
        String id = "master_" + category.name().toLowerCase();
        String name = category.getDisplayName();  // "效果"、"载体"、"形态"、"调制"、"成品"
        return new StarNode(id, name, null, category, 0, 0);
    }

    /**
     * 更新布局（根据当前状态）
     */
    private void updateLayout() {
        // 总览状态：主节点五角布局
        for (StarNodeCategory category : StarNodeCategory.values()) {
            StarNodeInstance master = masterNodes.get(category);
            if (master != null) {
                Vec3 pos = layout.getMasterOverviewPosition(category);
                master.setLocalPos(pos);
            }
        }

        // 子节点布局（聚焦时展开）
        if (focusedCategory != null) {
            List<StarNodeInstance> children = childNodes.get(focusedCategory);
            if (children != null) {
                List<Vec3> positions = layout.getChildGridPositions(children.size());
                for (int i = 0; i < children.size() && i < positions.size(); i++) {
                    children.get(i).setLocalPos(positions.get(i));
                }
            }
        }
    }

    /**
     * 计算世界坐标
     */
    public void calculateLayout(Vec3 playerPos, Vec3 lookDir) {
        if (!enabled || allInstances.isEmpty()) {
            return;
        }

        // 完全锁定朝向（打开时固定，不跟随）
        ensureBaseOrientation(lookDir);
        Vec3 forward = baseForward;
        Vec3 right = baseRight;
        Vec3 up = baseUp;

        Vec3 origin = playerPos.add(forward.scale(STAR_CHART_DISTANCE)).add(up.scale(STAR_CHART_HEIGHT_OFFSET));
        long gameTime = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0;

        // 更新所有节点的世界坐标
        for (StarNodeInstance instance : allInstances) {
            // 根据状态决定是否显示
            boolean shouldShow = shouldShowNode(instance);
            instance.setVisible(shouldShow);

            if (shouldShow) {
                instance.updateWorldPosition(origin, right, up, forward, gameTime);
            }
        }
    }

    /**
     * 判断节点是否应该显示
     */
    private boolean shouldShowNode(StarNodeInstance instance) {
        if (currentState == State.OVERVIEW) {
            // 总览状态：只显示主节点
            return instance.isMasterNode();
        } else {
            // 聚焦状态
            if (instance.isMasterNode()) {
                // 显示所有主节点（聚焦的放大，其他淡出）
                return true;
            } else {
                // 只显示聚焦星宫的子节点
                return instance.getNode().getCategory() == focusedCategory;
            }
        }
    }

    /**
     * 每帧更新
     */
    public void update() {
        if (!enabled || allInstances.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastUpdateTime) / 1000.0f;
        lastUpdateTime = currentTime;
        deltaTime = Math.min(deltaTime, 0.1f);

        // ========== 处理延迟聚焦（手盘联动） ==========
        updatePendingFocus(deltaTime);

        // 射线检测
        StarNodeInstance newGazedNode = performGazeRaycast(mc);
        gazedNode = newGazedNode;

        // 根据状态更新
        if (currentState == State.OVERVIEW) {
            updateOverviewState(newGazedNode, deltaTime);
        } else {
            focusedStateTime += deltaTime;  // 累积聚焦状态持续时间
            updateFocusedState(newGazedNode, deltaTime);
        }

        // 更新聚焦过渡（阻尼插值）
        float targetTransition = (currentState == State.FOCUSED) ? 1f : 0f;
        focusTransition = Mth.lerp(FOCUS_LERP_ALPHA, focusTransition, targetTransition);

        // 更新所有节点的渲染状态
        updateNodeRenderStates(deltaTime);
    }

    /**
     * 处理延迟聚焦（手盘联动用）
     */
    private void updatePendingFocus(float deltaTime) {
        if (pendingFocusCategory == null) {
            return;
        }

        pendingFocusDelay -= deltaTime;
        if (pendingFocusDelay <= 0) {
            // 延迟结束，执行聚焦
            enterFocusStateImmediate(pendingFocusCategory);
            pendingFocusCategory = null;
            pendingFocusDelay = 0f;
        }
    }

    /**
     * 总览状态更新
     */
    private void updateOverviewState(StarNodeInstance gazed, float deltaTime) {
        if (gazed != null && gazed.isMasterNode()) {
            // 正在注视主节点
            masterGazeTime += deltaTime;

            if (masterGazeTime >= MASTER_FOCUS_THRESHOLD) {
                // 触发聚焦（注视触发用直接聚焦，无延迟）
                enterFocusStateDirect(gazed.getNode().getCategory());
            }
        } else {
            masterGazeTime = 0f;
        }
    }

    /**
     * 聚焦状态更新
     */
    private void updateFocusedState(StarNodeInstance gazed, float deltaTime) {
        // 最小持续时间内不退出（防止抖动）
        if (focusedStateTime < MIN_FOCUSED_DURATION) {
            masterGazeTime = 0f;
            return;
        }

        // 检测是否注视空白区域或其他星宫（退出聚焦）
        boolean shouldExit = false;

        if (gazed == null) {
            // 注视空白区域
            shouldExit = true;
        } else if (gazed.isMasterNode() && gazed.getNode().getCategory() != focusedCategory) {
            // 注视其他主节点（切换星宫）
            shouldExit = true;
        } else {
            // 注视当前星宫的节点，重置退出计时
            masterGazeTime = 0f;
            return;
        }

        if (shouldExit) {
            masterGazeTime += deltaTime;
            if (masterGazeTime >= EXIT_FOCUS_THRESHOLD) {
                exitFocusState();
            }
        }
    }

    /**
     * 进入聚焦状态（带延迟，用于手盘联动）
     * 等待手盘旋转动画进行一段时间后再展开星宫
     */
    public void enterFocusState(StarNodeCategory category) {
        if (category == null) return;

        // 如果已经聚焦到同一个星宫，不重复触发
        if (currentState == State.FOCUSED && focusedCategory == category) {
            return;
        }

        // 如果已经有等待中的聚焦，更新目标（快速滚动时只响应最后一次）
        pendingFocusCategory = category;
        pendingFocusDelay = HANDWHEEL_LINK_DELAY;
    }

    /**
     * 立即进入聚焦状态（内部使用，无延迟）
     */
    private void enterFocusStateImmediate(StarNodeCategory category) {
        if (category == null) return;

        // 如果当前已经聚焦其他星宫，先快速收缩
        if (currentState == State.FOCUSED && focusedCategory != category) {
            List<StarNodeInstance> oldChildren = childNodes.get(focusedCategory);
            if (oldChildren != null) {
                for (StarNodeInstance child : oldChildren) {
                    child.startCollapseAnimation();
                }
            }
        }

        currentState = State.FOCUSED;
        focusedCategory = category;
        masterGazeTime = 0f;
        focusedStateTime = 0f;

        // 重新计算布局
        updateLayout();

        // 初始化子节点展开动画（加快展开速度，减少延迟间隔）
        List<StarNodeInstance> children = childNodes.get(category);
        if (children != null) {
            for (int i = 0; i < children.size(); i++) {
                StarNodeInstance child = children.get(i);
                child.startExpandAnimation(i * 0.03f);  // 更快的依次展开（原0.05f）
            }
        }
    }

    /**
     * 直接聚焦（用于注视触发，无延迟）
     */
    public void enterFocusStateDirect(StarNodeCategory category) {
        if (category == null) return;
        // 清除任何等待中的延迟聚焦
        pendingFocusCategory = null;
        pendingFocusDelay = 0f;
        enterFocusStateImmediate(category);
    }

    /**
     * 退出聚焦状态
     */
    public void exitFocusState() {
        // 收缩子节点
        if (focusedCategory != null) {
            List<StarNodeInstance> children = childNodes.get(focusedCategory);
            if (children != null) {
                for (StarNodeInstance child : children) {
                    child.startCollapseAnimation();
                }
            }
        }

        currentState = State.OVERVIEW;
        focusedCategory = null;
        masterGazeTime = 0f;
        focusedStateTime = 0f;
    }

    /**
     * 更新所有节点的渲染状态
     */
    private void updateNodeRenderStates(float deltaTime) {
        for (StarNodeInstance instance : allInstances) {
            if (!instance.isVisible()) {
                continue;
            }

            boolean isGazed = instance == gazedNode;

            if (instance.isMasterNode()) {
                // 主节点渲染状态
                updateMasterNodeState(instance, isGazed, deltaTime);
            } else {
                // 子节点渲染状态
                updateChildNodeState(instance, isGazed, deltaTime);
            }

            instance.updateAnimation(deltaTime);
        }
    }

    /**
     * 更新主节点状态
     */
    private void updateMasterNodeState(StarNodeInstance master, boolean isGazed, float deltaTime) {
        StarNodeCategory category = master.getNode().getCategory();

        if (currentState == State.OVERVIEW) {
            // 总览状态
            if (isGazed) {
                float progress = Math.min(1.0f, masterGazeTime / MASTER_FOCUS_THRESHOLD);
                master.setTargetScale(1.0f + progress * 0.3f);  // 1.0 → 1.3
                master.setTargetAlpha(1.0f);
            } else {
                master.setTargetScale(1.0f);
                master.setTargetAlpha(1.0f);
            }
            master.setTargetOffset(Vec3.ZERO);
        } else {
            // 聚焦状态
            if (category == focusedCategory) {
                // 被聚焦的主节点：放大 + 移动到顶部中央
                Vec3 currentPos = layout.getMasterOverviewPosition(category);
                Vec3 targetPos = layout.getFocusedMasterPosition();
                Vec3 offset = new Vec3(
                    targetPos.x - currentPos.x,
                    targetPos.y - currentPos.y,
                    targetPos.z - currentPos.z
                );
                master.setTargetScale(1.5f);
                master.setTargetAlpha(1.0f);
                master.setTargetOffset(offset);
            } else {
                // 其他主节点：快速消失
                master.setTargetScale(0.0f);
                master.setTargetAlpha(0.0f);
                master.setTargetOffset(new Vec3(0, -0.2, 0.1));
            }
        }
    }

    /**
     * 更新子节点状态
     */
    private void updateChildNodeState(StarNodeInstance child, boolean isGazed, float deltaTime) {
        if (currentState == State.FOCUSED && child.getNode().getCategory() == focusedCategory) {
            child.setHighlighted(false);  // 不用呼吸效果
            if (isGazed) {
                // 选中时放大
                child.setTargetScale(1.25f);
                child.setTargetAlpha(1.0f);
            } else {
                child.setTargetScale(1.0f);
                child.setTargetAlpha(0.85f);
            }
            child.setTargetOffset(Vec3.ZERO);
        } else {
            child.setHighlighted(false);
            child.setTargetScale(0f);
            child.setTargetAlpha(0f);
            child.setTargetOffset(Vec3.ZERO);
        }
    }

    /**
     * 使用 AABB 碰撞检测从准星拾取节点
     * 比角度检测更准确，准星对准哪个节点就选中哪个
     *
     * 性能优化：
     * 1. 提前过滤不可见节点
     * 2. 距离粗筛选后再做AABB检测
     */
    private StarNodeInstance performGazeRaycast(Minecraft mc) {
        if (mc.player == null || mc.gameRenderer.getMainCamera() == null) {
            return null;
        }

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 rayStart = camera.getPosition();
        Vec3 lookVec = new Vec3(camera.getLookVector());
        Vec3 rayEnd = rayStart.add(lookVec.scale(GAZE_MAX_DISTANCE));

        StarNodeInstance hit = null;
        double bestDist = GAZE_MAX_DISTANCE;

        // 性能优化：预先筛选候选节点
        for (StarNodeInstance instance : allInstances) {
            // 快速过滤：可见性和透明度
            if (!instance.isVisible()) continue;
            if (instance.getCurrentAlpha() < 0.3f) continue;

            // 快速过滤：子节点展开进度
            if (!instance.isMasterNode() && instance.getExpandProgress() < 0.5f) continue;

            // 快速过滤：缩放
            if (instance.getCurrentScale() < 0.3f) continue;

            // 粗筛选：距离检测（避免对远离的节点做昂贵的AABB计算）
            Vector3f nodePos = instance.getWorldPos();
            double dx = nodePos.x - rayStart.x;
            double dy = nodePos.y - rayStart.y;
            double dz = nodePos.z - rayStart.z;
            double distSq = dx * dx + dy * dy + dz * dz;

            if (distSq > GAZE_MAX_DISTANCE * GAZE_MAX_DISTANCE) continue;

            // 精确检测：AABB碰撞
            AABB box = instance.getBoundingBox();
            Optional<Vec3> clipResult = box.clip(rayStart, rayEnd);

            if (clipResult.isPresent()) {
                double dist = clipResult.get().distanceTo(rayStart);
                if (dist < bestDist) {
                    bestDist = dist;
                    hit = instance;
                }
            }
        }

        return hit;
    }

    private void cacheBaseOrientation() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        Vec3 look = mc.player.getLookAngle().normalize();
        buildBasis(look);
    }

    private void ensureBaseOrientation(Vec3 fallbackLook) {
        if (baseForward == null || baseRight == null || baseUp == null) {
            buildBasis(fallbackLook.normalize());
        }
    }

    private void buildBasis(Vec3 forward) {
        baseForward = forward;
        baseRight = forward.cross(new Vec3(0, 1, 0));
        if (baseRight.lengthSqr() < 1.0e-6) {
            baseRight = new Vec3(1, 0, 0);
        } else {
            baseRight = baseRight.normalize();
        }
        baseUp = baseRight.cross(baseForward).normalize();
    }

    private static final float ORIENTATION_LERP_SPEED = 3.0f;  // 跟随速度

    /**
     * 平滑更新朝向（只跟随玩家身体朝向，忽略上下看）
     */
    private void updateOrientationSmooth(Vec3 targetLook) {
        if (baseForward == null) {
            // 初始化时使用水平方向
            Vec3 horizontalLook = new Vec3(targetLook.x, 0, targetLook.z).normalize();
            if (horizontalLook.lengthSqr() < 0.01) {
                horizontalLook = new Vec3(0, 0, -1);
            }
            buildBasis(horizontalLook);
            return;
        }

        // 只取水平方向（忽略pitch，只跟随yaw）
        Vec3 targetForward = new Vec3(targetLook.x, 0, targetLook.z);
        if (targetForward.lengthSqr() < 0.01) {
            return;  // 几乎垂直看，不更新
        }
        targetForward = targetForward.normalize();

        Vec3 targetRight = targetForward.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 targetUp = new Vec3(0, 1, 0);  // 固定向上

        // 平滑插值
        long currentTime = System.currentTimeMillis();
        float dt = Math.min(0.1f, (currentTime - lastUpdateTime) / 1000.0f);
        float t = Math.min(1.0f, dt * ORIENTATION_LERP_SPEED);

        baseForward = lerpVec3(baseForward, targetForward, t).normalize();
        baseRight = lerpVec3(baseRight, targetRight, t).normalize();
        baseUp = targetUp;  // 始终向上
    }

    private Vec3 lerpVec3(Vec3 a, Vec3 b, float t) {
        return new Vec3(
            a.x + (b.x - a.x) * t,
            a.y + (b.y - a.y) * t,
            a.z + (b.z - a.z) * t
        );
    }

    // ========== Getters ==========
    public boolean isEnabled() { return enabled; }
    public State getCurrentState() { return currentState; }
    public StarNodeCategory getFocusedCategory() { return focusedCategory; }
    public float getFocusTransition() { return focusTransition; }
    public StarNodeInstance getGazedNode() { return gazedNode; }
    public List<StarNodeInstance> getInstances() { return allInstances; }
    public Map<StarNodeCategory, StarNodeInstance> getMasterNodes() { return masterNodes; }
    public StarNodeInstance getMasterNode(StarNodeCategory category) { return masterNodes.get(category); }
    public List<StarNodeInstance> getChildNodes(StarNodeCategory category) { return childNodes.get(category); }
    public float getDistance() { return STAR_CHART_DISTANCE; }
    public StarConstellationLayout getLayout() { return layout; }
    public Vec3 getBaseForward() { return baseForward; }
    public Vec3 getBaseRight() { return baseRight; }
    public Vec3 getBaseUp() { return baseUp; }

    public StarNodeInstance getFocusedNode() {
        for (StarNodeInstance instance : allInstances) {
            if (instance.isFocused()) return instance;
        }
        return null;
    }
}
