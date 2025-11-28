package org.example.Kangnaixi.tiandao.client.mindsea;

import java.util.ArrayList;
import java.util.List;

/**
 * 识海内视模式 - 状态管理（4列垂直光球拼装系统）
 * 4列光球，底部4个槽位，纯鼠标交互
 */
public class MindSeaHoloState {

    private static MindSeaHoloState instance;

    /**
     * 光球数据
     */
    public static class OrbData {
        private final String id;
        private final String displayName;
        private final int color;

        public OrbData(String id, String displayName, int color) {
            this.id = id;
            this.displayName = displayName;
            this.color = color;
        }

        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        public int getColor() { return color; }
    }

    /**
     * 组数据（每组包含多个光球选项，环绕在特定方位）
     */
    public static class GroupData {
        private final String title;           // 组标题
        private final int color;              // 组颜色
        private final List<OrbData> orbs;     // 光球选项
        private final float baseYaw;          // 基础偏航角（相对玩家朝向）
        private final float arcSpan;          // 弧线跨度（度数）

        public GroupData(String title, int color, float baseYaw, float arcSpan) {
            this.title = title;
            this.color = color;
            this.baseYaw = baseYaw;
            this.arcSpan = arcSpan;
            this.orbs = new ArrayList<>();
        }

        public String getTitle() { return title; }
        public int getColor() { return color; }
        public List<OrbData> getOrbs() { return orbs; }
        public float getBaseYaw() { return baseYaw; }
        public float getArcSpan() { return arcSpan; }
    }

    // ==================== 状态字段 ====================

    private boolean enabled = false;
    private final List<GroupData> groups = new ArrayList<>(5);    // 5组环绕光球
    private final OrbData[] selectedOrbs = new OrbData[5];        // 底部5个槽位
    private int hoveredGroup = -1;                                // 鼠标hover的组
    private int hoveredOrbIndex = -1;                             // 鼠标hover的光球索引
    private float radius = 2.0f;                                  // 环绕半径（紧贴玩家）

    // ==================== 单例 ====================

    public static MindSeaHoloState getInstance() {
        if (instance == null) {
            instance = new MindSeaHoloState();
        }
        return instance;
    }

    private MindSeaHoloState() {
        initializeGroups();
    }

    /**
     * 初始化5组环绕光球（固定在世界空间，玩家需转身对准）
     */
    private void initializeGroups() {
        // 第1组：起手式（固定在左前方，-45度世界方向，弧线跨度30度）
        GroupData group1 = new GroupData("起手式", 0xFFFFD700, -45f, 22f);
        group1.orbs.add(new OrbData("self", "自身", 0xFFFFD700));
        group1.orbs.add(new OrbData("array", "阵盘", 0xFFFFD700));
        group1.orbs.add(new OrbData("artifact", "法器", 0xFFFFD700));
        groups.add(group1);

        // 第2组：释放方式（固定在右前方，+45度世界方向，弧线跨度40度）
        GroupData group2 = new GroupData("释放方式", 0xFF40E0D0, 45f, 22f);
        group2.orbs.add(new OrbData("projectile", "投射", 0xFF40E0D0));
        group2.orbs.add(new OrbData("line", "直线", 0xFF40E0D0));
        group2.orbs.add(new OrbData("sphere", "球体", 0xFF40E0D0));
        group2.orbs.add(new OrbData("ground", "地面", 0xFF40E0D0));
        groups.add(group2);

        // 第3组：形状（固定在左后方，-135度世界方向，弧线跨度40度）
        GroupData group3 = new GroupData("形状", 0xFFB0E0FF, -135f, 22f);
        group3.orbs.add(new OrbData("ball", "球体", 0xFFB0E0FF));
        group3.orbs.add(new OrbData("wall", "墙体", 0xFFB0E0FF));
        group3.orbs.add(new OrbData("ring", "环绕", 0xFFB0E0FF));
        group3.orbs.add(new OrbData("wave", "冲击波", 0xFFB0E0FF));
        groups.add(group3);

        // 第4组：属性（固定在正后方，180度世界方向，弧线跨度40度）
        GroupData group4 = new GroupData("属性", 0xFFDA70D6, 180f, 22f);
        group4.orbs.add(new OrbData("fire", "火", 0xFFDA70D6));
        group4.orbs.add(new OrbData("water", "水", 0xFFDA70D6));
        group4.orbs.add(new OrbData("thunder", "雷", 0xFFDA70D6));
        group4.orbs.add(new OrbData("ice", "冰", 0xFFDA70D6));
        groups.add(group4);

        // 第5组：效果（固定在右后方，+135度世界方向，弧线跨度40度）
        GroupData group5 = new GroupData("效果", 0xFFFFE4B5, 135f, 22f);
        group5.orbs.add(new OrbData("explode", "爆炸", 0xFFFFE4B5));
        group5.orbs.add(new OrbData("ignite", "点燃", 0xFFFFE4B5));
        group5.orbs.add(new OrbData("knockback", "击退", 0xFFFFE4B5));
        group5.orbs.add(new OrbData("slow", "减速", 0xFFFFE4B5));
        group5.orbs.add(new OrbData("shield", "护盾", 0xFFFFE4B5));
        groups.add(group5);
    }

    // ==================== 状态控制 ====================

    public void toggle() {
        enabled = !enabled;
        if (!enabled) {
            reset();
        }
    }

    public void enable() {
        enabled = true;
    }

    public void disable() {
        enabled = false;
        reset();
    }

    private void reset() {
        radius = 2.0f;
        hoveredGroup = -1;
        hoveredOrbIndex = -1;
        for (int i = 0; i < selectedOrbs.length; i++) {
            selectedOrbs[i] = null;
        }
    }

    // ==================== Getters & Setters ====================

    public boolean isEnabled() {
        return enabled;
    }

    public List<GroupData> getGroups() {
        return groups;
    }

    public OrbData[] getSelectedOrbs() {
        return selectedOrbs;
    }

    public int getHoveredGroup() {
        return hoveredGroup;
    }

    public int getHoveredOrbIndex() {
        return hoveredOrbIndex;
    }

    public void setHoveredOrb(int group, int orbIndex) {
        this.hoveredGroup = group;
        this.hoveredOrbIndex = orbIndex;
    }

    public float getRadius() {
        return radius;
    }

    public void adjustRadius(float delta) {
        radius = Math.max(1.5f, Math.min(4.0f, radius + delta));
    }

    /**
     * 选中光球到对应槽位
     */
    public void selectOrb(int groupIndex, int orbIndex) {
        if (groupIndex >= 0 && groupIndex < groups.size()) {
            GroupData group = groups.get(groupIndex);
            if (orbIndex >= 0 && orbIndex < group.getOrbs().size()) {
                selectedOrbs[groupIndex] = group.getOrbs().get(orbIndex);
            }
        }
    }
}
