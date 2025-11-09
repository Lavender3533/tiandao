package org.example.Kangnaixi.tiandao.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 基础配置类
 */
public class Config {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // 基础配置选项
    public static final ForgeConfigSpec.BooleanValue ENABLE_LOGGING;
    public static final ForgeConfigSpec.IntValue MAGIC_NUMBER;
    
    // 视觉效果配置
    public static final ForgeConfigSpec.BooleanValue ENABLE_CUSTOM_SHIELD_RENDERER;
    public static final ForgeConfigSpec.BooleanValue ENABLE_SPELL_CASTING_ANIMATION;
    public static final ForgeConfigSpec.IntValue SPELL_PARTICLE_DENSITY;

    static {
        BUILDER.push("General Settings");
        
        ENABLE_LOGGING = BUILDER
                .comment("Enable logging for debugging purposes")
                .define("enableLogging", true);
        
        MAGIC_NUMBER = BUILDER
                .comment("A magic number used for various calculations")
                .defineInRange("magicNumber", 42, 0, 100);
        
        BUILDER.pop();
        
        BUILDER.push("Visual Effects");
        
        ENABLE_CUSTOM_SHIELD_RENDERER = BUILDER
                .comment("Enable custom renderer for Spiritual Shield (sphere with hexagon texture)",
                         "If disabled, will use particle effects as fallback",
                         "启用灵气护盾的自定义渲染器（六边形纹理球体）",
                         "禁用后将使用粒子效果作为备选")
                .define("enableCustomShieldRenderer", true);
        
        ENABLE_SPELL_CASTING_ANIMATION = BUILDER
                .comment("Enable spell casting animations (charging, bursting, trailing)",
                         "启用术法施放动画（蓄力、爆发、余韵）")
                .define("enableSpellCastingAnimation", true);
        
        SPELL_PARTICLE_DENSITY = BUILDER
                .comment("Spell particle density (1-5, default: 3)",
                         "1 = Low (30%), 3 = Medium (100%), 5 = High (150%)",
                         "术法粒子密度（1-5，默认：3）")
                .defineInRange("spellParticleDensity", 3, 1, 5);
        
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
