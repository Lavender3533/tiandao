package org.example.Kangnaixi.tiandao.client.starchart;

import net.minecraft.world.phys.Vec3;
import org.example.Kangnaixi.tiandao.starchart.StarNode;
import org.example.Kangnaixi.tiandao.starchart.StarNodeCategory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 星宫布局系统 - V4.1 弧形布局（4类别）
 *
 * 布局：从左到右弧形排列
 * EFFECT → FORM → MODIFIER → BLUEPRINT
 */
public class StarConstellationLayout {

    // ========== 主节点总览布局（4类别弧形）==========
    private static final Map<StarNodeCategory, Vec3> MASTER_OVERVIEW_POSITIONS = new HashMap<>();

    static {
        // 4类别弧形布局：从左到右，中间高两边低
        // EFFECT - 左侧
        MASTER_OVERVIEW_POSITIONS.put(StarNodeCategory.EFFECT, new Vec3(-0.7, 0.3, 0.05));

        // FORM - 左中（较高）
        MASTER_OVERVIEW_POSITIONS.put(StarNodeCategory.FORM, new Vec3(-0.2, 0.65, -0.08));

        // MODIFIER - 右中（较高）
        MASTER_OVERVIEW_POSITIONS.put(StarNodeCategory.MODIFIER, new Vec3(0.2, 0.65, -0.08));

        // BLUEPRINT - 右侧
        MASTER_OVERVIEW_POSITIONS.put(StarNodeCategory.BLUEPRINT, new Vec3(0.7, 0.3, 0.05));
    }

    // ========== 子节点布局参数 ==========
    private static final int GRID_COLUMNS = 4;
    private static final float GRID_CELL_WIDTH = 0.4f;
    private static final float GRID_CELL_HEIGHT = 0.35f;

    private final Map<String, Vec3> nodePositions = new HashMap<>();

    public Map<String, Vec3> calculateLayout(List<StarNode> allNodes) {
        nodePositions.clear();

        Map<StarNodeCategory, List<StarNode>> grouped = new HashMap<>();
        for (StarNode node : allNodes) {
            grouped.computeIfAbsent(node.getCategory(), k -> new ArrayList<>()).add(node);
        }

        for (StarNodeCategory category : StarNodeCategory.values()) {
            List<StarNode> nodes = grouped.get(category);
            if (nodes != null && !nodes.isEmpty()) {
                nodePositions.put(nodes.get(0).getId(), getMasterOverviewPosition(category));
            }
        }

        return nodePositions;
    }

    public Vec3 getMasterOverviewPosition(StarNodeCategory category) {
        return MASTER_OVERVIEW_POSITIONS.getOrDefault(category, Vec3.ZERO);
    }

    /**
     * 获取子节点的展开位置（相对于屏幕中心，主节点会移到上方）
     */
    public List<Vec3> getChildGridPositions(int childCount) {
        List<Vec3> positions = new ArrayList<>();
        if (childCount == 0) return positions;

        int rows = (int) Math.ceil((double) childCount / GRID_COLUMNS);

        for (int i = 0; i < childCount; i++) {
            int row = i / GRID_COLUMNS;
            int col = i % GRID_COLUMNS;

            // 当前行的节点数
            int colsInThisRow = (row == rows - 1) ? 
                (childCount % GRID_COLUMNS == 0 ? GRID_COLUMNS : childCount % GRID_COLUMNS) : GRID_COLUMNS;

            // X坐标：居中
            float totalWidth = (colsInThisRow - 1) * GRID_CELL_WIDTH;
            float startX = -totalWidth / 2f;
            float x = startX + col * GRID_CELL_WIDTH;

            // Y坐标：从主节点下方开始往下排列
            float y = 0.2f - row * GRID_CELL_HEIGHT;

            // Z坐标：轻微前后错开
            float z = (row % 2 == 0 ? -0.02f : 0.02f);

            positions.add(new Vec3(x, y, z));
        }

        return positions;
    }

    /**
     * 聚焦时主节点的目标位置（居中上方）
     */
    public Vec3 getFocusedMasterPosition() {
        return new Vec3(0.0, 0.75, -0.1);
    }

    public Vec3 getLocalPosition(String nodeId) {
        return nodePositions.getOrDefault(nodeId, Vec3.ZERO);
    }

    public Vec3 getMasterPos(StarNodeCategory category) {
        return getMasterOverviewPosition(category);
    }
}
