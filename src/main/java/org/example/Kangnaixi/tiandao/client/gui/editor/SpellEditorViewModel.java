package org.example.Kangnaixi.tiandao.client.gui.editor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.spell.definition.SpellDefinition;
import org.example.Kangnaixi.tiandao.spell.runtime.AttributeType;
import org.example.Kangnaixi.tiandao.spell.runtime.CarrierType;
import org.example.Kangnaixi.tiandao.spell.runtime.EffectType;
import org.example.Kangnaixi.tiandao.spell.runtime.FormType;
import org.example.Kangnaixi.tiandao.spell.runtime.SourceType;
import org.example.Kangnaixi.tiandao.spell.runtime.Spell;
import org.example.Kangnaixi.tiandao.spell.runtime.calc.SpellSpiritCostCalculator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 修仙术法编辑器 ViewModel
 * 基于 HTML 原型的完整实现，支持三段式骨架 + 属性 + 效果的术法构建系统
 */
public class SpellEditorViewModel {

    // ID 验证正则：允许 namespace:path/to/spell，path只能包含小写字母、数字、下划线、斜杠
    private static final Pattern VALID_ID_PATTERN = Pattern.compile("^[a-z0-9_]+:[a-z0-9_/]+$");

    // 基本信息
    private String spellId = null; // 默认为null，保存时如果未设置则自动生成
    private String displayName = "未命名术法";
    private String description = "请在此填写术法描述。";

    // 三段式骨架
    private SpellComponent source = null;
    private SpellComponent carrier = null;
    private SpellComponent form = null;

    // 基础属性
    private double baseDamage = 10;
    private double speed = 1.0;
    private double range = 10;
    private double cooldown = 6;

    // 时间相关
    private int channelTicks = 0;
    private int durationTicks = 0;

    // 属性系统 (五行/阴阳/意境)
    private final List<SpellAttribute> attributes = new ArrayList<>();

    // 效果系统
    private final List<SpellEffect> effects = new ArrayList<>();

    // 剑修强化相关 (动态计算，不需要手动设置)
    private boolean swordQiEnhanced = false;
    private double calculatedSwordDamageMultiplier = 1.0;
    private double calculatedSwordSpeedMultiplier = 1.0;
    private double calculatedSwordRangeMultiplier = 1.0;

    // ① 起手式选项（对应 SourceExecutors）
    private static final List<SpellComponent> SOURCE_OPTIONS = List.of(
        new SpellComponent("finger", "指诀", "从手前0.5m发出，快速灵活。", "source"),
        new SpellComponent("seal", "法印", "从身体中心发出，稳定持久。", "source"),
        new SpellComponent("weapon", "法器", "从武器端点发出，威力强大。", "source"),
        new SpellComponent("talisman", "符箓", "在落点生成，范围控制。", "source"),
        new SpellComponent("array", "阵盘", "从地面阵法触发，覆盖广阔。", "source")
    );

    // ② 载体选项（对应 CarrierExecutors）
    private static final List<SpellComponent> CARRIER_OPTIONS = List.of(
        new SpellComponent("sword_qi", "剑气", "线性斩击，快速贯穿。", "carrier"),
        new SpellComponent("projectile", "弹丸", "飞行投射物，命中爆散。", "carrier"),
        new SpellComponent("shockwave", "冲击波", "近战范围攻击，瞬间爆发。", "carrier"),
        new SpellComponent("field", "领域", "持续范围Aura，覆盖区域。", "carrier"),
        new SpellComponent("ground_spike", "地刺", "由地面冒起，突刺敌人。", "carrier"),
        new SpellComponent("buff", "护体", "挂在玩家身上的Buff形态。", "carrier")
    );

    // ③ 术式选项（对应 FormExecutors）
    private static final List<SpellComponent> FORM_OPTIONS = List.of(
        new SpellComponent("instant", "瞬发", "无需蓄力，点击即发。", "form"),
        new SpellComponent("channel", "引导", "持续引导，蓄力增强。", "form"),
        new SpellComponent("delay", "延迟触发", "延迟爆发，适合预判。", "form"),
        new SpellComponent("duration", "持续通道", "持续一段时间生效。", "form"),
        new SpellComponent("combo", "连段/多段", "多段连击，形成连招。", "form"),
        new SpellComponent("mark_detonate", "标记引爆", "标记后手动引爆。", "form")
    );

    // ④ 属性选项（对应 AttributeExecutors - 只保留五行）
    private static final List<SpellAttribute> ATTRIBUTE_OPTIONS = List.of(
        new SpellAttribute("fire", "火", "焚烧爆裂", "element"),
        new SpellAttribute("water", "水", "流动寒冰", "element"),
        new SpellAttribute("wood", "木", "生长恢复", "element"),
        new SpellAttribute("metal", "金", "锐利穿透", "element"),
        new SpellAttribute("earth", "土", "沉稳防御", "element")
    );

    // ⑤ 效果选项（对应 EffectExecutors）
    private static final List<SpellEffect> EFFECT_OPTIONS = List.of(
        new SpellEffect("armor_break", "破甲", "降低防御"),
        new SpellEffect("slow", "减速", "降低移速"),
        new SpellEffect("dot", "点燃", "持续伤害"),
        new SpellEffect("explode", "爆裂", "范围爆炸"),
        new SpellEffect("penetrate", "穿透", "无视护甲"),
        new SpellEffect("spread", "扩散", "范围扩散"),
        new SpellEffect("lifesteal", "吸血", "吸取生命"),
        new SpellEffect("track", "追踪", "自动追踪"),
        new SpellEffect("shield", "护盾", "额外护盾"),
        new SpellEffect("boomerang", "回旋", "飞回施法者")
    );

    // ==================== ID 生成与验证 ====================

    /**
     * 生成基于UUID的唯一术法ID
     * 格式: tiandao:custom/spell_<uuid前8位>
     */
    public static String generateUniqueId() {
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return Tiandao.MODID + ":custom/spell_" + uuid;
    }

    /**
     * 生成基于玩家名和时间戳的术法ID
     * 格式: tiandao:custom/<player_name>_<timestamp>
     */
    public static String generatePlayerBasedId(String playerName) {
        String sanitizedName = playerName.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9_]", "_");
        long timestamp = System.currentTimeMillis() / 1000; // 秒级时间戳
        return Tiandao.MODID + ":custom/" + sanitizedName + "_" + timestamp;
    }

    /**
     * 验证术法ID是否合法
     * @param id 术法ID
     * @return ValidationResult 包含是否合法和错误信息
     */
    public static ValidationResult validateSpellId(String id) {
        if (id == null || id.isBlank()) {
            return ValidationResult.invalid("术法ID不能为空");
        }

        String trimmed = id.trim().toLowerCase(Locale.ROOT);

        // 检查格式
        if (!VALID_ID_PATTERN.matcher(trimmed).matches()) {
            return ValidationResult.invalid("ID格式不正确。正确格式: namespace:path (只能包含小写字母、数字、下划线、斜杠)");
        }

        // 检查长度
        if (trimmed.length() > 128) {
            return ValidationResult.invalid("ID长度不能超过128字符");
        }

        // 检查namespace
        String[] parts = trimmed.split(":", 2);
        if (parts.length != 2) {
            return ValidationResult.invalid("ID必须包含namespace和path，格式: namespace:path");
        }

        String namespace = parts[0];
        String path = parts[1];

        if (namespace.isEmpty() || path.isEmpty()) {
            return ValidationResult.invalid("namespace和path不能为空");
        }

        return ValidationResult.valid(trimmed);
    }

    /**
     * ID验证结果
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String normalizedId; // 标准化后的ID（小写、去空格）
        private final String errorMessage;

        private ValidationResult(boolean valid, String normalizedId, String errorMessage) {
            this.valid = valid;
            this.normalizedId = normalizedId;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult valid(String normalizedId) {
            return new ValidationResult(true, normalizedId, null);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, null, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public String getNormalizedId() {
            return normalizedId;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    // ==================== 基本设置方法 ====================

    public void setSpellId(String id) {
        ValidationResult result = validateSpellId(id);
        if (result.isValid()) {
            this.spellId = result.getNormalizedId();
        } else {
            throw new IllegalArgumentException(result.getErrorMessage());
        }
    }

    /**
     * 获取术法ID，如果未设置则自动生成唯一ID
     */
    public String getSpellId() {
        if (spellId == null || spellId.isBlank()) {
            spellId = generateUniqueId();
        }
        return spellId;
    }

    /**
     * 获取原始ID（可能为null）
     */
    public String getSpellIdRaw() {
        return spellId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        if (!displayName.isBlank()) {
            this.displayName = displayName;
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // 骨架设置方法
    public void setSource(String id) {
        this.source = SOURCE_OPTIONS.stream()
            .filter(s -> s.id.equals(id))
            .findFirst()
            .orElse(null);
        updateSwordQiEnhancement();
    }

    public void setCarrier(String id) {
        this.carrier = CARRIER_OPTIONS.stream()
            .filter(c -> c.id.equals(id))
            .findFirst()
            .orElse(null);
        updateSwordQiEnhancement();
    }

    public void setForm(String id) {
        this.form = FORM_OPTIONS.stream()
            .filter(f -> f.id.equals(id))
            .findFirst()
            .orElse(null);
    }

    // 获取选项列表
    public static List<SpellComponent> getSourceOptions() {
        return SOURCE_OPTIONS;
    }

    public static List<SpellComponent> getCarrierOptions() {
        return CARRIER_OPTIONS;
    }

    public static List<SpellComponent> getFormOptions() {
        return FORM_OPTIONS;
    }

    public static List<SpellAttribute> getAttributeOptions() {
        return ATTRIBUTE_OPTIONS;
    }

    public static List<SpellEffect> getEffectOptions() {
        return EFFECT_OPTIONS;
    }

    // 属性和效果管理
    public void addAttribute(String id) {
        if (attributes.size() >= 3) {
            return; // 最多3个属性
        }
        SpellAttribute attr = ATTRIBUTE_OPTIONS.stream()
            .filter(a -> a.id.equals(id))
            .findFirst()
            .orElse(null);
        if (attr != null && !attributes.contains(attr)) {
            attributes.add(attr);
            updateSwordQiEnhancement();
        }
    }

    public void removeAttribute(String id) {
        attributes.removeIf(attr -> attr.id.equals(id));
        updateSwordQiEnhancement();
    }

    public void addEffect(String id) {
        if (effects.size() >= 4) {
            return; // 最多4个效果
        }
        SpellEffect effect = EFFECT_OPTIONS.stream()
            .filter(e -> e.id.equals(id))
            .findFirst()
            .orElse(null);
        if (effect != null && !effects.contains(effect)) {
            effects.add(effect);
        }
    }

    public void removeEffect(String id) {
        effects.removeIf(effect -> effect.id.equals(id));
    }

    /**
     * 更新剑修强化状态
     * 根据提示词要求：载体是"剑气"时，如果有"剑意"属性，会触发强化
     */
    private void updateSwordQiEnhancement() {
        // 检查载体是否为剑气
        boolean isSwordQiCarrier = carrier != null && carrier.id.equals("sword_qi");

        // 检查属性中是否包含剑意
        boolean hasSwordIntent = attributes.stream().anyMatch(attr -> attr.id.equals("sword"));

        // 剑修强化条件：剑气载体 AND 剑意属性
        // 注意：持剑判定在施法时进行，这里只是标记潜在的强化能力
        this.swordQiEnhanced = isSwordQiCarrier && hasSwordIntent;

        if (this.swordQiEnhanced) {
            // 可配置的强化系数
            this.calculatedSwordDamageMultiplier = 1.3;  // 伤害提升30%
            this.calculatedSwordSpeedMultiplier = 1.2;   // 速度提升20%
            this.calculatedSwordRangeMultiplier = 1.1;   // 范围提升10%
        } else {
            this.calculatedSwordDamageMultiplier = 1.0;
            this.calculatedSwordSpeedMultiplier = 1.0;
            this.calculatedSwordRangeMultiplier = 1.0;
        }
    }

    // 数值设置方法
    public void setBaseDamage(double damage) {
        this.baseDamage = Math.max(0, damage);
    }

    public void setSpeed(double speed) {
        this.speed = Math.max(0.1, speed);
    }

    public void setRange(double range) {
        this.range = Math.max(1, range);
    }

    public void setCooldown(double cooldown) {
        this.cooldown = Math.max(0, cooldown);
    }

    public void setChannelTicks(int channelTicks) {
        this.channelTicks = Math.max(0, channelTicks);
    }

    public void setDurationTicks(int durationTicks) {
        this.durationTicks = Math.max(0, durationTicks);
    }


    public SpellComponent getSource() {
        return source;
    }

    public SpellComponent getCarrier() {
        return carrier;
    }

    public SpellComponent getForm() {
        return form;
    }

    public double getBaseDamage() {
        return baseDamage;
    }

    public double getSpeed() {
        return speed;
    }

    public double getRange() {
        return range;
    }

    public double getCooldown() {
        return cooldown;
    }

    public int getChannelTicks() {
        return channelTicks;
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    public List<SpellAttribute> getAttributes() {
        return new ArrayList<>(attributes); // 返回副本
    }

    public List<SpellEffect> getEffects() {
        return new ArrayList<>(effects); // 返回副本
    }

    public boolean isSwordQiEnhanced() {
        return swordQiEnhanced;
    }

    public double getCalculatedSwordDamageMultiplier() {
        return calculatedSwordDamageMultiplier;
    }

    public double getCalculatedSwordSpeedMultiplier() {
        return calculatedSwordSpeedMultiplier;
    }

    public double getCalculatedSwordRangeMultiplier() {
        return calculatedSwordRangeMultiplier;
    }

    /**
     * 重置所有状态
     */
    public void reset() {
        spellId = null; // 重置为null，下次保存时自动生成新ID
        displayName = "未命名术法";
        description = "请在此填写术法描述。";
        source = null;
        carrier = null;
        form = null;
        baseDamage = 10;
        speed = 1.0;
        range = 10;
        cooldown = 6;
        channelTicks = 0;
        durationTicks = 0;
        attributes.clear();
        effects.clear();
        updateSwordQiEnhancement();
    }

    /**
     * 验证术法是否完整
     */
    public boolean isValid() {
        return source != null && carrier != null && form != null;
    }

    /**
     * 获取术法描述文本(用于预览)
     */
    public String getPreviewText() {
        if (!isValid()) {
            return "术法骨架尚未完整,请完成至少三项选择。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("以「").append(source.label).append("」为施法源，");
        sb.append("驱动「").append(carrier.label).append("」，");
        sb.append("以「").append(form.label).append("」的方式释放。");

        if (!attributes.isEmpty()) {
            sb.append(" 术法蕴含 ");
            sb.append(attributes.stream()
                .map(attr -> attr.label)
                .reduce((a, b) -> a + "、" + b)
                .orElse(""));
            sb.append(" 之道性。");
        }

        if (!effects.isEmpty()) {
            sb.append(" 附加效果包括：");
            sb.append(effects.stream()
                .map(effect -> effect.label)
                .reduce((a, b) -> a + "、" + b)
                .orElse(""));
            sb.append("。");
        }

        if (swordQiEnhanced) {
            sb.append(" §e[剑修强化：持剑时伤害×")
                .append(String.format("%.1f", calculatedSwordDamageMultiplier))
                .append("]§r");
        }

        return sb.toString();
    }

    /**
     * 序列化为JSON (与HTML原型的JSON结构兼容)
     */
    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("id", spellId);

        // 基础元数据
        JsonObject metadata = new JsonObject();
        metadata.addProperty("name", displayName);
        metadata.addProperty("description", description);
        metadata.addProperty("required_realm", "FOUNDATION");
        metadata.addProperty("required_stage", 1);
        metadata.addProperty("rarity", "COMMON");
        root.add("metadata", metadata);

        // 骨架结构
        if (source != null) {
            root.add("source", componentToJson(source));
        }
        if (carrier != null) {
            root.add("carrier", componentToJson(carrier));
        }
        if (form != null) {
            root.add("form", componentToJson(form));
        }

        // 属性
        JsonArray attrs = new JsonArray();
        for (SpellAttribute attr : attributes) {
            JsonObject attrJson = new JsonObject();
            attrJson.addProperty("id", attr.id);
            attrJson.addProperty("display_name", attr.label);
            attrJson.addProperty("layer", attr.type.toUpperCase());
            attrJson.addProperty("magnitude", 1.0);
            // 缩放系数
            JsonObject scaling = new JsonObject();
            scaling.addProperty("damage", 0.05);
            attrJson.add("scaling", scaling);
            attrs.add(attrJson);
        }
        root.add("attributes", attrs);

        // 效果
        JsonArray effectsArray = new JsonArray();
        for (SpellEffect effect : effects) {
            JsonObject effectJson = new JsonObject();
            effectJson.addProperty("id", effect.id);
            effectJson.addProperty("display_name", effect.label);
            JsonObject payload = new JsonObject();
            payload.addProperty("power", 1.0);
            effectJson.add("payload", payload);
            effectsArray.add(effectJson);
        }
        root.add("effects", effectsArray);

        // 基础属性
        JsonObject stats = new JsonObject();
        stats.addProperty("damage", baseDamage);
        stats.addProperty("speed", speed);
        stats.addProperty("range", range);
        stats.addProperty("channel_ticks", channelTicks);
        stats.addProperty("duration_ticks", durationTicks);
        stats.addProperty("cooldown", cooldown);
        root.add("base_stats", stats);

        // 剑修强化 (如果有)
        if (swordQiEnhanced) {
            JsonObject swordQi = new JsonObject();
            swordQi.addProperty("damage_multiplier", calculatedSwordDamageMultiplier);
            swordQi.addProperty("speed_multiplier", calculatedSwordSpeedMultiplier);
            swordQi.addProperty("range_multiplier", calculatedSwordRangeMultiplier);
            swordQi.addProperty("requires_sword", true);
            swordQi.addProperty("requires_sword_intent", true);
            root.add("sword_qi_enhancement", swordQi);
        }

        return root;
    }

    public double getComputedSpiritCost() {
        try {
            SpellDefinition definition = toRuntimeDefinition();
            return SpellSpiritCostCalculator.compute(definition);
        } catch (Exception ex) {
            return 10.0;
        }
    }

    public SpellDefinition toRuntimeDefinition() {
        ResourceLocation id = ResourceLocation.tryParse(normalizeSpellId(spellId));
        if (id == null) {
            id = ResourceLocation.fromNamespaceAndPath(Tiandao.MODID, "spell/generated");
        }

        SpellDefinition.Component sourceComp = toDefinitionComponent(source, "source");
        SpellDefinition.Component carrierComp = toDefinitionComponent(carrier, "carrier");
        SpellDefinition.Component formComp = toDefinitionComponent(form, "form");

        List<SpellDefinition.Attribute> attrs = new ArrayList<>();
        for (SpellAttribute attr : attributes) {
            SpellDefinition.Attribute definitionAttr = toDefinitionAttribute(attr);
            if (definitionAttr != null) {
                attrs.add(definitionAttr);
            }
        }

        List<SpellDefinition.Effect> effectDefs = new ArrayList<>();
        for (SpellEffect effect : effects) {
            SpellDefinition.Effect def = toDefinitionEffect(effect);
            if (def != null) {
                effectDefs.add(def);
            }
        }

        SpellDefinition.Numbers numbers = new SpellDefinition.Numbers(
            baseDamage,
            speed,
            range,
            channelTicks,
            durationTicks,
            cooldown,
            0
        );

        SpellDefinition.Metadata metadata = new SpellDefinition.Metadata(
            displayName == null ? "" : displayName,
            description == null ? "" : description,
            null,
            0,
            "COMMON",
            List.of()
        );

        return new SpellDefinition(id, sourceComp, carrierComp, formComp, attrs, effectDefs, numbers, metadata, null);
    }

    private SpellDefinition.Component toDefinitionComponent(SpellComponent component, String category) {
        // 使用真实的 resource ID (例如 tiandao:source/finger)，而不是临时的 editor/ 前缀
        ResourceLocation compId = component != null
            ? ResourceLocation.fromNamespaceAndPath(Tiandao.MODID, category + "/" + component.id)
            : ResourceLocation.fromNamespaceAndPath(Tiandao.MODID, category + "/missing");
        String name = component != null ? component.label : "未选择";
        String desc = component != null ? component.description : "";
        Map<String, Double> params = new LinkedHashMap<>();
        return new SpellDefinition.Component(compId, name, desc, params, Collections.emptySet());
    }

    private SpellDefinition.Attribute toDefinitionAttribute(SpellAttribute attribute) {
        if (attribute == null) {
            return null;
        }
        // 使用真实的 attribute resource ID (例如 tiandao:attr/fire)
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Tiandao.MODID, "attr/" + attribute.id);
        SpellDefinition.AttributeLayer layer = switch (attribute.type.toLowerCase(Locale.ROOT)) {
            case "element" -> SpellDefinition.AttributeLayer.ELEMENT;
            case "yin_yang" -> SpellDefinition.AttributeLayer.YIN_YANG;
            case "intent" -> SpellDefinition.AttributeLayer.INTENT;
            default -> SpellDefinition.AttributeLayer.CUSTOM;
        };
        return new SpellDefinition.Attribute(id, attribute.label, layer, 1.0,
            Collections.emptyMap(), Collections.emptySet());
    }

    private SpellDefinition.Effect toDefinitionEffect(SpellEffect effect) {
        if (effect == null) {
            return null;
        }
        // 使用真实的 effect resource ID (例如 tiandao:effect/explode)
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Tiandao.MODID, "effect/" + effect.id);
        return new SpellDefinition.Effect(id, effect.label, Collections.emptyMap(), Collections.emptySet());
    }

    /**
     * 将当前 GUI 配置转换为服务器可执行的 Spell 对象.
     * 若缺少关键信息将抛出 IllegalStateException 以便界面提示玩家补全。
     */
    public Spell toRuntimeSpell() {
        if (source == null || carrier == null || form == null) {
            throw new IllegalStateException("请先选择施法源/载体/生效方式");
        }
        String normalizedId = normalizeSpellId(spellId);
        SourceType sourceType = mapSource(source.id);
        CarrierType carrierType = mapCarrier(carrier.id);
        FormType formType = mapForm(form.id);

        List<AttributeType> attrTypes = attributes.stream()
            .map(attr -> mapAttribute(attr.id))
            .collect(Collectors.toList());
        List<EffectType> effectTypes = effects.stream()
            .map(effect -> mapEffect(effect.id))
            .collect(Collectors.toList());

        return new Spell(
            normalizedId,
            displayName == null ? "" : displayName,
            sourceType,
            carrierType,
            formType,
            attrTypes,
            effectTypes,
            baseDamage,
            getComputedSpiritCost(),
            cooldown,
            range
        );
    }

    private static String normalizeSpellId(String rawId) {
        String id = (rawId == null || rawId.isBlank()) ? Tiandao.MODID + ":custom_spell" : rawId.trim().toLowerCase(Locale.ROOT);
        id = id.replace(' ', '_');
        if (!id.contains(":")) {
            id = Tiandao.MODID + ":" + id;
        }
        return id;
    }

    private static SourceType mapSource(String id) {
        return switch (id) {
            case "finger" -> SourceType.FINGER;
            case "seal" -> SourceType.SEAL;
            case "weapon" -> SourceType.WEAPON_SWORD;
            case "talisman" -> SourceType.TALISMAN;
            case "array" -> SourceType.ARRAY;
            default -> SourceType.RUNE_CORE;
        };
    }

    private static CarrierType mapCarrier(String id) {
        return switch (id) {
            case "sword_qi" -> CarrierType.SWORD_QI;
            case "projectile" -> CarrierType.PROJECTILE;
            case "shockwave" -> CarrierType.WAVE;      // 冲击波
            case "field" -> CarrierType.FIELD;
            case "ground_spike" -> CarrierType.GLYPH;  // 地刺
            case "buff" -> CarrierType.BUFF;
            // 向后兼容旧选项
            case "cone" -> CarrierType.WAVE;
            case "glyph" -> CarrierType.GLYPH;
            default -> CarrierType.PROJECTILE;
        };
    }

    private static FormType mapForm(String id) {
        return switch (id) {
            case "instant" -> FormType.INSTANT;
            case "channel" -> FormType.CHANNEL;
            case "delay" -> FormType.DELAYED;
            case "duration" -> FormType.DURATION;
            case "combo" -> FormType.COMBO;
            case "mark_detonate" -> FormType.MARK_DETONATE;  // 标记引爆
            default -> FormType.INSTANT;
        };
    }

    private static AttributeType mapAttribute(String id) {
        try {
            return AttributeType.valueOf(id.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("未知属性: " + id);
        }
    }

    private static EffectType mapEffect(String id) {
        try {
            return EffectType.valueOf(id.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("未知效果: " + id);
        }
    }

    private JsonObject componentToJson(SpellComponent component) {
        JsonObject json = new JsonObject();
        json.addProperty("id", component.id);
        json.addProperty("display_name", component.label);
        json.addProperty("description", component.description);
        return json;
    }

    // 内部数据类
    public static class SpellComponent {
        public final String id;
        public final String label;
        public final String description;
        public final String type;

        public SpellComponent(String id, String label, String description, String type) {
            this.id = id;
            this.label = label;
            this.description = description;
            this.type = type;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            SpellComponent that = (SpellComponent) obj;
            return id.equals(that.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }
    }

    public static class SpellAttribute {
        public final String id;
        public final String label;
        public final String description;
        public final String type;

        public SpellAttribute(String id, String label, String description, String type) {
            this.id = id;
            this.label = label;
            this.description = description;
            this.type = type;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            SpellAttribute that = (SpellAttribute) obj;
            return id.equals(that.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }
    }

    public static class SpellEffect {
        public final String id;
        public final String label;
        public final String description;

        public SpellEffect(String id, String label, String description) {
            this.id = id;
            this.label = label;
            this.description = description;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            SpellEffect that = (SpellEffect) obj;
            return id.equals(that.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }
    }
}
