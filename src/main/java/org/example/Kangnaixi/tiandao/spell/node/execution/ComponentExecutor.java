package org.example.Kangnaixi.tiandao.spell.node.execution;

import org.example.Kangnaixi.tiandao.spell.node.NodeComponent;

/**
 * 组件执行器接口
 *
 * 每个组件类型都需要实现此接口来定义具体的执行逻辑
 */
public interface ComponentExecutor {

    /**
     * 执行组件
     *
     * @param component 要执行的组件
     * @param context 执行上下文
     */
    void execute(NodeComponent component, NodeSpellContext context);

    /**
     * 获取执行器ID（通常与组件ID相同）
     */
    String getId();

    /**
     * 是否需要目标实体
     * @return true表示需要context中有affectedEntities
     */
    default boolean requiresTargets() {
        return false;
    }
}
