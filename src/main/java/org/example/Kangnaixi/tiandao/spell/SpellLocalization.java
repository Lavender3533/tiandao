package org.example.Kangnaixi.tiandao.spell;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.example.Kangnaixi.tiandao.spell.blueprint.SpellBlueprint;

import java.util.Locale;

/**
 * 统一管理术法系统的本地化 key，避免在各处硬编码字符串。
 */
public final class SpellLocalization {

    private static final Locale ROOT = Locale.ROOT;

    private SpellLocalization() {
    }

    private static String enumKey(Enum<?> value) {
        return value == null ? "unknown" : value.name().toLowerCase(ROOT);
    }

    private static MutableComponent translate(String prefix, String key, Object... args) {
        return Component.translatable(prefix + key, args);
    }

    public static MutableComponent gui(String key, Object... args) {
        return translate("gui.tiandao.spell.", key, args);
    }

    public static MutableComponent message(String key, Object... args) {
        return translate("message.tiandao.spell.", key, args);
    }

    public static MutableComponent tooltip(String key, Object... args) {
        return translate("tooltip.tiandao.spell.", key, args);
    }

    public static MutableComponent element(SpellBlueprint.ElementType type) {
        return translate("tiandao.spell.element.", enumKey(type));
    }

    public static MutableComponent effect(SpellBlueprint.EffectType type) {
        return translate("tiandao.spell.effect.", enumKey(type));
    }

    public static MutableComponent targeting(SpellBlueprint.TargetingType type) {
        return translate("tiandao.spell.targeting.", enumKey(type));
    }

    public static MutableComponent shape(SpellBlueprint.ShapeType type) {
        return translate("tiandao.spell.shape.", enumKey(type));
    }

    public static String elementName(SpellBlueprint.ElementType type) {
        return element(type).getString();
    }

    public static String shapeName(SpellBlueprint.ShapeType type) {
        return shape(type).getString();
    }
}
