package org.example.Kangnaixi.tiandao.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 修仙系统配置类
 * 用于管理客户端配置选项
 */
public class CultivationConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;
    
    // HUD显示配置
    public static final ForgeConfigSpec.BooleanValue SHOW_HUD;
    public static final ForgeConfigSpec.BooleanValue SHOW_SPIRIT_POWER_BAR;
    public static final ForgeConfigSpec.BooleanValue SHOW_SPIRIT_POWER_TEXT;
    public static final ForgeConfigSpec.BooleanValue SHOW_SPIRIT_ROOT_INFO;
    public static final ForgeConfigSpec.BooleanValue SHOW_REALM_INFO;
    public static final ForgeConfigSpec.BooleanValue SHOW_FOUNDATION_INFO;
    public static final ForgeConfigSpec.BooleanValue SHOW_RECOVERY_RATE;
    
    // HUD位置配置
    public static final ForgeConfigSpec.IntValue HUD_X;
    public static final ForgeConfigSpec.IntValue HUD_Y;
    
    // 粒子效果配置
    public static final ForgeConfigSpec.BooleanValue SHOW_SPIRIT_PARTICLES;
    public static final ForgeConfigSpec.IntValue PARTICLE_SPAWN_RATE;
    public static final ForgeConfigSpec.IntValue PARTICLE_COUNT;
    public static final ForgeConfigSpec.DoubleValue PARTICLE_SPREAD;
    public static final ForgeConfigSpec.DoubleValue PARTICLE_HEIGHT;
    
    static {
        BUILDER.push("HUD Settings");
        
        // HUD显示选项
        SHOW_HUD = BUILDER
            .comment("Show cultivation HUD on screen")
            .define("showHUD", true);
            
        SHOW_SPIRIT_POWER_BAR = BUILDER
            .comment("Show spirit power bar")
            .define("showSpiritPowerBar", true);
            
        SHOW_SPIRIT_POWER_TEXT = BUILDER
            .comment("Show spirit power text")
            .define("showSpiritPowerText", true);
            
        SHOW_SPIRIT_ROOT_INFO = BUILDER
            .comment("Show spiritual root information")
            .define("showSpiritualRootInfo", true);
            
        SHOW_REALM_INFO = BUILDER
            .comment("Show realm information")
            .define("showRealmInfo", true);
            
        SHOW_FOUNDATION_INFO = BUILDER
            .comment("Show foundation (base) information")
            .define("showFoundationInfo", true);
            
        SHOW_RECOVERY_RATE = BUILDER
            .comment("Show spirit power recovery rate")
            .define("showRecoveryRate", true);
        
        // HUD位置设置
        HUD_X = BUILDER
            .comment("HUD X position")
            .defineInRange("hudX", 10, 0, 1000);
            
        HUD_Y = BUILDER
            .comment("HUD Y position")
            .defineInRange("hudY", 10, 0, 1000);
        
        BUILDER.pop();
        
        BUILDER.push("Particle Effects");
        
        // 粒子效果选项
        SHOW_SPIRIT_PARTICLES = BUILDER
            .comment("Show spiritual energy particles around player")
            .define("showSpiritParticles", true);
            
        PARTICLE_SPAWN_RATE = BUILDER
            .comment("Particle spawn rate (higher = less frequent)")
            .defineInRange("particleSpawnRate", 5, 1, 20);
            
        PARTICLE_COUNT = BUILDER
            .comment("Number of particles to spawn each time")
            .defineInRange("particleCount", 3, 1, 10);
            
        PARTICLE_SPREAD = BUILDER
            .comment("Horizontal spread of particles")
            .defineInRange("particleSpread", 1.0, 0.1, 3.0);
            
        PARTICLE_HEIGHT = BUILDER
            .comment("Maximum height of particles from player feet")
            .defineInRange("particleHeight", 2.0, 0.5, 5.0);
        
        BUILDER.pop();
        
        SPEC = BUILDER.build();
    }
}
