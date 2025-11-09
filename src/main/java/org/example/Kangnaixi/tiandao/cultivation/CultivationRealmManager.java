package org.example.Kangnaixi.tiandao.cultivation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 境界管理器，管理所有玩家的境界数据
 */
public class CultivationRealmManager {
    private static final Map<UUID, CultivationRealmData> playerRealms = new HashMap<>();
    
    /**
     * 获取玩家的境界数据
     * @param player 玩家对象
     * @return 境界数据
     */
    public static CultivationRealmData getPlayerRealmData(Player player) {
        return playerRealms.computeIfAbsent(player.getUUID(), k -> new CultivationRealmData());
    }
    
    /**
     * 从NBT加载玩家的境界数据
     * @param player 玩家对象
     * @param nbt NBT数据
     */
    public static void loadPlayerRealmData(Player player, CompoundTag nbt) {
        if (nbt.contains("CultivationRealm")) {
            CultivationRealmData realmData = new CultivationRealmData(nbt.getCompound("CultivationRealm"));
            playerRealms.put(player.getUUID(), realmData);
        } else {
            // 如果没有保存的数据，创建新的境界数据
            playerRealms.put(player.getUUID(), new CultivationRealmData());
        }
    }
    
    /**
     * 保存玩家的境界数据到NBT
     * @param player 玩家对象
     * @param nbt NBT数据
     */
    public static void savePlayerRealmData(Player player, CompoundTag nbt) {
        CultivationRealmData realmData = playerRealms.get(player.getUUID());
        if (realmData != null) {
            nbt.put("CultivationRealm", realmData.save());
        }
    }
    
    /**
     * 移除下线玩家的境界数据
     * @param playerUUID 玩家UUID
     */
    public static void removePlayerRealmData(UUID playerUUID) {
        playerRealms.remove(playerUUID);
    }
    
    /**
     * 增加玩家的修炼进度
     * @param player 玩家对象
     * @param amount 增加量
     * @return 是否达到突破要求
     */
    public static boolean addCultivationProgress(Player player, float amount) {
        CultivationRealmData realmData = getPlayerRealmData(player);
        
        // 应用修炼速度加成
        float speedBonus = realmData.getCultivationSpeedBonus();
        float adjustedAmount = amount * speedBonus;
        
        return realmData.addCultivationProgress(adjustedAmount);
    }
    
    /**
     * 尝试突破境界或等级
     * @param player 玩家对象
     * @return 突破结果
     */
    public static BreakthroughResult tryBreakthrough(Player player) {
        CultivationRealmData realmData = getPlayerRealmData(player);
        
        if (!realmData.tryBreakthrough()) {
            return new BreakthroughResult(false, "进度不足，无法突破", realmData.getCurrentRealm(), realmData.getRealmLevel());
        }
        
        // 获取突破后的境界和等级
        CultivationRealm newRealm = realmData.getCurrentRealm();
        int newLevel = realmData.getRealmLevel();
        
        // 更新灵力上限
        SpiritualEnergyManager.updateMaxEnergy(player, realmData.getMaxEnergy(player));
        
        // 返回突破结果
        String message = String.format("恭喜突破到%s %d级！", newRealm.getDisplayName(), newLevel);
        return new BreakthroughResult(true, message, newRealm, newLevel);
    }
    
    /**
     * 获取玩家当前境界等级的显示文本
     * @param player 玩家对象
     * @return 境界等级显示文本
     */
    public static String getRealmDisplayText(Player player) {
        CultivationRealmData realmData = getPlayerRealmData(player);
        CultivationRealm realm = realmData.getCurrentRealm();
        int level = realmData.getRealmLevel();
        
        if (realm.getMaxLevel() > 0) {
            return String.format("%s %d级", realm.getDisplayName(), level);
        } else {
            return realm.getDisplayName();
        }
    }
    
    /**
     * 获取玩家修炼进度的百分比
     * @param player 玩家对象
     * @return 修炼进度百分比 (0.0 - 1.0)
     */
    public static float getProgressPercentage(Player player) {
        CultivationRealmData realmData = getPlayerRealmData(player);
        float progress = realmData.getCultivationProgress();
        float requirement = realmData.getBreakthroughRequirement();
        
        if (requirement <= 0) {
            return 0.0f;
        }
        
        return Math.min(1.0f, progress / requirement);
    }
    
    /**
     * 获取玩家突破所需的进度
     * @param player 玩家对象
     * @return 突破所需的进度
     */
    public static float getBreakthroughRequirement(Player player) {
        CultivationRealmData realmData = getPlayerRealmData(player);
        return realmData.getBreakthroughRequirement();
    }
    
    /**
     * 获取玩家当前修炼进度
     * @param player 玩家对象
     * @return 当前修炼进度
     */
    public static float getCurrentProgress(Player player) {
        CultivationRealmData realmData = getPlayerRealmData(player);
        return realmData.getCultivationProgress();
    }
    
    /**
     * 设置玩家的境界（用于调试或特殊事件）
     * @param player 玩家对象
     * @param realm 目标境界
     * @param level 境界内等级
     */
    public static void setPlayerRealm(Player player, CultivationRealm realm, int level) {
        CultivationRealmData realmData = getPlayerRealmData(player);
        realmData.setCurrentRealm(realm);
        realmData.setRealmLevel(level);
        realmData.setCultivationProgress(0.0f);
        realmData.setBreakthroughRequirement(realm.getBreakthroughRequirement());
        
        // 更新灵力上限
        SpiritualEnergyManager.updateMaxEnergy(player, realmData.getMaxEnergy(player));
    }
    
    /**
     * 突破结果类
     */
    public static class BreakthroughResult {
        private final boolean success;
        private final String message;
        private final CultivationRealm newRealm;
        private final int newLevel;
        
        public BreakthroughResult(boolean success, String message, CultivationRealm newRealm, int newLevel) {
            this.success = success;
            this.message = message;
            this.newRealm = newRealm;
            this.newLevel = newLevel;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public CultivationRealm getNewRealm() {
            return newRealm;
        }
        
        public int getNewLevel() {
            return newLevel;
        }
    }
}