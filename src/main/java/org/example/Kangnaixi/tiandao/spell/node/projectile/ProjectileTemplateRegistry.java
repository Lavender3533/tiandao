package org.example.Kangnaixi.tiandao.spell.node.projectile;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.example.Kangnaixi.tiandao.Tiandao;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProjectileTemplateRegistry {

    private static final ProjectileTemplateRegistry INSTANCE = new ProjectileTemplateRegistry();
    private static final Gson GSON = new Gson();

    private final Map<String, ProjectileTemplate> templates = new ConcurrentHashMap<>();

    public static ProjectileTemplateRegistry getInstance() {
        return INSTANCE;
    }

    public void register(ProjectileTemplate template) {
        templates.put(template.getId(), template);
    }

    public ProjectileTemplate get(String id) {
        return templates.get(id);
    }

    public Collection<ProjectileTemplate> getAll() {
        return Collections.unmodifiableCollection(templates.values());
    }

    public void clear() {
        templates.clear();
    }

    public void loadDefaults() {
        clear();
        loadFromResource("spells/projectiles/default.json");
    }

    public void loadFromResource(String path) {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                Tiandao.LOGGER.warn("未找到投射模板资源: {}", path);
                registerFallback();
                return;
            }
            JsonObject root = GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class);
            JsonArray array = root.getAsJsonArray("projectiles");
            if (array == null) {
                Tiandao.LOGGER.warn("投射模板文件 {} 中缺少 projectiles 数组", path);
                registerFallback();
                return;
            }
            for (var element : array) {
                JsonObject obj = element.getAsJsonObject();
                ProjectileTemplate template = new ProjectileTemplate(
                    obj.get("id").getAsString(),
                    obj.get("speed").getAsDouble(),
                    obj.get("gravity").getAsDouble(),
                    obj.get("lifetime").getAsInt(),
                    obj.get("maxTargets").getAsInt(),
                    obj.get("pierceBlocks").getAsBoolean(),
                    obj.has("trail") ? obj.get("trail").getAsString() : "minecraft:crit"
                );
                register(template);
            }
            Tiandao.LOGGER.info("加载 {} 个投射模板", templates.size());
        } catch (Exception ex) {
            Tiandao.LOGGER.error("加载投射模板失败: {}", path, ex);
            registerFallback();
        }
    }

    private void registerFallback() {
        register(new ProjectileTemplate(
            "tiandao:default_projectile",
            1.5,
            0.01,
            200,
            1,
            false,
            "minecraft:crit"
        ));
    }
}

