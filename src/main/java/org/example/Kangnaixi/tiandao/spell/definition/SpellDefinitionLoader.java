package org.example.Kangnaixi.tiandao.spell.definition;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.cultivation.CultivationRealm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 负责从资源/配置目录加载 SpellDefinition。
 */
public class SpellDefinitionLoader {

    private static final Gson GSON = new GsonBuilder()
        .disableHtmlEscaping()
        .setLenient()
        .create();

    private static final String BUNDLED_RESOURCE = "/spells/definitions/sword_qi_examples.json";

    private final Path exportDir = FMLPaths.CONFIGDIR.get()
        .resolve("tiandao")
        .resolve("spell_exports");

    public List<SpellDefinition> loadAll() {
        List<SpellDefinition> result = new ArrayList<>();
        result.addAll(loadBundledDefinitions());
        result.addAll(loadFromConfigDir());
        return result;
    }

    private List<SpellDefinition> loadBundledDefinitions() {
        try (InputStream stream = SpellDefinitionLoader.class.getResourceAsStream(BUNDLED_RESOURCE)) {
            if (stream == null) {
                Tiandao.LOGGER.warn("缺少内置术法样例资源: {}", BUNDLED_RESOURCE);
                return Collections.emptyList();
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                JsonElement root = JsonParser.parseReader(reader);
                return parseRoot(root, "bundled");
            }
        } catch (IOException | JsonParseException ex) {
            Tiandao.LOGGER.error("解析内置术法样例失败", ex);
            return Collections.emptyList();
        }
    }

    private List<SpellDefinition> loadFromConfigDir() {
        List<SpellDefinition> results = new ArrayList<>();
        try {
            Files.createDirectories(exportDir);
            try (var streams = Files.list(exportDir)) {
                streams.filter(path -> path.toString().endsWith(".json"))
                    .forEach(path -> results.addAll(readDefinitionFile(path)));
            }
        } catch (IOException e) {
            Tiandao.LOGGER.error("读取 config 术法目录失败: {}", exportDir, e);
        }
        return results;
    }

    private List<SpellDefinition> readDefinitionFile(Path path) {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            return parseRoot(root, path.getFileName().toString());
        } catch (IOException | JsonParseException ex) {
            Tiandao.LOGGER.error("解析术法定义文件 {} 失败: {}", path, ex.getMessage());
            return Collections.emptyList();
        }
    }

    private List<SpellDefinition> parseRoot(JsonElement element, String source) {
        if (element == null || element.isJsonNull()) {
            return Collections.emptyList();
        }
        List<SpellDefinition> definitions = new ArrayList<>();
        if (element.isJsonArray()) {
            for (JsonElement entry : element.getAsJsonArray()) {
                parseDefinition(entry.getAsJsonObject(), source).ifPresent(definitions::add);
            }
        } else if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has("spells") && obj.get("spells").isJsonArray()) {
                for (JsonElement entry : obj.getAsJsonArray("spells")) {
                    parseDefinition(entry.getAsJsonObject(), source).ifPresent(definitions::add);
                }
            } else {
                parseDefinition(obj, source).ifPresent(definitions::add);
            }
        }
        return definitions;
    }

    public Optional<SpellDefinition> parseDefinition(JsonObject obj, String source) {
        try {
            ResourceLocation id = parseId(obj.get("id"), "spell id");
            SpellDefinition.Component sourceComp = parseComponent(obj.getAsJsonObject("source"));
            SpellDefinition.Component carrierComp = parseComponent(obj.getAsJsonObject("carrier"));
            SpellDefinition.Component formComp = parseComponent(obj.getAsJsonObject("form"));
            List<SpellDefinition.Attribute> attributes = parseAttributes(obj.getAsJsonArray("attributes"));
            List<SpellDefinition.Effect> effects = parseEffects(obj.getAsJsonArray("effects"));
            SpellDefinition.Numbers numbers = parseNumbers(obj.getAsJsonObject("base_stats"));
            SpellDefinition.Metadata metadata = parseMetadata(obj.getAsJsonObject("metadata"));
            SpellDefinition.SwordQiOverride swordQi = null;
            if (obj.has("sword_qi")) {
                swordQi = parseSwordQi(obj.getAsJsonObject("sword_qi"));
            }
            return Optional.of(new SpellDefinition(id, sourceComp, carrierComp, formComp,
                attributes, effects, numbers, metadata, swordQi));
        } catch (Exception ex) {
            Tiandao.LOGGER.error("解析术法 {} 中 {} 失败: {}", source, obj, ex.getMessage());
            return Optional.empty();
        }
    }

    private SpellDefinition.Component parseComponent(JsonObject object) {
        ResourceLocation id = parseId(object.get("id"), "component");
        String name = getOrDefault(object, "display_name", id.getPath());
        String description = getOrDefault(object, "description", "");
        Map<String, Double> params = new LinkedHashMap<>();
        if (object.has("parameters") && object.get("parameters").isJsonObject()) {
            object.getAsJsonObject("parameters").entrySet()
                .forEach(entry -> params.put(entry.getKey(), entry.getValue().getAsDouble()));
        }
        Set<String> tags = readTags(object);
        return new SpellDefinition.Component(id, name, description, params, tags);
    }

    private List<SpellDefinition.Attribute> parseAttributes(JsonArray array) {
        if (array == null) {
            return Collections.emptyList();
        }
        List<SpellDefinition.Attribute> attributes = new ArrayList<>();
        for (JsonElement element : array) {
            JsonObject obj = element.getAsJsonObject();
            ResourceLocation id = parseId(obj.get("id"), "attribute");
            String name = getOrDefault(obj, "display_name", id.getPath());
            SpellDefinition.AttributeLayer layer = SpellDefinition.AttributeLayer.valueOf(
                getOrDefault(obj, "layer", "CUSTOM").toUpperCase(Locale.ROOT));
            double magnitude = obj.has("magnitude") ? obj.get("magnitude").getAsDouble() : 1.0D;
            Map<String, Double> scaling = new LinkedHashMap<>();
            if (obj.has("scaling") && obj.get("scaling").isJsonObject()) {
                obj.getAsJsonObject("scaling").entrySet()
                    .forEach(entry -> scaling.put(entry.getKey(), entry.getValue().getAsDouble()));
            }
            Set<String> tags = readTags(obj);
            attributes.add(new SpellDefinition.Attribute(id, name, layer, magnitude, scaling, tags));
        }
        return attributes;
    }

    private List<SpellDefinition.Effect> parseEffects(JsonArray array) {
        if (array == null) {
            return Collections.emptyList();
        }
        List<SpellDefinition.Effect> effects = new ArrayList<>();
        for (JsonElement element : array) {
            JsonObject obj = element.getAsJsonObject();
            ResourceLocation id = parseId(obj.get("id"), "effect");
            String name = getOrDefault(obj, "display_name", id.getPath());
            Map<String, Double> payload = new LinkedHashMap<>();
            if (obj.has("payload") && obj.get("payload").isJsonObject()) {
                obj.getAsJsonObject("payload").entrySet()
                    .forEach(entry -> payload.put(entry.getKey(), entry.getValue().getAsDouble()));
            }
            Set<String> tags = readTags(obj);
            effects.add(new SpellDefinition.Effect(id, name, payload, tags));
        }
        return effects;
    }

    private SpellDefinition.Numbers parseNumbers(JsonObject object) {
        if (object == null) {
            throw new IllegalArgumentException("缺少 base_stats");
        }
        double damage = getDouble(object, "damage", 5.0D);
        double speed = getDouble(object, "speed", 1.0D);
        double range = getDouble(object, "range", 5.0D);
        int channel = (int) Math.round(getDouble(object, "channel_ticks", 0));
        int duration = (int) Math.round(getDouble(object, "duration_ticks", 0));
        double cooldown = getDouble(object, "cooldown", 5.0D);
        double spiritCost = getDouble(object, "spirit_cost", 10.0D);
        return new SpellDefinition.Numbers(damage, speed, range, channel, duration, cooldown, spiritCost);
    }

    private SpellDefinition.Metadata parseMetadata(JsonObject object) {
        if (object == null) {
            return new SpellDefinition.Metadata("Unnamed Spell", "", null, 0, "COMMON",
                Collections.emptyList());
        }
        String name = getOrDefault(object, "name", "Unnamed Spell");
        String description = getOrDefault(object, "description", "");
        CultivationRealm realm = parseRealm(object.get("required_realm"));
        int stage = object.has("required_stage") ? object.get("required_stage").getAsInt() : 0;
        String rarity = getOrDefault(object, "rarity", "COMMON");
        List<String> unlockTags = new ArrayList<>();
        if (object.has("unlocks") && object.get("unlocks").isJsonArray()) {
            object.getAsJsonArray("unlocks").forEach(elem -> unlockTags.add(elem.getAsString()));
        }
        return new SpellDefinition.Metadata(name, description, realm, stage, rarity, unlockTags);
    }

    private SpellDefinition.SwordQiOverride parseSwordQi(JsonObject object) {
        double damageMultiplier = getDouble(object, "damage_multiplier", 1.0D);
        double speedMultiplier = getDouble(object, "speed_multiplier", 1.0D);
        double rangeMultiplier = getDouble(object, "range_multiplier", 1.0D);
        List<SpellDefinition.Effect> extraEffects = parseEffects(object.getAsJsonArray("extra_effects"));
        String requiredAttribute = object.has("required_attribute_tag")
            ? object.get("required_attribute_tag").getAsString() : null;
        return new SpellDefinition.SwordQiOverride(damageMultiplier, speedMultiplier, rangeMultiplier,
            extraEffects, requiredAttribute);
    }

    private Set<String> readTags(JsonObject object) {
        if (object == null || !object.has("tags")) {
            return Collections.emptySet();
        }
        Set<String> tags = new LinkedHashSet<>();
        JsonElement element = object.get("tags");
        if (element.isJsonArray()) {
            for (JsonElement tag : element.getAsJsonArray()) {
                tags.add(tag.getAsString());
            }
        } else if (element.isJsonPrimitive()) {
            tags.add(element.getAsString());
        }
        return tags;
    }

    private String getOrDefault(JsonObject obj, String key, String fallback) {
        return obj != null && obj.has(key) ? obj.get(key).getAsString() : fallback;
    }

    private double getDouble(JsonObject obj, String key, double fallback) {
        return obj != null && obj.has(key) ? obj.get(key).getAsDouble() : fallback;
    }

    private ResourceLocation parseId(JsonElement element, String context) {
        if (element == null || element.isJsonNull()) {
            throw new IllegalArgumentException("Missing resource location for " + context);
        }
        ResourceLocation id = ResourceLocation.tryParse(element.getAsString());
        if (id == null) {
            throw new IllegalArgumentException("Invalid resource location '" + element.getAsString() + "' for " + context);
        }
        return id;
    }

    private CultivationRealm parseRealm(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        String value = element.getAsString();
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return CultivationRealm.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            if ("foundation".equalsIgnoreCase(value)) {
                return CultivationRealm.FOUNDATION_BUILDING;
            }
            CultivationRealm realm = CultivationRealm.fromId(value.toLowerCase(Locale.ROOT));
            if (realm == CultivationRealm.MORTAL && !"mortal".equalsIgnoreCase(value)) {
                Tiandao.LOGGER.warn("未知境界 '{}', 默认使用 MORTAL", value);
            }
            return realm;
        }
    }
}
