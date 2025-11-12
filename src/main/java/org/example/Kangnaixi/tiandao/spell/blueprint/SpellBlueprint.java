package org.example.Kangnaixi.tiandao.spell.blueprint;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.cultivation.CultivationRealm;
import org.example.Kangnaixi.tiandao.spell.SpellLocalization;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Locale;

/**
 * 术法蓝图的基础数据结构（支持可扩展的高级配置）
 */
public class SpellBlueprint {

    private static final Gson ADVANCED_GSON = new GsonBuilder()
        .disableHtmlEscaping()
        .serializeNulls()
        .setPrettyPrinting()
        .create();

    public enum ElementType {
        FIRE, WATER, EARTH, METAL, WOOD, LIGHTNING, WIND, VOID
    }

    public enum EffectType {
        DAMAGE, HEALING, CONTROL, UTILITY, SUMMON
    }

    public enum TargetingType {
        SELF,
        TARGET_ENTITY,
        TARGET_BLOCK,
        DIRECTIONAL_RELEASE,
        AREA_RELEASE
    }

    public enum ShapeType {
        SELF_AURA,
        LINE,
        CONE,
        SPHERE,
        PROJECTILE,
        TARGET_AREA
    }

    private final String id;
    private final String name;
    private final String description;
    private final ElementType element;
    private final EffectType effectType;
    private final TargetingType targeting;
    private final double basePower;
    private final double spiritCost;
    private final double cooldownSeconds;
    private final double range;
    private final double areaRadius;
    private final CultivationRealm requiredRealm;
    private final int requiredSubRealmLevel;
    private final List<ElementType> elementVariants;
    @Nullable
    private final AdvancedData advancedData;

    public SpellBlueprint(String id,
                          String name,
                          String description,
                          ElementType element,
                          EffectType effectType,
                          TargetingType targeting,
                          double basePower,
                          double spiritCost,
                          double cooldownSeconds,
                          double range,
                          double areaRadius,
                          CultivationRealm requiredRealm,
                          int requiredSubRealmLevel) {
        this(id, name, description, element, effectType, targeting, basePower, spiritCost,
            cooldownSeconds, range, areaRadius, requiredRealm, requiredSubRealmLevel, null);
    }

    public SpellBlueprint(String id,
                          String name,
                          String description,
                          ElementType element,
                          EffectType effectType,
                          TargetingType targeting,
                          double basePower,
                          double spiritCost,
                          double cooldownSeconds,
                          double range,
                          double areaRadius,
                          CultivationRealm requiredRealm,
                          int requiredSubRealmLevel,
                          @Nullable AdvancedData advancedData) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.element = element;
        this.effectType = effectType;
        this.targeting = targeting;
        this.basePower = basePower;
        this.spiritCost = spiritCost;
        this.cooldownSeconds = cooldownSeconds;
        this.range = range;
        this.areaRadius = areaRadius;
        this.requiredRealm = requiredRealm;
        this.requiredSubRealmLevel = requiredSubRealmLevel;
        AdvancedData copied = advancedData != null ? advancedData.copy() : null;
        this.advancedData = copied;
        if (copied != null && !copied.getElements().isEmpty()) {
            this.elementVariants = Collections.unmodifiableList(new ArrayList<>(copied.getElements()));
        } else {
            this.elementVariants = Collections.singletonList(element);
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ElementType getElement() {
        return element;
    }

    public List<ElementType> getElements() {
        return elementVariants;
    }

    public EffectType getEffectType() {
        return effectType;
    }

    public TargetingType getTargeting() {
        return targeting;
    }

    public double getBasePower() {
        return basePower;
    }

    public double getSpiritCost() {
        return spiritCost;
    }

    public double getCooldownSeconds() {
        return cooldownSeconds;
    }

    public double getRange() {
        return range;
    }

    public double getAreaRadius() {
        return areaRadius;
    }

    public CultivationRealm getRequiredRealm() {
        return requiredRealm;
    }

    public int getRequiredSubRealmLevel() {
        return requiredSubRealmLevel;
    }

    public Optional<AdvancedData> getAdvancedData() {
        return Optional.ofNullable(advancedData);
    }

    @Nullable
    public AdvancedData copyAdvancedData() {
        return advancedData != null ? advancedData.copy() : null;
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        tag.putString("name", name);
        tag.putString("description", description);
        tag.putString("element", element.name());
        tag.putString("effectType", effectType.name());
        tag.putString("targeting", targeting.name());
        tag.putDouble("basePower", basePower);
        tag.putDouble("spiritCost", spiritCost);
        tag.putDouble("cooldownSeconds", cooldownSeconds);
        tag.putDouble("range", range);
        tag.putDouble("areaRadius", areaRadius);
        tag.putString("requiredRealm", requiredRealm.name());
        tag.putInt("requiredSubRealmLevel", requiredSubRealmLevel);
        if (advancedData != null) {
            tag.putString("advancedData", ADVANCED_GSON.toJson(advancedData));
        }
        return tag;
    }

    public static SpellBlueprint fromNBT(CompoundTag tag) {
        AdvancedData advancedData = null;
        if (tag.contains("advancedData", Tag.TAG_STRING)) {
            try {
                advancedData = ADVANCED_GSON.fromJson(tag.getString("advancedData"), AdvancedData.class);
            } catch (Exception e) {
                Tiandao.LOGGER.warn("反序列化术法蓝图高级配置失败: {}", e.getMessage());
            }
        }
        return new SpellBlueprint(
            tag.getString("id"),
            tag.getString("name"),
            tag.getString("description"),
            ElementType.valueOf(tag.getString("element")),
            EffectType.valueOf(tag.getString("effectType")),
            TargetingType.valueOf(tag.getString("targeting")),
            tag.getDouble("basePower"),
            tag.getDouble("spiritCost"),
            tag.getDouble("cooldownSeconds"),
            tag.getDouble("range"),
            tag.getDouble("areaRadius"),
            CultivationRealm.valueOf(tag.getString("requiredRealm")),
            tag.getInt("requiredSubRealmLevel"),
            advancedData
        );
    }

    /**
     * 生成简短描述，用于玉简提示或命令反馈
     */
    public Component getFormattedSummary() {
        return SpellLocalization.gui("summary",
            name,
            SpellLocalization.element(element),
            SpellLocalization.effect(effectType),
            String.format(Locale.ROOT, "%.1f", spiritCost),
            String.format(Locale.ROOT, "%.1f", cooldownSeconds));
    }

    public static String serializeAdvancedData(AdvancedData data) {
        return ADVANCED_GSON.toJson(data);
    }

    public static AdvancedData deserializeAdvancedData(String json) {
        return ADVANCED_GSON.fromJson(json, AdvancedData.class);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SpellBlueprint)) return false;
        SpellBlueprint that = (SpellBlueprint) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * 蓝图高级配置（JSON驱动）
     */
    public static class AdvancedData {
        private List<ElementType> elements = new ArrayList<>();
        private ShapeConfig shape = new ShapeConfig();
        private List<SegmentConfig> segments = new ArrayList<>();
        private List<StatusEffectConfig> statusEffects = new ArrayList<>();
        private List<VisualLayerConfig> visualLayers = new ArrayList<>();
        private List<AudioCueConfig> audioCues = new ArrayList<>();
        private RequirementConfig requirements = new RequirementConfig();

        public List<ElementType> getElements() {
            return elements == null ? Collections.emptyList() : Collections.unmodifiableList(elements);
        }

        public void setElements(List<ElementType> elements) {
            this.elements = elements == null ? new ArrayList<>() : new ArrayList<>(elements);
        }

        public ShapeConfig getShape() {
            return shape;
        }

        public void setShape(ShapeConfig shape) {
            this.shape = shape == null ? new ShapeConfig() : shape;
        }

        public List<SegmentConfig> getSegments() {
            return segments == null ? Collections.emptyList() : Collections.unmodifiableList(segments);
        }

        public List<SegmentConfig> editableSegments() {
            return segments;
        }

        public void setSegments(List<SegmentConfig> segments) {
            this.segments = segments == null ? new ArrayList<>() : new ArrayList<>(segments);
        }

        public List<StatusEffectConfig> getStatusEffects() {
            return statusEffects == null ? Collections.emptyList() : Collections.unmodifiableList(statusEffects);
        }

        public List<StatusEffectConfig> editableStatusEffects() {
            return statusEffects;
        }

        public void setStatusEffects(List<StatusEffectConfig> statusEffects) {
            this.statusEffects = statusEffects == null ? new ArrayList<>() : new ArrayList<>(statusEffects);
        }

        public List<VisualLayerConfig> getVisualLayers() {
            return visualLayers == null ? Collections.emptyList() : Collections.unmodifiableList(visualLayers);
        }

        public List<VisualLayerConfig> editableVisualLayers() {
            return visualLayers;
        }

        public void setVisualLayers(List<VisualLayerConfig> visualLayers) {
            this.visualLayers = visualLayers == null ? new ArrayList<>() : new ArrayList<>(visualLayers);
        }

        public List<AudioCueConfig> getAudioCues() {
            return audioCues == null ? Collections.emptyList() : Collections.unmodifiableList(audioCues);
        }

        public void setAudioCues(List<AudioCueConfig> audioCues) {
            this.audioCues = audioCues == null ? new ArrayList<>() : new ArrayList<>(audioCues);
        }

        public RequirementConfig getRequirements() {
            return requirements;
        }

        public void setRequirements(RequirementConfig requirements) {
            this.requirements = requirements == null ? new RequirementConfig() : requirements;
        }

        public AdvancedData copy() {
            return ADVANCED_GSON.fromJson(ADVANCED_GSON.toJson(this), AdvancedData.class);
        }
    }

    public static class ShapeConfig {
        private ShapeType type = ShapeType.SELF_AURA;
        private double length = 0.0;
        private double radius = 0.0;
        private double angle = 0.0;
        private double width = 0.0;

        public ShapeType getType() {
            return type;
        }

        public void setType(ShapeType type) {
            this.type = type == null ? ShapeType.SELF_AURA : type;
        }

        public double getLength() {
            return length;
        }

        public void setLength(double length) {
            this.length = length;
        }

        public double getRadius() {
            return radius;
        }

        public void setRadius(double radius) {
            this.radius = radius;
        }

        public double getAngle() {
            return angle;
        }

        public void setAngle(double angle) {
            this.angle = angle;
        }

        public double getWidth() {
            return width;
        }

        public void setWidth(double width) {
            this.width = width;
        }
    }

    public static class SegmentConfig {
        private String name = "";
        private double powerMultiplier = 1.0;
        private int durationTicks = 0;
        private int delayTicks = 0;
        private double complexityWeight = 1.0;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public double getPowerMultiplier() {
            return powerMultiplier;
        }

        public void setPowerMultiplier(double powerMultiplier) {
            this.powerMultiplier = powerMultiplier;
        }

        public int getDurationTicks() {
            return durationTicks;
        }

        public void setDurationTicks(int durationTicks) {
            this.durationTicks = durationTicks;
        }

        public int getDelayTicks() {
            return delayTicks;
        }

        public void setDelayTicks(int delayTicks) {
            this.delayTicks = delayTicks;
        }

        public double getComplexityWeight() {
            return complexityWeight;
        }

        public void setComplexityWeight(double complexityWeight) {
            this.complexityWeight = complexityWeight;
        }
    }

    public static class StatusEffectConfig {
        private String effectId = "";
        private int amplifier = 0;
        private int durationTicks = 0;
        private boolean ambient = false;
        private boolean visible = true;

        public String getEffectId() {
            return effectId;
        }

        public void setEffectId(String effectId) {
            this.effectId = effectId;
        }

        public int getAmplifier() {
            return amplifier;
        }

        public void setAmplifier(int amplifier) {
            this.amplifier = amplifier;
        }

        public int getDurationTicks() {
            return durationTicks;
        }

        public void setDurationTicks(int durationTicks) {
            this.durationTicks = durationTicks;
        }

        public boolean isAmbient() {
            return ambient;
        }

        public void setAmbient(boolean ambient) {
            this.ambient = ambient;
        }

        public boolean isVisible() {
            return visible;
        }

        public void setVisible(boolean visible) {
            this.visible = visible;
        }
    }

    public static class VisualLayerConfig {
        private String layerType = "";
        private String color = "#FFFFFF";
        private double scale = 1.0;
        private String particle = "";
        private double intensity = 1.0;
        private String texture = "";

        public String getLayerType() {
            return layerType;
        }

        public void setLayerType(String layerType) {
            this.layerType = layerType;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }

        public double getScale() {
            return scale;
        }

        public void setScale(double scale) {
            this.scale = scale;
        }

        public String getParticle() {
            return particle;
        }

        public void setParticle(String particle) {
            this.particle = particle;
        }

        public double getIntensity() {
            return intensity;
        }

        public void setIntensity(double intensity) {
            this.intensity = intensity;
        }

        public String getTexture() {
            return texture;
        }

        public void setTexture(String texture) {
            this.texture = texture;
        }
    }

    public static class AudioCueConfig {
        private String soundEvent = "";
        private float volume = 1.0f;
        private float pitch = 1.0f;
        private String trigger = "cast";

        public String getSoundEvent() {
            return soundEvent;
        }

        public float getVolume() {
            return volume;
        }

        public float getPitch() {
            return pitch;
        }

        public String getTrigger() {
            return trigger;
        }
    }

    public static class RequirementConfig {
        private double baseSpiritCost = 0.0;
        private double baseCooldown = 0.0;
        private double complexity = 1.0;
        private double overloadFactor = 1.0;
        private CultivationRealm minRealm = CultivationRealm.QI_CONDENSATION;
        private int minSubRealmLevel = 0;
        private List<MaterialRequirement> materials = new ArrayList<>();

        public double getBaseSpiritCost() {
            return baseSpiritCost;
        }

        public void setBaseSpiritCost(double baseSpiritCost) {
            this.baseSpiritCost = baseSpiritCost;
        }

        public double getBaseCooldown() {
            return baseCooldown;
        }

        public void setBaseCooldown(double baseCooldown) {
            this.baseCooldown = baseCooldown;
        }

        public double getComplexity() {
            return complexity;
        }

        public void setComplexity(double complexity) {
            this.complexity = complexity;
        }

        public double getOverloadFactor() {
            return overloadFactor;
        }

        public void setOverloadFactor(double overloadFactor) {
            this.overloadFactor = overloadFactor;
        }

        public CultivationRealm getMinRealm() {
            return minRealm;
        }

        public void setMinRealm(CultivationRealm minRealm) {
            this.minRealm = minRealm == null ? CultivationRealm.QI_CONDENSATION : minRealm;
        }

        public int getMinSubRealmLevel() {
            return minSubRealmLevel;
        }

        public void setMinSubRealmLevel(int minSubRealmLevel) {
            this.minSubRealmLevel = minSubRealmLevel;
        }

        public List<MaterialRequirement> getMaterials() {
            return materials == null ? Collections.emptyList() : Collections.unmodifiableList(materials);
        }

        public void setMaterials(List<MaterialRequirement> materials) {
            this.materials = materials == null ? new ArrayList<>() : new ArrayList<>(materials);
        }
    }

    public static class MaterialRequirement {
        private String itemId = "";
        private int amount = 0;
        private String tier = "";

        public String getItemId() {
            return itemId;
        }

        public int getAmount() {
            return amount;
        }

        public String getTier() {
            return tier;
        }
    }
}
