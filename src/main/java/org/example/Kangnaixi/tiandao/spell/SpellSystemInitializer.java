package org.example.Kangnaixi.tiandao.spell;

import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.Config;
import org.example.Kangnaixi.tiandao.spell.effect.EffectRegistry;
import org.example.Kangnaixi.tiandao.spell.node.ComponentRegistry;
import org.example.Kangnaixi.tiandao.spell.node.execution.ExecutorRegistry;
import org.example.Kangnaixi.tiandao.spell.node.projectile.ProjectileTemplateRegistry;
import org.example.Kangnaixi.tiandao.spell.node.target.TargetStageRegistry;
import org.example.Kangnaixi.tiandao.spell.rune.RuneRegistry;
import org.example.Kangnaixi.tiandao.spell.rune.loader.RuneConfigLoader;

/**
 * Spell system bootstrapper.
 */
public class SpellSystemInitializer {

    private static boolean initialized = false;

    public static synchronized void initialize() {
        if (initialized) {
            Tiandao.LOGGER.warn("SpellSystemInitializer already ran.");
            return;
        }

        if (!Config.enableLegacyNodeSpell) {
            Tiandao.LOGGER.info("Legacy NodeSpell pipeline 已禁用，使用 Cultivation Spell Editor 定义。");
            initialized = true;
            return;
        }

        try {
            Tiandao.LOGGER.info("Initializing spell system ...");
            EffectRegistry.getInstance();
            RuneConfigLoader loader = new RuneConfigLoader();
            loader.loadFromJson("runes/runes.json");
            ComponentRegistry.getInstance();
            ExecutorRegistry.getInstance();
            ProjectileTemplateRegistry.getInstance().loadDefaults();
            TargetStageRegistry.getInstance().bootstrapDefaults();

            int runeCount = RuneRegistry.getInstance().getRuneCount();
            int componentCount = ComponentRegistry.getInstance().getAllComponents().size();
            int executorCount = ExecutorRegistry.getInstance().getAllExecutors().size();
            Tiandao.LOGGER.info("Spell system ready: runes={}, components={}, executors={}",
                runeCount, componentCount, executorCount);

            initialized = true;
        } catch (Exception e) {
            Tiandao.LOGGER.error("Failed to initialize spell system", e);
        }
    }

    public static synchronized void reload() {
        Tiandao.LOGGER.info("Reloading spell system ...");
        RuneRegistry.getInstance().clear();
        EffectRegistry.getInstance().clear();
        ComponentRegistry.getInstance().clear();
        ExecutorRegistry.getInstance().clear();
        ProjectileTemplateRegistry.getInstance().clear();
        TargetStageRegistry.getInstance().clear();
        initialized = false;
        initialize();
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
