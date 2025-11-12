package org.example.Kangnaixi.tiandao.spell.builder;

import org.example.Kangnaixi.tiandao.Tiandao;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 内存中的 Form/Effect/Augment/Template 映射，供 GUI / 校验逻辑使用。
 */
public final class SpellComponentLibrary {

    private static final SpellComponentConfigLoader LOADER = new SpellComponentConfigLoader();

    private static final Map<String, FormDefinition> FORMS = new LinkedHashMap<>();
    private static final Map<String, EffectDefinition> EFFECTS = new LinkedHashMap<>();
    private static final Map<String, AugmentDefinition> AUGMENTS = new LinkedHashMap<>();
    private static final Map<String, SpellTemplateDefinition> TEMPLATES = new LinkedHashMap<>();

    private static boolean initialized = false;

    private SpellComponentLibrary() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        reload();
        initialized = true;
    }

    public static synchronized void reload() {
        SpellComponentConfigLoader.LoadedComponents data = LOADER.loadAll();
        FORMS.clear();
        data.forms().forEach(form -> FORMS.put(form.getId(), form));

        EFFECTS.clear();
        data.effects().forEach(effect -> EFFECTS.put(effect.getId(), effect));

        AUGMENTS.clear();
        data.augments().forEach(augment -> AUGMENTS.put(augment.getId(), augment));

        TEMPLATES.clear();
        data.templates().forEach(template -> TEMPLATES.put(template.getId(), template));

        Tiandao.LOGGER.info("术法组件库载入完成：forms={}, effects={}, augments={}, templates={}",
            FORMS.size(), EFFECTS.size(), AUGMENTS.size(), TEMPLATES.size());
    }

    public static Collection<FormDefinition> getForms() {
        return Collections.unmodifiableCollection(FORMS.values());
    }

    public static Collection<EffectDefinition> getEffects() {
        return Collections.unmodifiableCollection(EFFECTS.values());
    }

    public static Collection<AugmentDefinition> getAugments() {
        return Collections.unmodifiableCollection(AUGMENTS.values());
    }

    public static Collection<SpellTemplateDefinition> getTemplates() {
        return Collections.unmodifiableCollection(TEMPLATES.values());
    }

    public static Optional<FormDefinition> findForm(String id) {
        return Optional.ofNullable(FORMS.get(id));
    }

    public static Optional<EffectDefinition> findEffect(String id) {
        return Optional.ofNullable(EFFECTS.get(id));
    }

    public static Optional<AugmentDefinition> findAugment(String id) {
        return Optional.ofNullable(AUGMENTS.get(id));
    }

    public static Optional<SpellTemplateDefinition> findTemplate(String id) {
        return Optional.ofNullable(TEMPLATES.get(id));
    }
}
