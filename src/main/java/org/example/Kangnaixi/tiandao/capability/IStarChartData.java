package org.example.Kangnaixi.tiandao.capability;

import java.util.Set;

/**
 * 星盘数据接口
 * 建议位置: org.example.Kangnaixi.tiandao.capability
 */
public interface IStarChartData {
    /**
     * 获取已解锁的节点ID集合
     */
    Set<String> getUnlockedNodes();

    /**
     * 解锁一个节点
     * @return true if newly unlocked, false if already unlocked
     */
    boolean unlockNode(String nodeId);

    /**
     * 检查节点是否已解锁
     */
    boolean isNodeUnlocked(String nodeId);

    /**
     * 清空所有已解锁节点（用于重置）
     */
    void clearAll();
}
