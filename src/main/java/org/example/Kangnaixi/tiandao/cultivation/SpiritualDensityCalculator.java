package org.example.Kangnaixi.tiandao.cultivation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.example.Kangnaixi.tiandao.Tiandao;

import java.util.HashMap;
import java.util.Map;

/**
 * 灵力密度计算器
 * 负责计算玩家当前位置的综合灵力密度
 */
public class SpiritualDensityCalculator {
    
    // 缓存玩家的密度计算结果，避免每tick重复计算
    private static final Map<String, CachedDensity> densityCache = new HashMap<>();
    private static final long CACHE_DURATION = 20; // 缓存1秒（20 ticks）
    
    /**
     * 计算玩家当前位置的总灵力密度
     * @param player 玩家
     * @return 总密度系数（1.0为基准）
     */
    public static double calculateTotalDensity(ServerPlayer player) {
        String playerUUID = player.getStringUUID();
        long currentTime = player.level().getGameTime();
        
        // 检查缓存
        CachedDensity cached = densityCache.get(playerUUID);
        if (cached != null && (currentTime - cached.timestamp) < CACHE_DURATION) {
            return cached.density;
        }
        
        ServerLevel level = (ServerLevel) player.level();
        BlockPos pos = player.blockPosition();
        
        // 计算各项系数
        double biomeDensity = getBiomeDensityMultiplier(level, pos);
        double timeDensity = getTimeDensityMultiplier(level);
        double blockBonus = getBlockDensityBonus(level, pos);
        double heightMultiplier = getHeightDensityMultiplier(pos.getY());
        
        // 最终密度 = 生物群系系数 × 时间系数 × 高度系数 + 方块加成
        // 方块加成是额外的，不参与乘法
        double totalDensity = biomeDensity * timeDensity * heightMultiplier + blockBonus;
        
        // 密度不能低于0.05（最低5%）
        totalDensity = Math.max(0.05, totalDensity);
        
        // 缓存结果
        densityCache.put(playerUUID, new CachedDensity(totalDensity, currentTime));
        
        // 调试日志
        if (Tiandao.LOGGER.isDebugEnabled()) {
            Tiandao.LOGGER.debug("灵力密度计算 [{}] 生物群系:{} 时间:{} 高度:{} 方块加成:{} 总计:{}",
                    player.getName().getString(), biomeDensity, timeDensity, heightMultiplier, blockBonus, totalDensity);
        }
        
        return totalDensity;
    }
    
    /**
     * 获取生物群系的灵力密度系数
     */
    public static double getBiomeDensityMultiplier(Level level, BlockPos pos) {
        // 获取生物群系的Holder，然后获取其ResourceLocation
        var biomeHolder = level.getBiome(pos);
        var biomeKey = biomeHolder.unwrapKey();
        if (biomeKey.isPresent()) {
            return BiomeSpiritualData.getDensityMultiplier(biomeKey.get().location());
        }
        return 1.0; // 默认值
    }
    
    /**
     * 获取时间影响的灵力密度系数
     * 夜晚灵气更充沛
     */
    public static double getTimeDensityMultiplier(Level level) {
        long dayTime = level.getDayTime() % 24000;
        
        // 夜晚时间：13000-23000 (18:00-6:00)
        if (dayTime >= 13000 && dayTime <= 23000) {
            // 检查是否满月
            long moonPhase = level.getMoonPhase();
            if (moonPhase == 0) { // 满月
                return org.example.Kangnaixi.tiandao.Config.fullMoonMultiplier;
            }
            return org.example.Kangnaixi.tiandao.Config.nightDensityMultiplier;
        }
        
        return 1.0; // 白天基准
    }
    
    /**
     * 获取特殊方块带来的灵力密度加成
     * 灵气聚集方块和修炼台会提供范围加成
     */
    public static double getBlockDensityBonus(Level level, BlockPos pos) {
        double bonus = 0.0;
        
        // 检查灵气聚集方块（使用配置的范围和加成）
        int spiritGatheringRange = org.example.Kangnaixi.tiandao.Config.spiritGatheringRange;
        if (isNearSpiritGatheringBlock(level, pos, spiritGatheringRange)) {
            bonus += org.example.Kangnaixi.tiandao.Config.spiritGatheringBonus;
        }
        
        // 检查修炼台（使用配置的范围和加成）
        int cultivationAltarRange = org.example.Kangnaixi.tiandao.Config.cultivationAltarRange;
        if (isNearCultivationAltar(level, pos, cultivationAltarRange)) {
            bonus += org.example.Kangnaixi.tiandao.Config.cultivationAltarBonus;
        }
        
        // 方块加成最多叠加到配置的上限
        return Math.min(bonus, org.example.Kangnaixi.tiandao.Config.maxBlockBonus);
    }
    
    /**
     * 获取高度影响的灵力密度系数
     * 高山灵气充沛，地下灵气稀薄
     */
    public static double getHeightDensityMultiplier(int y) {
        int highThreshold = org.example.Kangnaixi.tiandao.Config.highAltitudeThreshold;
        int lowThreshold = org.example.Kangnaixi.tiandao.Config.lowAltitudeThreshold;
        
        if (y > highThreshold) {
            // 高山：使用配置的系数
            return org.example.Kangnaixi.tiandao.Config.highAltitudeMultiplier;
        } else if (y < lowThreshold) {
            // 地下：使用配置的系数
            return org.example.Kangnaixi.tiandao.Config.lowAltitudeMultiplier;
        }
        return 1.0; // 正常高度
    }
    
    /**
     * 检查附近是否有灵气聚集方块
     */
    private static boolean isNearSpiritGatheringBlock(Level level, BlockPos playerPos, int range) {
        // 在指定范围内搜索灵气聚集方块
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos checkPos = playerPos.offset(x, y, z);
                    if (level.getBlockState(checkPos).getBlock() == org.example.Kangnaixi.tiandao.Tiandao.SPIRIT_GATHERING_BLOCK.get()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * 检查附近是否有修炼台
     */
    private static boolean isNearCultivationAltar(Level level, BlockPos playerPos, int range) {
        // 在指定范围内搜索修炼台
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos checkPos = playerPos.offset(x, y, z);
                    if (level.getBlockState(checkPos).getBlock() == org.example.Kangnaixi.tiandao.Tiandao.CULTIVATION_ALTAR.get()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * 清理玩家的缓存（在玩家离开时调用）
     */
    public static void clearCache(String playerUUID) {
        densityCache.remove(playerUUID);
    }
    
    /**
     * 缓存的密度数据
     */
    private static class CachedDensity {
        final double density;
        final long timestamp;
        
        CachedDensity(double density, long timestamp) {
            this.density = density;
            this.timestamp = timestamp;
        }
    }
}

