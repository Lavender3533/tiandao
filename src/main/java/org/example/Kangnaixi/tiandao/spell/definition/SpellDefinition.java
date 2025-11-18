package org.example.Kangnaixi.tiandao.spell.definition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import org.example.Kangnaixi.tiandao.cultivation.CultivationRealm;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 数据驱动的修仙术法定义。每个定义由 Source/Carrier/Form
 * 三段式骨架 + 属性/效果层 + 元数据组成。
 */
public final class SpellDefinition {

    private final ResourceLocation id;
    private final Component source;
    private final Component carrier;
    private final Component form;
    private final List<Attribute> attributes;
    private final List<Effect> effects;
    private final Numbers baseStats;
    private final Metadata metadata;
    @Nullable
    private final SwordQiOverride swordQiOverride;

    public SpellDefinition(ResourceLocation id,
                           Component source,
                           Component carrier,
                           Component form,
                           List<Attribute> attributes,
                           List<Effect> effects,
                           Numbers baseStats,
                           Metadata metadata,
                           @Nullable SwordQiOverride swordQiOverride) {
        this.id = id;
        this.source = source;
        this.carrier = carrier;
        this.form = form;
        this.attributes = Collections.unmodifiableList(new ArrayList<>(attributes));
        this.effects = Collections.unmodifiableList(new ArrayList<>(effects));
        this.baseStats = baseStats;
        this.metadata = metadata;
        this.swordQiOverride = swordQiOverride;
    }

    public ResourceLocation getId() {
        return id;
    }

    public Component getSource() {
        return source;
    }

    public Component getCarrier() {
        return carrier;
    }

    public Component getForm() {
        return form;
    }

    public List<Attribute> getAttributes() {
        return attributes;
    }

    public List<Effect> getEffects() {
        return effects;
    }

    public Numbers getBaseStats() {
        return baseStats;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public Optional<SwordQiOverride> getSwordQiOverride() {
        return Optional.ofNullable(swordQiOverride);
    }

    public boolean isSwordQiCarrier() {
        return carrier.hasTag("sword_qi") || carrier.id().getPath().contains("sword_qi");
    }

    public boolean hasAttributeTag(String tag) {
        for (Attribute attribute : attributes) {
            if (attribute.tags().contains(tag)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 将 SpellDefinition 序列化为 JSON 对象
     */
    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("id", id.toString());

        // 骨架组件
        root.add("source", componentToJson(source));
        root.add("carrier", componentToJson(carrier));
        root.add("form", componentToJson(form));

        // 属性
        JsonArray attrsArray = new JsonArray();
        for (Attribute attr : attributes) {
            attrsArray.add(attributeToJson(attr));
        }
        root.add("attributes", attrsArray);

        // 效果
        JsonArray effectsArray = new JsonArray();
        for (Effect effect : effects) {
            effectsArray.add(effectToJson(effect));
        }
        root.add("effects", effectsArray);

        // 基础属性
        root.add("base_stats", numbersToJson(baseStats));

        // 元数据
        root.add("metadata", metadataToJson(metadata));

        // 剑修强化
        if (swordQiOverride != null) {
            root.add("sword_qi", swordQiOverrideToJson(swordQiOverride));
        }

        return root;
    }

    private JsonObject componentToJson(Component component) {
        JsonObject json = new JsonObject();
        json.addProperty("id", component.id().toString());
        json.addProperty("display_name", component.displayName());
        json.addProperty("description", component.description());

        if (!component.numericParameters().isEmpty()) {
            JsonObject params = new JsonObject();
            component.numericParameters().forEach(params::addProperty);
            json.add("parameters", params);
        }

        if (!component.tags().isEmpty()) {
            JsonArray tags = new JsonArray();
            component.tags().forEach(tags::add);
            json.add("tags", tags);
        }

        return json;
    }

    private JsonObject attributeToJson(Attribute attribute) {
        JsonObject json = new JsonObject();
        json.addProperty("id", attribute.id().toString());
        json.addProperty("display_name", attribute.displayName());
        json.addProperty("layer", attribute.layer().name());
        json.addProperty("magnitude", attribute.magnitude());

        if (!attribute.scaling().isEmpty()) {
            JsonObject scaling = new JsonObject();
            attribute.scaling().forEach(scaling::addProperty);
            json.add("scaling", scaling);
        }

        if (!attribute.tags().isEmpty()) {
            JsonArray tags = new JsonArray();
            attribute.tags().forEach(tags::add);
            json.add("tags", tags);
        }

        return json;
    }

    private JsonObject effectToJson(Effect effect) {
        JsonObject json = new JsonObject();
        json.addProperty("id", effect.id().toString());
        json.addProperty("display_name", effect.displayName());

        if (!effect.payload().isEmpty()) {
            JsonObject payload = new JsonObject();
            effect.payload().forEach(payload::addProperty);
            json.add("payload", payload);
        }

        if (!effect.tags().isEmpty()) {
            JsonArray tags = new JsonArray();
            effect.tags().forEach(tags::add);
            json.add("tags", tags);
        }

        return json;
    }

    private JsonObject numbersToJson(Numbers numbers) {
        JsonObject json = new JsonObject();
        json.addProperty("damage", numbers.baseDamage());
        json.addProperty("speed", numbers.projectileSpeed());
        json.addProperty("range", numbers.areaRange());
        json.addProperty("channel_ticks", numbers.channelTicks());
        json.addProperty("duration_ticks", numbers.durationTicks());
        json.addProperty("cooldown", numbers.cooldownSeconds());
        json.addProperty("spirit_cost", numbers.spiritCost());
        return json;
    }

    private JsonObject metadataToJson(Metadata metadata) {
        JsonObject json = new JsonObject();
        json.addProperty("name", metadata.displayName());
        json.addProperty("description", metadata.description());
        if (metadata.requiredRealm() != null) {
            json.addProperty("required_realm", metadata.requiredRealm().name());
        }
        json.addProperty("required_stage", metadata.requiredStage());
        json.addProperty("rarity", metadata.rarity());

        if (!metadata.unlockTags().isEmpty()) {
            JsonArray tags = new JsonArray();
            metadata.unlockTags().forEach(tags::add);
            json.add("unlock_tags", tags);
        }

        return json;
    }

    private JsonObject swordQiOverrideToJson(SwordQiOverride override) {
        JsonObject json = new JsonObject();
        json.addProperty("damage_multiplier", override.damageMultiplier());
        json.addProperty("speed_multiplier", override.speedMultiplier());
        json.addProperty("range_multiplier", override.rangeMultiplier());

        if (override.requiredAttributeTag().isPresent()) {
            json.addProperty("required_attribute_tag", override.requiredAttributeTag().get());
        }

        if (!override.extraEffects().isEmpty()) {
            JsonArray effects = new JsonArray();
            for (Effect effect : override.extraEffects()) {
                effects.add(effectToJson(effect));
            }
            json.add("extra_effects", effects);
        }

        return json;
    }

    public record Component(ResourceLocation id,
                            String displayName,
                            String description,
                            Map<String, Double> numericParameters,
                            Set<String> tags) {

        public Component {
            numericParameters = Collections.unmodifiableMap(new LinkedHashMap<>(numericParameters));
            tags = Collections.unmodifiableSet(new LinkedHashSet<>(tags));
        }

        public double getParameter(String key, double fallback) {
            return numericParameters.getOrDefault(key, fallback);
        }

        public boolean hasTag(String tag) {
            return tags.contains(tag);
        }
    }

    public enum AttributeLayer {
        ELEMENT,
        YIN_YANG,
        INTENT,
        CUSTOM
    }

    public record Attribute(ResourceLocation id,
                            String displayName,
                            AttributeLayer layer,
                            double magnitude,
                            Map<String, Double> scaling,
                            Set<String> tags) {

        public Attribute {
            scaling = Collections.unmodifiableMap(new LinkedHashMap<>(scaling));
            tags = Collections.unmodifiableSet(new LinkedHashSet<>(tags));
        }

        public double scalingOrDefault(String key, double fallback) {
            return scaling.getOrDefault(key, fallback);
        }
    }

    public record Effect(ResourceLocation id,
                         String displayName,
                         Map<String, Double> payload,
                         Set<String> tags) {

        public Effect {
            payload = Collections.unmodifiableMap(new LinkedHashMap<>(payload));
            tags = Collections.unmodifiableSet(new LinkedHashSet<>(tags));
        }

        public double payloadOrDefault(String key, double fallback) {
            return payload.getOrDefault(key, fallback);
        }
    }

    public static final class Numbers {
        private final double baseDamage;
        private final double projectileSpeed;
        private final double areaRange;
        private final int channelTicks;
        private final int durationTicks;
        private final double cooldownSeconds;
        private final double spiritCost;

        public Numbers(double baseDamage,
                       double projectileSpeed,
                       double areaRange,
                       int channelTicks,
                       int durationTicks,
                       double cooldownSeconds,
                       double spiritCost) {
            this.baseDamage = baseDamage;
            this.projectileSpeed = projectileSpeed;
            this.areaRange = areaRange;
            this.channelTicks = channelTicks;
            this.durationTicks = durationTicks;
            this.cooldownSeconds = cooldownSeconds;
            this.spiritCost = spiritCost;
        }

        public double baseDamage() {
            return baseDamage;
        }

        public double projectileSpeed() {
            return projectileSpeed;
        }

        public double areaRange() {
            return areaRange;
        }

        public int channelTicks() {
            return channelTicks;
        }

        public int durationTicks() {
            return durationTicks;
        }

        public double cooldownSeconds() {
            return cooldownSeconds;
        }

        public double spiritCost() {
            return spiritCost;
        }
    }

    public static final class Metadata {
        private final String displayName;
        private final String description;
        private final CultivationRealm requiredRealm;
        private final int requiredStage;
        private final String rarity;
        private final List<String> unlockTags;

        public Metadata(String displayName,
                        String description,
                        @Nullable CultivationRealm requiredRealm,
                        int requiredStage,
                        String rarity,
                        List<String> unlockTags) {
            this.displayName = displayName;
            this.description = description;
            this.requiredRealm = requiredRealm;
            this.requiredStage = requiredStage;
            this.rarity = rarity;
            this.unlockTags = Collections.unmodifiableList(new ArrayList<>(unlockTags));
        }

        public String displayName() {
            return displayName;
        }

        public String description() {
            return description;
        }

        public CultivationRealm requiredRealm() {
            return requiredRealm;
        }

        public int requiredStage() {
            return requiredStage;
        }

        public String rarity() {
            return rarity;
        }

        public List<String> unlockTags() {
            return unlockTags;
        }
    }

    public static final class SwordQiOverride {
        private final double damageMultiplier;
        private final double speedMultiplier;
        private final double rangeMultiplier;
        private final List<Effect> extraEffects;
        private final Optional<String> requiredAttributeTag;

        public SwordQiOverride(double damageMultiplier,
                               double speedMultiplier,
                               double rangeMultiplier,
                               List<Effect> extraEffects,
                               @Nullable String requiredAttributeTag) {
            this.damageMultiplier = damageMultiplier;
            this.speedMultiplier = speedMultiplier;
            this.rangeMultiplier = rangeMultiplier;
            this.extraEffects = Collections.unmodifiableList(new ArrayList<>(extraEffects));
            this.requiredAttributeTag = Optional.ofNullable(requiredAttributeTag);
        }

        public double damageMultiplier() {
            return damageMultiplier;
        }

        public double speedMultiplier() {
            return speedMultiplier;
        }

        public double rangeMultiplier() {
            return rangeMultiplier;
        }

        public List<Effect> extraEffects() {
            return extraEffects;
        }

        public Optional<String> requiredAttributeTag() {
            return requiredAttributeTag;
        }
    }
}
