package org.example.Kangnaixi.tiandao.spell.blueprint;

import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.cultivation.CultivationRealm;

import java.util.*;

/**
 * 术法蓝图模板注册表
 */
public class SpellBlueprintLibrary {

    private static final Map<String, SpellBlueprint> BLUEPRINTS = new LinkedHashMap<>();
    private static boolean initialized = false;

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        registerDefaults();
    }

    private static void registerDefaults() {
        SpellBlueprintConfigLoader loader = new SpellBlueprintConfigLoader();
        List<SpellBlueprint> loaded = loader.loadBlueprints();
        if (loaded.isEmpty()) {
            Tiandao.LOGGER.warn("未能从配置目录加载术法蓝图，回退到内置模板");
            registerLegacyDefaults();
            return;
        }
        loaded.forEach(SpellBlueprintLibrary::register);
        Tiandao.LOGGER.info("已从配置加载 {} 个术法蓝图模板", loaded.size());
    }

    private static void registerLegacyDefaults() {
        register(new SpellBlueprint(
            "tiandao:fire_burst",
            "赤焰爆发",
            "向指向方向释放火焰冲击，命中后造成范围灼烧伤害。",
            SpellBlueprint.ElementType.FIRE,
            SpellBlueprint.EffectType.DAMAGE,
            SpellBlueprint.TargetingType.DIRECTIONAL_RELEASE,
            8.0,
            35.0,
            6.0,
            20.0,
            3.5,
            CultivationRealm.QI_CONDENSATION,
            2
        ));

        register(new SpellBlueprint(
            "tiandao:verdant_recovery",
            "青木回春",
            "以自身为中心释放木灵之力，回复附近同伴的生命值。",
            SpellBlueprint.ElementType.WOOD,
            SpellBlueprint.EffectType.HEALING,
            SpellBlueprint.TargetingType.AREA_RELEASE,
            5.0,
            28.0,
            8.0,
            0.0,
            5.0,
            CultivationRealm.QI_CONDENSATION,
            3
        ));

        register(new SpellBlueprint(
            "tiandao:stone_fortress",
            "厚土护墙",
            "在指定位置生成土灵壁障，吸收一定量的伤害。",
            SpellBlueprint.ElementType.EARTH,
            SpellBlueprint.EffectType.UTILITY,
            SpellBlueprint.TargetingType.TARGET_BLOCK,
            12.0,
            42.0,
            12.0,
            15.0,
            4.0,
            CultivationRealm.QI_CONDENSATION,
            4
        ));
    }

    public static void register(SpellBlueprint blueprint) {
        BLUEPRINTS.put(blueprint.getId(), blueprint);
    }

    public static SpellBlueprint get(String id) {
        return BLUEPRINTS.get(id);
    }

    public static Collection<SpellBlueprint> getAll() {
        return Collections.unmodifiableCollection(BLUEPRINTS.values());
    }

    public static Set<String> getIds() {
        return Collections.unmodifiableSet(BLUEPRINTS.keySet());
    }
}
