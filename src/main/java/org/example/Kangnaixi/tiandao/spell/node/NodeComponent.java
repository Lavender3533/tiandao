package org.example.Kangnaixi.tiandao.spell.node;

import net.minecraft.nbt.CompoundTag;
import org.example.Kangnaixi.tiandao.cultivation.SpiritualRootType;
import org.example.Kangnaixi.tiandao.spell.node.target.SpellTargetKind;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * 节点组件 - 放在节点槽位里的基础单元
 *
 * 组件类型：
 * - PROJECTILE: 投射组件，定义传播/命中方式
 * - EFFECT: 效果组件，对目标施加效果
 * - MODIFIER: 修饰组件，修改术法执行参数
 */
public class NodeComponent {

    private final String id;
    private final String name;
    private final ComponentType type;
    private final String description;

    // 灵力属性
    @Nullable
    private final SpiritualRootType elementType;
    private final double elementBonus;  // 属性加成（匹配时的效果倍率）

    // 消耗
    private final double baseSpiritCost;
    private final double baseCooldown;

    // 参数（可调节）
    private final Map<String, ParameterDefinition> parameterDefinitions;
    private final Map<String, Object> currentParameters;

    // 图标颜色
    private final int color;

    private final String stageId;
    private final EnumSet<SpellTargetKind> allowedInputKinds;
    private final EnumSet<SpellTargetKind> producedTargetKinds;
    private final EnumSet<SpellTargetKind> supportedTargetKinds;
    private final boolean spawnsProjectileEntity;
    private final boolean allowEmptyTargets;

    private NodeComponent(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.type = builder.type;
        this.description = builder.description;
        this.elementType = builder.elementType;
        this.elementBonus = builder.elementBonus;
        this.baseSpiritCost = builder.baseSpiritCost;
        this.baseCooldown = builder.baseCooldown;
        this.parameterDefinitions = builder.parameterDefinitions;
        this.currentParameters = new HashMap<>(builder.defaultParameters);
        this.color = builder.color;
        this.stageId = builder.stageId;
        this.allowedInputKinds = builder.allowedInputKinds.clone();
        this.producedTargetKinds = builder.producedTargetKinds.clone();
        this.supportedTargetKinds = builder.supportedTargetKinds.clone();
        this.spawnsProjectileEntity = builder.spawnsProjectileEntity;
        this.allowEmptyTargets = builder.allowEmptyTargets;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public ComponentType getType() { return type; }
    public String getDescription() { return description; }
    public SpiritualRootType getElementType() { return elementType; }
    public double getElementBonus() { return elementBonus; }
    public double getBaseSpiritCost() { return baseSpiritCost; }
    public double getBaseCooldown() { return baseCooldown; }
    public int getColor() { return color; }
    public String getStageId() { return stageId; }
    public EnumSet<SpellTargetKind> getAllowedInputKinds() { return allowedInputKinds.clone(); }
    public EnumSet<SpellTargetKind> getProducedTargetKinds() { return producedTargetKinds.clone(); }
    public EnumSet<SpellTargetKind> getSupportedTargetKinds() { return supportedTargetKinds.clone(); }
    public boolean spawnsProjectileEntity() { return spawnsProjectileEntity; }
    public boolean allowEmptyTargets() { return allowEmptyTargets; }

    public boolean supportsTargetKind(SpellTargetKind kind) {
        return supportedTargetKinds.contains(kind);
    }

    public boolean producesTargetKind(SpellTargetKind kind) {
        return producedTargetKinds.contains(kind);
    }

    /**
     * 获取参数值
     */
    public <T> T getParameter(String key, Class<T> type) {
        Object value = currentParameters.get(key);
        if (value != null && type.isInstance(value)) {
            return type.cast(value);
        }
        return null;
    }

    /**
     * 设置参数值
     */
    public void setParameter(String key, Object value) {
        if (parameterDefinitions.containsKey(key)) {
            currentParameters.put(key, value);
        }
    }

    /**
     * 获取所有参数名称
     */
    public java.util.Set<String> getParameterNames() {
        return parameterDefinitions.keySet();
    }

    /**
     * 获取参数值（不指定类型）
     */
    public Object getParameter(String key) {
        return currentParameters.get(key);
    }

    /**
     * 计算实际灵力消耗（考虑参数）
     */
    public double calculateSpiritCost() {
        double cost = baseSpiritCost;

        // 参数影响消耗
        for (Map.Entry<String, ParameterDefinition> entry : parameterDefinitions.entrySet()) {
            String key = entry.getKey();
            ParameterDefinition def = entry.getValue();
            Object value = currentParameters.get(key);

            if (value instanceof Number) {
                double numValue = ((Number) value).doubleValue();
                double ratio = numValue / def.defaultValue;

                // 参数越大，消耗越高（平方关系）
                if (def.affectsCost) {
                    cost *= Math.pow(ratio, def.costMultiplier);
                }
            }
        }

        return cost;
    }

    /**
     * 获取属性匹配系数
     */
    public double getElementMatchBonus(SpiritualRootType playerElement) {
        if (elementType == null) {
            return 1.0;  // 无属性要求
        }
        if (elementType == playerElement) {
            return elementBonus;  // 属性匹配，获得加成
        }
        return 0.8;  // 属性不匹配，效果降低
    }

    /**
     * 序列化
     */
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);

        // 保存当前参数
        CompoundTag paramsTag = new CompoundTag();
        for (Map.Entry<String, Object> entry : currentParameters.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Double) {
                paramsTag.putDouble(entry.getKey(), (Double) value);
            } else if (value instanceof Integer) {
                paramsTag.putInt(entry.getKey(), (Integer) value);
            } else if (value instanceof String) {
                paramsTag.putString(entry.getKey(), (String) value);
            }
        }
        tag.put("parameters", paramsTag);

        return tag;
    }

    /**
     * 反序列化（需要ComponentRegistry支持）
     */
    public static NodeComponent fromNBT(CompoundTag tag) {
        String id = tag.getString("id");

        // 从注册表获取组件定义
        NodeComponent template = ComponentRegistry.getInstance().getComponent(id);
        if (template == null) {
            throw new IllegalStateException("未找到组件: " + id);
        }

        // 复制组件
        NodeComponent component = template.copy();

        // 恢复参数值
        if (tag.contains("parameters")) {
            CompoundTag paramsTag = tag.getCompound("parameters");
            for (String key : paramsTag.getAllKeys()) {
                if (paramsTag.contains(key, 6)) {  // 6 = DOUBLE
                    component.setParameter(key, paramsTag.getDouble(key));
                } else if (paramsTag.contains(key, 3)) {  // 3 = INT
                    component.setParameter(key, paramsTag.getInt(key));
                } else if (paramsTag.contains(key, 8)) {  // 8 = STRING
                    component.setParameter(key, paramsTag.getString(key));
                }
            }
        }

        return component;
    }

    /**
     * 复制组件（用于放入槽位时）
     */
    public NodeComponent copy() {
        NodeComponent copy = new Builder(id, name, type)
            .description(description)
            .element(elementType, elementBonus)
            .cost(baseSpiritCost, baseCooldown)
            .color(color)
            .stage(stageId)
            .allowedInputs(allowedInputKinds.toArray(new SpellTargetKind[0]))
            .produces(producedTargetKinds.toArray(new SpellTargetKind[0]))
            .supportedTargets(supportedTargetKinds.toArray(new SpellTargetKind[0]))
            .spawnsProjectileEntity(spawnsProjectileEntity)
            .allowEmptyTargets(allowEmptyTargets)
            .build();

        // 复制参数定义
        copy.parameterDefinitions.putAll(this.parameterDefinitions);
        copy.currentParameters.putAll(this.currentParameters);

        return copy;
    }

    // ===== Builder =====

    public static class Builder {
        private final String id;
        private final String name;
        private final ComponentType type;

        private String description = "";
        private SpiritualRootType elementType = null;
        private double elementBonus = 1.2;
        private double baseSpiritCost = 10.0;
        private double baseCooldown = 1.0;
        private Map<String, ParameterDefinition> parameterDefinitions = new HashMap<>();
        private Map<String, Object> defaultParameters = new HashMap<>();
        private int color = 0xFFFFFFFF;
        private String stageId = "";
        private EnumSet<SpellTargetKind> allowedInputKinds = EnumSet.noneOf(SpellTargetKind.class);
        private EnumSet<SpellTargetKind> producedTargetKinds = EnumSet.noneOf(SpellTargetKind.class);
        private EnumSet<SpellTargetKind> supportedTargetKinds = EnumSet.noneOf(SpellTargetKind.class);
        private boolean spawnsProjectileEntity = false;
        private boolean allowEmptyTargets = false;

        public Builder(String id, String name, ComponentType type) {
            this.id = id;
            this.name = name;
            this.type = type;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder element(SpiritualRootType elementType, double bonus) {
            this.elementType = elementType;
            this.elementBonus = bonus;
            return this;
        }

        public Builder cost(double spiritCost, double cooldown) {
            this.baseSpiritCost = spiritCost;
            this.baseCooldown = cooldown;
            return this;
        }

        public Builder parameter(String key, double defaultValue, double min, double max, boolean affectsCost) {
            ParameterDefinition def = new ParameterDefinition(key, defaultValue, min, max, affectsCost, 2.0);
            parameterDefinitions.put(key, def);
            defaultParameters.put(key, defaultValue);
            return this;
        }

        public Builder color(int color) {
            this.color = color;
            return this;
        }

        public Builder stage(String id) {
            this.stageId = id;
            return this;
        }

        public Builder allowedInputs(SpellTargetKind... kinds) {
            this.allowedInputKinds = EnumSet.noneOf(SpellTargetKind.class);
            if (kinds != null) {
                Collections.addAll(this.allowedInputKinds, kinds);
            }
            return this;
        }

        public Builder produces(SpellTargetKind... kinds) {
            this.producedTargetKinds = EnumSet.noneOf(SpellTargetKind.class);
            if (kinds != null) {
                Collections.addAll(this.producedTargetKinds, kinds);
            }
            return this;
        }

        public Builder supportedTargets(SpellTargetKind... kinds) {
            this.supportedTargetKinds = EnumSet.noneOf(SpellTargetKind.class);
            if (kinds != null) {
                Collections.addAll(this.supportedTargetKinds, kinds);
            }
            return this;
        }

        public Builder spawnsProjectileEntity(boolean value) {
            this.spawnsProjectileEntity = value;
            return this;
        }

        public Builder allowEmptyTargets(boolean value) {
            this.allowEmptyTargets = value;
            return this;
        }

        public NodeComponent build() {
            return new NodeComponent(this);
        }
    }

    // ===== 参数定义 =====

    public static class ParameterDefinition {
        public final String name;
        public final double defaultValue;
        public final double minValue;
        public final double maxValue;
        public final boolean affectsCost;  // 是否影响消耗
        public final double costMultiplier;  // 消耗倍率系数

        public ParameterDefinition(String name, double defaultValue, double minValue,
                                  double maxValue, boolean affectsCost, double costMultiplier) {
            this.name = name;
            this.defaultValue = defaultValue;
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.affectsCost = affectsCost;
            this.costMultiplier = costMultiplier;
        }
    }
}
