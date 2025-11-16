package org.example.Kangnaixi.tiandao.spell.node.execution;

import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.spell.node.execution.effect.*;
import org.example.Kangnaixi.tiandao.spell.node.execution.shape.*;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 执行器注册表 - 管理所有组件执行器
 */
public class ExecutorRegistry {

    private static ExecutorRegistry instance;

    private final Map<String, ComponentExecutor> executors = new LinkedHashMap<>();

    private ExecutorRegistry() {
        registerDefaultExecutors();
    }

    public static ExecutorRegistry getInstance() {
        if (instance == null) {
            instance = new ExecutorRegistry();
        }
        return instance;
    }

    /**
     * 注册默认执行器
     */
    private void registerDefaultExecutors() {
        // 形状执行器
        register(new CircleShapeExecutor());
        register(new ConeShapeExecutor());
        register(new LineShapeExecutor());

        // 效果执行器
        register(new DamageEffectExecutor());
        register(new HealEffectExecutor());
        register(new PushEffectExecutor());
        register(new ExplosionEffectExecutor());
        register(new TeleportEffectExecutor());

        Tiandao.LOGGER.info("已注册 {} 个组件执行器", executors.size());
    }

    /**
     * 注册执行器
     */
    public void register(ComponentExecutor executor) {
        if (executors.containsKey(executor.getId())) {
            Tiandao.LOGGER.warn("执行器 {} 已存在，将被覆盖", executor.getId());
        }
        executors.put(executor.getId(), executor);
        Tiandao.LOGGER.debug("注册执行器: {}", executor.getId());
    }

    /**
     * 根据ID获取执行器
     */
    @Nullable
    public ComponentExecutor getExecutor(String id) {
        return executors.get(id);
    }

    /**
     * 获取所有执行器
     */
    public Collection<ComponentExecutor> getAllExecutors() {
        return Collections.unmodifiableCollection(executors.values());
    }

    /**
     * 检查执行器是否存在
     */
    public boolean hasExecutor(String id) {
        return executors.containsKey(id);
    }

    /**
     * 清空注册表
     */
    public void clear() {
        executors.clear();
        Tiandao.LOGGER.info("执行器注册表已清空");
    }

    /**
     * 重新加载默认执行器
     */
    public void reload() {
        clear();
        registerDefaultExecutors();
    }
}
