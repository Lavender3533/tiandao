package org.example.Kangnaixi.tiandao.spell.builder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.minecraftforge.fml.loading.FMLPaths;
import org.example.Kangnaixi.tiandao.Tiandao;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 负责在 config/tiandao/components/ 目录中读取 Form/Effect/Augment/Template 配置，
 * 并在首次运行写入一份可编辑的默认草案。
 */
public class SpellComponentConfigLoader {

    private static final Gson GSON = new GsonBuilder()
        .disableHtmlEscaping()
        .serializeNulls()
        .setPrettyPrinting()
        .create();

    private static final String FORMS_FILE = "spell_forms.json";
    private static final String EFFECTS_FILE = "spell_effects.json";
    private static final String AUGMENTS_FILE = "spell_augments.json";
    private static final String TEMPLATES_FILE = "spell_templates.json";

    private static final List<DefaultFile> DEFAULT_FILES = List.of(
        new DefaultFile(FORMS_FILE, "/default_components/spell_forms.json"),
        new DefaultFile(EFFECTS_FILE, "/default_components/spell_effects.json"),
        new DefaultFile(AUGMENTS_FILE, "/default_components/spell_augments.json"),
        new DefaultFile(TEMPLATES_FILE, "/default_components/spell_templates.json")
    );

    private final Path componentDir = FMLPaths.CONFIGDIR.get()
        .resolve("tiandao")
        .resolve("components");

    public LoadedComponents loadAll() {
        try {
            Files.createDirectories(componentDir);
            ensureDefaultFiles();
        } catch (IOException e) {
            Tiandao.LOGGER.error("创建术法组件配置目录失败: {}", componentDir, e);
            return LoadedComponents.empty();
        }

        List<FormDefinition> forms = readFile(componentDir.resolve(FORMS_FILE), FormFile.class)
            .map(FormFile::forms)
            .orElse(Collections.emptyList());

        List<EffectDefinition> effects = readFile(componentDir.resolve(EFFECTS_FILE), EffectFile.class)
            .map(EffectFile::effects)
            .orElse(Collections.emptyList());

        List<AugmentDefinition> augments = readFile(componentDir.resolve(AUGMENTS_FILE), AugmentFile.class)
            .map(AugmentFile::augments)
            .orElse(Collections.emptyList());

        List<SpellTemplateDefinition> templates = readFile(componentDir.resolve(TEMPLATES_FILE), TemplateFile.class)
            .map(TemplateFile::templates)
            .orElse(Collections.emptyList());

        return new LoadedComponents(forms, effects, augments, templates);
    }

    private void ensureDefaultFiles() throws IOException {
        for (DefaultFile file : DEFAULT_FILES) {
            Path target = componentDir.resolve(file.fileName());
            if (Files.notExists(target)) {
                try (InputStream in = SpellComponentConfigLoader.class.getResourceAsStream(file.resourcePath())) {
                    if (in == null) {
                        Tiandao.LOGGER.warn("缺少默认组件资源文件: {}", file.resourcePath());
                        continue;
                    }
                    Files.createDirectories(target.getParent());
                    Files.copy(in, target);
                }
            }
        }
    }

    private <T> Optional<T> readFile(Path path, Class<T> type) {
        if (Files.notExists(path)) {
            return Optional.empty();
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            T data = GSON.fromJson(reader, type);
            return Optional.ofNullable(data);
        } catch (JsonSyntaxException ex) {
            Tiandao.LOGGER.error("解析术法组件文件 {} 失败: {}", path.getFileName(), ex.getMessage());
        } catch (IOException e) {
            Tiandao.LOGGER.error("读取术法组件文件 {} 失败", path.getFileName(), e);
        }
        return Optional.empty();
    }

    public record LoadedComponents(
        List<FormDefinition> forms,
        List<EffectDefinition> effects,
        List<AugmentDefinition> augments,
        List<SpellTemplateDefinition> templates
    ) {
        private static LoadedComponents empty() {
            return new LoadedComponents(Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList());
        }
    }

    private record DefaultFile(String fileName, String resourcePath) {}

    private static class FormFile {
        private List<FormDefinition> forms = new ArrayList<>();

        public List<FormDefinition> forms() {
            return forms == null ? Collections.emptyList() : forms;
        }
    }

    private static class EffectFile {
        private List<EffectDefinition> effects = new ArrayList<>();

        public List<EffectDefinition> effects() {
            return effects == null ? Collections.emptyList() : effects;
        }
    }

    private static class AugmentFile {
        private List<AugmentDefinition> augments = new ArrayList<>();

        public List<AugmentDefinition> augments() {
            return augments == null ? Collections.emptyList() : augments;
        }
    }

    private static class TemplateFile {
        private List<SpellTemplateDefinition> templates = new ArrayList<>();

        public List<SpellTemplateDefinition> templates() {
            return templates == null ? Collections.emptyList() : templates;
        }
    }
}
