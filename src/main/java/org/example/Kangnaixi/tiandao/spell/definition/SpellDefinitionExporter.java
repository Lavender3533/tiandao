package org.example.Kangnaixi.tiandao.spell.definition;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraftforge.fml.loading.FMLPaths;
import org.example.Kangnaixi.tiandao.Tiandao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SpellDefinitionExporter {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private SpellDefinitionExporter() {}

    public static void saveToConfig(String spellId, JsonObject json) throws IOException {
        Path dir = FMLPaths.CONFIGDIR.get().resolve("tiandao").resolve("spell_exports");
        Files.createDirectories(dir);
        Path file = dir.resolve(sanitize(spellId) + ".json");
        Files.writeString(file, GSON.toJson(json), StandardCharsets.UTF_8);
        Tiandao.LOGGER.info("保存术法定义到 {}", file);
    }

    private static String sanitize(String id) {
        return id.replace(':', '_').replace('/', '_');
    }
}
