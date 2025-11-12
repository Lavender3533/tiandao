package org.example.Kangnaixi.tiandao.spell.blueprint;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.minecraftforge.fml.loading.FMLPaths;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.cultivation.CultivationRealm;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * 负责从配置目录加载术法蓝图，并在缺失时写入默认示例
 */
public class SpellBlueprintConfigLoader {

    private static final Gson GSON = new GsonBuilder()
        .disableHtmlEscaping()
        .serializeNulls()
        .setPrettyPrinting()
        .create();

    private static final Map<String, String> DEFAULT_BLUEPRINTS = Map.of();

    private final Path configDir = FMLPaths.CONFIGDIR.get()
        .resolve("tiandao")
        .resolve("spells");

    public List<SpellBlueprint> loadBlueprints() {
        List<SpellBlueprint> blueprints = new ArrayList<>();
        try {
            Files.createDirectories(configDir);
            ensureDefaults();
            try (Stream<Path> stream = Files.list(configDir)) {
                stream.filter(path -> path.toString().endsWith(".json"))
                    .map(this::loadFile)
                    .flatMap(Optional::stream)
                    .forEach(blueprints::add);
            }
        } catch (IOException e) {
            Tiandao.LOGGER.error("加载术法蓝图配置目录失败", e);
        }
        return blueprints;
    }

    private Optional<SpellBlueprint> loadFile(Path path) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            ConfigEntry entry = GSON.fromJson(reader, ConfigEntry.class);
            if (entry == null || entry.id == null || entry.id.isBlank()) {
                Tiandao.LOGGER.warn("跳过无效术法蓝图文件: {}", path.getFileName());
                return Optional.empty();
            }
            SpellBlueprint.ElementType primary = entry.element != null
                ? entry.element
                : SpellBlueprint.ElementType.VOID;

            SpellBlueprint blueprint = new SpellBlueprint(
                entry.id,
                entry.name != null ? entry.name : entry.id,
                entry.description != null ? entry.description : "",
                primary,
                entry.effectType != null ? entry.effectType : SpellBlueprint.EffectType.UTILITY,
                entry.targeting != null ? entry.targeting : SpellBlueprint.TargetingType.SELF,
                entry.basePower != null ? entry.basePower : 0.0,
                entry.spiritCost != null ? entry.spiritCost : 0.0,
                entry.cooldownSeconds != null ? entry.cooldownSeconds : 0.0,
                entry.range != null ? entry.range : 0.0,
                entry.areaRadius != null ? entry.areaRadius : 0.0,
                entry.requiredRealm != null ? entry.requiredRealm : CultivationRealm.QI_CONDENSATION,
                entry.requiredSubRealmLevel != null ? entry.requiredSubRealmLevel : 0,
                entry.advanced
            );
            return Optional.of(blueprint);
        } catch (JsonSyntaxException ex) {
            Tiandao.LOGGER.error("解析术法蓝图文件 {} 失败: {}", path.getFileName(), ex.getMessage());
        } catch (IOException e) {
            Tiandao.LOGGER.error("读取术法蓝图文件 {} 失败", path.getFileName(), e);
        }
        return Optional.empty();
    }

    private void ensureDefaults() throws IOException {
        boolean hasJson;
        try (Stream<Path> stream = Files.list(configDir)) {
            hasJson = stream.anyMatch(path -> path.toString().endsWith(".json"));
        }
        if (hasJson) {
            return;
        }
        for (Map.Entry<String, String> entry : DEFAULT_BLUEPRINTS.entrySet()) {
            Path target = configDir.resolve(entry.getKey());
            Files.writeString(target, entry.getValue(), StandardCharsets.UTF_8);
        }
        Tiandao.LOGGER.info("首次运行：已写入 {} 个默认术法蓝图配置", DEFAULT_BLUEPRINTS.size());
    }

    private static class ConfigEntry {
        private String id;
        private String name;
        private String description;
        private SpellBlueprint.ElementType element;
        private SpellBlueprint.EffectType effectType;
        private SpellBlueprint.TargetingType targeting;
        private Double basePower;
        private Double spiritCost;
        private Double cooldownSeconds;
        private Double range;
        private Double areaRadius;
        private CultivationRealm requiredRealm;
        private Integer requiredSubRealmLevel;
        private SpellBlueprint.AdvancedData advanced;
    }
}
