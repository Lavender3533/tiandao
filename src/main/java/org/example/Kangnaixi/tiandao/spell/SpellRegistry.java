package org.example.Kangnaixi.tiandao.spell;

import net.minecraft.resources.ResourceLocation;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.capability.ICultivation;
import org.example.Kangnaixi.tiandao.cultivation.SubRealm;
import org.example.Kangnaixi.tiandao.spell.definition.SpellDefinition;
import org.example.Kangnaixi.tiandao.spell.definition.SpellDefinitionLoader;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * SpellDefinition 注册表。负责从 JSON 载入并缓存术法定义。
 */
public class SpellRegistry {

    private static SpellRegistry instance;
    private final Map<ResourceLocation, SpellDefinition> spells = new LinkedHashMap<>();
    private final SpellDefinitionLoader loader = new SpellDefinitionLoader();

    private SpellRegistry() {
        reload();
    }

    public static SpellRegistry getInstance() {
        if (instance == null) {
            instance = new SpellRegistry();
        }
        return instance;
    }

    public void registerSpell(SpellDefinition spell) {
        ResourceLocation id = spell.getId();
        if (spells.containsKey(id)) {
            Tiandao.LOGGER.warn("术法 {} 已存在，将被覆盖", id);
        }
        spells.put(id, spell);
        Tiandao.LOGGER.debug("注册术法: {} ({})", spell.getMetadata().displayName(), id);
    }

    public SpellDefinition getSpell(ResourceLocation id) {
        return spells.get(id);
    }

    public SpellDefinition getSpellById(String id) {
        ResourceLocation identifier = ResourceLocation.tryParse(id);
        if (identifier == null) {
            return null;
        }
        return spells.get(identifier);
    }

    public Collection<SpellDefinition> getAllSpells() {
        return Collections.unmodifiableCollection(spells.values());
    }

    public Set<String> getAllSpellIds() {
        return spells.keySet().stream()
            .map(ResourceLocation::toString)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public List<SpellDefinition> getUnlockedSpells(ICultivation cultivation) {
        return spells.values().stream()
            .filter(spell -> {
                SpellDefinition.Metadata metadata = spell.getMetadata();
                if (metadata.requiredRealm() == null) {
                    return true;
                }
                if (cultivation.getRealm().ordinal() > metadata.requiredRealm().ordinal()) {
                    return true;
                }
                if (cultivation.getRealm() == metadata.requiredRealm()) {
                    return getSubRealmToLevel(cultivation.getSubRealm()) >= metadata.requiredStage();
                }
                return false;
            })
            .collect(Collectors.toList());
    }

    private int getSubRealmToLevel(SubRealm subRealm) {
        switch (subRealm) {
            case MIDDLE:
                return 4;
            case LATE:
                return 7;
            case EARLY:
            default:
                return 1;
        }
    }

    public boolean hasSpell(String id) {
        return getSpellById(id) != null;
    }

    public int getSpellCount() {
        return spells.size();
    }

    public void clear() {
        spells.clear();
        Tiandao.LOGGER.info("术法注册表已清空");
    }

    public void reload() {
        clear();
        loader.loadAll().forEach(this::registerSpell);
        Tiandao.LOGGER.info("加载 {} 条术法定义", spells.size());
    }
}
