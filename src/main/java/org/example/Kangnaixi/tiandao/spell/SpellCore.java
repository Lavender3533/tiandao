package org.example.Kangnaixi.tiandao.spell;

import java.util.Objects;

/**
 * 术法骨架结构
 * 对应HTML原型中的三段式骨架：施法源、术法载体、生效方式
 */
public class SpellCore {

    private final String source;
    private final String carrier;
    private final String form;

    // 预定义的选项常量
    public static final class Sources {
        public static final String FINGER = "finger";
        public static final String SEAL = "seal";
        public static final String WEAPON = "weapon";
        public static final String TALISMAN = "talisman";
        public static final String ARRAY = "array";
    }

    public static final class Carriers {
        public static final String SWORD_QI = "sword_qi";
        public static final String PROJECTILE = "projectile";
        public static final String CONE = "cone";
        public static final String FIELD = "field";
        public static final String GLYPH = "glyph";
        public static final String BUFF = "buff";
    }

    public static final class Forms {
        public static final String INSTANT = "instant";
        public static final String CHANNEL = "channel";
        public static final String DELAY = "delay";
        public static final String DURATION = "duration";
        public static final String COMBO = "combo";
    }

    public SpellCore(String source, String carrier, String form) {
        this.source = Objects.requireNonNull(source, "Source cannot be null");
        this.carrier = Objects.requireNonNull(carrier, "Carrier cannot be null");
        this.form = Objects.requireNonNull(form, "Form cannot be null");
    }

    public String getSource() {
        return source;
    }

    public String getCarrier() {
        return carrier;
    }

    public String getForm() {
        return form;
    }

    /**
     * 获取载体的显示名称
     */
    public String getCarrierDisplayName() {
        switch (carrier) {
            case Carriers.SWORD_QI: return "剑气";
            case Carriers.PROJECTILE: return "灵光弹";
            case Carriers.CONE: return "波动冲击";
            case Carriers.FIELD: return "领域";
            case Carriers.GLYPH: return "地面术阵";
            case Carriers.BUFF: return "附体加持";
            default: return carrier;
        }
    }

    /**
     * 获取生效方式的显示名称
     */
    public String getFormDisplayName() {
        switch (form) {
            case Forms.INSTANT: return "瞬发";
            case Forms.CHANNEL: return "引导";
            case Forms.DELAY: return "延迟触发";
            case Forms.DURATION: return "持续通道";
            case Forms.COMBO: return "连段";
            default: return form;
        }
    }

    /**
     * 检查是否为剑气载体
     */
    public boolean isSwordQiCarrier() {
        return Carriers.SWORD_QI.equals(carrier);
    }

    /**
     * 检查是否需要引导
     */
    public boolean requiresChanneling() {
        return Forms.CHANNEL.equals(form) || Forms.DURATION.equals(form);
    }

    /**
     * 检查是否为瞬发
     */
    public boolean isInstant() {
        return Forms.INSTANT.equals(form);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SpellCore that = (SpellCore) obj;
        return source.equals(that.source) &&
               carrier.equals(that.carrier) &&
               form.equals(that.form);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, carrier, form);
    }

    @Override
    public String toString() {
        return String.format("SpellCore[source=%s, carrier=%s, form=%s]", source, carrier, form);
    }
}