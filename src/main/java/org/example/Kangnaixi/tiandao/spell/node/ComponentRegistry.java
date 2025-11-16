package org.example.Kangnaixi.tiandao.spell.node;

import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.cultivation.SpiritualRootType;
import org.example.Kangnaixi.tiandao.spell.node.target.SpellTargetKind;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 组件注册表 - 管理所有可用的节点组件
 */
public class ComponentRegistry {

    private static ComponentRegistry instance;

    private final Map<String, NodeComponent> components = new LinkedHashMap<>();

    private ComponentRegistry() {
        registerDefaultComponents();
    }

    public static ComponentRegistry getInstance() {
        if (instance == null) {
            instance = new ComponentRegistry();
        }
        return instance;
    }

    /**
     * 注册默认组件
     */
    private void registerDefaultComponents() {
        // ===== 投射组件 =====

        register(new NodeComponent.Builder("circle", "圆形", ComponentType.PROJECTILE)
            .description("以位置为中心的球形范围")
            .cost(10.0, 0.5)
            .parameter("radius", 5.0, 1.0, 20.0, true)
            .stage("tiandao:stage/circle")
            .allowedInputs(SpellTargetKind.POINT)
            .produces(SpellTargetKind.ENTITY, SpellTargetKind.AREA)
            .color(0xFF4FC3F7)
            .build());

        register(new NodeComponent.Builder("cone", "扇形", ComponentType.PROJECTILE)
            .description("前方扇形范围")
            .cost(12.0, 0.5)
            .parameter("distance", 10.0, 3.0, 30.0, true)
            .parameter("angle", 45.0, 15.0, 180.0, true)
            .stage("tiandao:stage/cone")
            .allowedInputs(SpellTargetKind.POINT)
            .produces(SpellTargetKind.ENTITY)
            .color(0xFF29B6F6)
            .build());

        register(new NodeComponent.Builder("line", "直线", ComponentType.PROJECTILE)
            .description("直线轨迹")
            .cost(8.0, 0.3)
            .parameter("distance", 15.0, 5.0, 40.0, true)
            .stage("tiandao:stage/ray")
            .allowedInputs(SpellTargetKind.POINT)
            .produces(SpellTargetKind.ENTITY, SpellTargetKind.POINT)
            .color(0xFF26C6DA)
            .build());

        // ===== 效果组件 =====

        // 伤害
        register(new NodeComponent.Builder("damage", "伤害", ComponentType.EFFECT)
            .description("对目标造成伤害")
            .cost(10.0, 1.0)
            .parameter("damage", 5.0, 1.0, 20.0, true)
            .supportedTargets(SpellTargetKind.ENTITY)
            .color(0xFFFF6B6B)
            .build());

        // 治疗
        register(new NodeComponent.Builder("heal", "治疗", ComponentType.EFFECT)
            .description("恢复目标生命值")
            .element(SpiritualRootType.WOOD, 1.3)  // 木属性加成
            .cost(10.0, 1.5)
            .parameter("healing", 5.0, 1.0, 20.0, true)
            .supportedTargets(SpellTargetKind.ENTITY)
            .color(0xFF4CAF50)
            .build());

        // 推动
        register(new NodeComponent.Builder("push", "推动", ComponentType.EFFECT)
            .description("击退目标")
            .cost(8.0, 0.8)
            .parameter("force", 1.5, 0.5, 5.0, true)
            .supportedTargets(SpellTargetKind.ENTITY)
            .color(0xFF9E9E9E)
            .build());

        // 传送
        register(new NodeComponent.Builder("teleport", "传送", ComponentType.EFFECT)
            .description("传送目标或自身")
            .cost(15.0, 3.0)
            .parameter("distance", 10.0, 3.0, 30.0, true)
            .supportedTargets(SpellTargetKind.ENTITY, SpellTargetKind.POINT)
            .color(0xFF9C27B0)
            .build());

        // 爆炸
        register(new NodeComponent.Builder("explosion", "爆炸", ComponentType.EFFECT)
            .description("产生爆炸")
            .element(SpiritualRootType.FIRE, 1.3)  // 火属性加成
            .cost(20.0, 5.0)
            .parameter("power", 3.0, 1.0, 10.0, true)
            .supportedTargets(SpellTargetKind.POINT, SpellTargetKind.AREA)
            .color(0xFFFF5722)
            .build());

        Tiandao.LOGGER.info("已注册 {} 个默认组件", components.size());
    }

    /**
     * 注册组件
     */
    public void register(NodeComponent component) {
        if (components.containsKey(component.getId())) {
            Tiandao.LOGGER.warn("组件 {} 已存在，将被覆盖", component.getId());
        }
        components.put(component.getId(), component);
        Tiandao.LOGGER.debug("注册组件: {} ({})", component.getName(), component.getId());
    }

    /**
     * 根据ID获取组件
     */
    @Nullable
    public NodeComponent getComponent(String id) {
        return components.get(id);
    }

    /**
     * 获取所有组件
     */
    public Collection<NodeComponent> getAllComponents() {
        return Collections.unmodifiableCollection(components.values());
    }

    /**
     * 根据类型获取组件
     */
    public List<NodeComponent> getComponentsByType(ComponentType type) {
        return components.values().stream()
            .filter(c -> c.getType() == type)
            .toList();
    }

    /**
     * 获取投射组件
     */
    public List<NodeComponent> getProjectileComponents() {
        return getComponentsByType(ComponentType.PROJECTILE);
    }

    /**
     * @deprecated 形状组件已被投射组件替代
     */
    @Deprecated
    public List<NodeComponent> getShapeComponents() {
        return getProjectileComponents();
    }

    /**
     * 获取效果组件
     */
    public List<NodeComponent> getEffectComponents() {
        return getComponentsByType(ComponentType.EFFECT);
    }

    /**
     * 获取修饰组件
     */
    public List<NodeComponent> getModifierComponents() {
        return getComponentsByType(ComponentType.MODIFIER);
    }

    /**
     * 检查组件是否存在
     */
    public boolean hasComponent(String id) {
        return components.containsKey(id);
    }

    /**
     * 清空注册表
     */
    public void clear() {
        components.clear();
        Tiandao.LOGGER.info("组件注册表已清空");
    }

    /**
     * 重新加载默认组件
     */
    public void reload() {
        clear();
        registerDefaultComponents();
    }
}
