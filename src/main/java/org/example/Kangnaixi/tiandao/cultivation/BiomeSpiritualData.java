package org.example.Kangnaixi.tiandao.cultivation;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * 生物群系灵力数据
 * 定义不同生物群系的灵力密度系数
 */
public class BiomeSpiritualData {
    
    private static final Map<ResourceLocation, Double> BIOME_DENSITY_MAP = new HashMap<>();
    private static final double DEFAULT_DENSITY = 1.0; // 默认密度系数
    
    static {
        initializeBiomeDensities();
    }
    
    /**
     * 初始化生物群系灵力密度数据
     */
    private static void initializeBiomeDensities() {
        // === 高灵气区（1.5-2.0x）===
        // 山脉
        addBiomeDensity("mountains", 1.8);
        addBiomeDensity("mountain_edge", 1.7);
        addBiomeDensity("windswept_hills", 1.8);
        addBiomeDensity("windswept_gravelly_hills", 1.7);
        addBiomeDensity("windswept_forest", 1.7);
        addBiomeDensity("stony_peaks", 1.9);
        addBiomeDensity("jagged_peaks", 2.0);
        addBiomeDensity("frozen_peaks", 1.9);
        addBiomeDensity("snowy_slopes", 1.7);
        
        // 竹林
        addBiomeDensity("bamboo_jungle", 1.8);
        
        // 樱花林（如果有模组添加）
        addBiomeDensity("cherry_grove", 1.9);
        
        // 森林
        addBiomeDensity("forest", 1.5);
        addBiomeDensity("flower_forest", 1.7);
        addBiomeDensity("dark_forest", 1.6);
        addBiomeDensity("birch_forest", 1.5);
        addBiomeDensity("old_growth_birch_forest", 1.6);
        addBiomeDensity("old_growth_pine_taiga", 1.6);
        addBiomeDensity("old_growth_spruce_taiga", 1.6);
        
        // 丛林
        addBiomeDensity("jungle", 1.6);
        addBiomeDensity("sparse_jungle", 1.5);
        
        // 蘑菇岛（灵气特殊）
        addBiomeDensity("mushroom_fields", 1.8);
        
        // === 中灵气区（1.0x）===
        // 平原
        addBiomeDensity("plains", 1.0);
        addBiomeDensity("sunflower_plains", 1.1);
        addBiomeDensity("meadow", 1.2);
        
        // 河流和海洋
        addBiomeDensity("river", 1.0);
        addBiomeDensity("frozen_river", 1.0);
        addBiomeDensity("ocean", 1.0);
        addBiomeDensity("cold_ocean", 1.0);
        addBiomeDensity("lukewarm_ocean", 1.0);
        addBiomeDensity("warm_ocean", 1.0);
        addBiomeDensity("deep_ocean", 1.0);
        addBiomeDensity("deep_cold_ocean", 1.0);
        addBiomeDensity("deep_lukewarm_ocean", 1.0);
        addBiomeDensity("deep_frozen_ocean", 1.0);
        
        // 沼泽
        addBiomeDensity("swamp", 1.0);
        addBiomeDensity("mangrove_swamp", 1.1);
        
        // === 低灵气区（0.3-0.5x）===
        // 沙漠
        addBiomeDensity("desert", 0.4);
        
        // 荒地
        addBiomeDensity("badlands", 0.3);
        addBiomeDensity("wooded_badlands", 0.4);
        addBiomeDensity("eroded_badlands", 0.3);
        
        // 冰原
        addBiomeDensity("snowy_plains", 0.5);
        addBiomeDensity("ice_spikes", 0.4);
        addBiomeDensity("snowy_taiga", 0.5);
        
        // 稀树草原
        addBiomeDensity("savanna", 0.5);
        addBiomeDensity("savanna_plateau", 0.5);
        addBiomeDensity("windswept_savanna", 0.4);
        
        // === 下界（0.3x）===
        addBiomeDensity("nether_wastes", 0.3);
        addBiomeDensity("soul_sand_valley", 0.2);
        addBiomeDensity("crimson_forest", 0.3);
        addBiomeDensity("warped_forest", 0.3);
        addBiomeDensity("basalt_deltas", 0.2);
        
        // === 末地（0.1x）===
        addBiomeDensity("the_end", 0.1);
        addBiomeDensity("end_highlands", 0.1);
        addBiomeDensity("end_midlands", 0.1);
        addBiomeDensity("end_barrens", 0.1);
        addBiomeDensity("small_end_islands", 0.1);
    }
    
    /**
     * 添加生物群系灵力密度
     */
    private static void addBiomeDensity(String biomeId, double density) {
        BIOME_DENSITY_MAP.put(ResourceLocation.withDefaultNamespace(biomeId), density);
    }
    
    /**
     * 获取生物群系的灵力密度系数（使用ResourceLocation）
     * @param biomeLocation 生物群系的ResourceLocation
     * @return 密度系数（1.0为基准）
     */
    public static double getDensityMultiplier(ResourceLocation biomeLocation) {
        return BIOME_DENSITY_MAP.getOrDefault(biomeLocation, DEFAULT_DENSITY);
    }
}

