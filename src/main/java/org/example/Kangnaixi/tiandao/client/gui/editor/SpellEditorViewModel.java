package org.example.Kangnaixi.tiandao.client.gui.editor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import org.example.Kangnaixi.tiandao.Tiandao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 修仙术法编辑器 ViewModel
 * 基于 HTML 原型的完整实现，支持三段式骨架 + 属性 + 效果的术法构建系统
 */
public class SpellEditorViewModel {

    // 基本信息
    private String spellId = Tiandao.MODID + ":custom_spell";
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
    private double spiritCost = 25;

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

    // 施法源选项 (对应HTML中的source选项)
    private static final List<SpellComponent> SOURCE_OPTIONS = List.of(
        new SpellComponent("finger", "指诀", "单手结印，适合轻快小术，出手迅速。", "source"),
        new SpellComponent("seal", "法印", "双手结印，调动天地灵力，适合大术。", "source"),
        new SpellComponent("weapon", "法器（剑）", "借助飞剑 / 灵兵，将剑意外放为术。", "source"),
        new SpellComponent("talisman", "符箓", "以灵符为媒介施术，附加效果丰富。", "source"),
        new SpellComponent("array", "阵盘", "以阵盘催动阵法，布下术阵。", "source")
    );

    // 术法载体选项 (对应HTML中的carrier选项)
    private static final List<SpellComponent> CARRIER_OPTIONS = List.of(
        new SpellComponent("sword_qi", "剑气", "向前发出斩击波，对路径上的敌人造成伤害。", "carrier"),
        new SpellComponent("projectile", "灵光弹", "凝聚灵力为光弹，命中后爆散。", "carrier"),
        new SpellComponent("cone", "波动冲击", "向前方形成锥形冲击波。", "carrier"),
        new SpellComponent("field", "领域", "以自身为中心展开领域，持续影响范围内单位。", "carrier"),
        new SpellComponent("glyph", "地面术阵", "在地面刻下术阵，触发时爆发力量。", "carrier"),
        new SpellComponent("buff", "附体加持", "将术法加持于自身或队友身上。", "carrier")
    );

    // 生效方式选项 (对应HTML中的form选项)
    private static final List<SpellComponent> FORM_OPTIONS = List.of(
        new SpellComponent("instant", "瞬发", "无需蓄力，点击即发，适合小术与连招。", "form"),
        new SpellComponent("channel", "引导", "按住引导，蓄力越久威力越强。", "form"),
        new SpellComponent("delay", "延迟触发", "延迟一段时间后才会爆发，适合预判。", "form"),
        new SpellComponent("duration", "持续通道", "持续一段时间，不断生效。", "form"),
        new SpellComponent("combo", "连段", "多次按键触发不同段数，形成连招。", "form")
    );

    // 属性选项 (对应HTML中的attr选项)
    private static final List<SpellAttribute> ATTRIBUTE_OPTIONS = List.of(
        // 五行属性
        new SpellAttribute("metal", "金", "金性之气，主锐利与穿透。", "element"),
        new SpellAttribute("wood", "木", "木性之气，主生长与恢复。", "element"),
        new SpellAttribute("water", "水", "水性之气，主流动与寒意。", "element"),
        new SpellAttribute("fire", "火", "火性之气，主焚烧与爆裂。", "element"),
        new SpellAttribute("earth", "土", "土性之气，主沉稳与防御。", "element"),
        // 阴阳属性
        new SpellAttribute("yang", "阳", "阳刚之力，爆发、攻击偏强。", "yin_yang"),
        new SpellAttribute("yin", "阴", "阴柔之力，控制、削弱偏强。", "yin_yang"),
        // 意境属性
        new SpellAttribute("thunder", "雷意", "雷霆之意，迅疾而暴烈。", "intent"),
        new SpellAttribute("sword", "剑意", "剑之本意，攻伐无双。", "intent"),
        new SpellAttribute("wind", "风意", "疾风之意，轻灵迅捷。", "intent"),
        new SpellAttribute("wood_spirit", "木灵", "木灵之性，善治愈与滋养。", "intent")
    );

    // 效果选项 (对应HTML中的effect选项)
    private static final List<SpellEffect> EFFECT_OPTIONS = List.of(
        // 控制类/输出类
        new SpellEffect("armor_break", "破甲", "降低敌人防御，适合配合高攻剑气。"),
        new SpellEffect("knockback", "击退", "将敌人推开，保证安全距离。"),
        new SpellEffect("aoe_up", "范围扩大", "小幅增加术法影响范围。"),
        new SpellEffect("dot", "持续伤害", "在一段时间内持续造成伤害。"),
        // 回复与辅助
        new SpellEffect("heal_up", "治疗强化", "提升治疗术法的回复量。"),
        new SpellEffect("shield", "护盾", "为目标提供额外护盾值。"),
        new SpellEffect("move_speed", "移速提升", "一定时间内移动速度提高。"),
        new SpellEffect("lifesteal", "吸血", "造成伤害的一部分会转化为自身生命。")
    );

    // 基本设置方法

    public void setSpellId(String id) {
        if (!id.isBlank()) {
            this.spellId = id.toLowerCase(Locale.ROOT);
        }
    }

    public String getSpellId() {
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

    public void setSpiritCost(double spiritCost) {
        this.spiritCost = Math.max(0, spiritCost);
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

    public double getSpiritCost() {
        return spiritCost;
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
        spellId = Tiandao.MODID + ":custom_spell";
        displayName = "未命名术法";
        description = "请在此填写术法描述。";
        source = null;
        carrier = null;
        form = null;
        baseDamage = 10;
        speed = 1.0;
        range = 10;
        cooldown = 6;
        spiritCost = 25;
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
        stats.addProperty("spirit_cost", spiritCost);
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
