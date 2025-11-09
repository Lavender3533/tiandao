package org.example.Kangnaixi.tiandao.cultivation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.LazyOptional;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.capability.ICultivation;
import org.example.Kangnaixi.tiandao.capability.CultivationCapability;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 灵力系统管理器，负责处理所有玩家的灵力相关操作
 */
public class SpiritualEnergyManager {
    private static final String SPIRITUAL_ENERGY_KEY = "SpiritualEnergy";
    private static final Map<UUID, SpiritualEnergy> playerEnergyData = new HashMap<>();
    
    /**
     * 获取玩家的灵力数据，如果不存在则创建新的
     * @param player 玩家对象
     * @return 灵力数据
     */
    public static SpiritualEnergy getPlayerEnergy(Player player) {
        UUID playerUUID = player.getUUID();
        
        if (!playerEnergyData.containsKey(playerUUID)) {
            // 尝试从玩家持久化数据中加载
            SpiritualEnergy energy = loadPlayerEnergy(player);
            if (energy == null) {
                // 如果没有保存的数据，创建新的灵力数据
                energy = new SpiritualEnergy();
            }
            playerEnergyData.put(playerUUID, energy);
        }
        
        return playerEnergyData.get(playerUUID);
    }
    
    /**
     * 从玩家的持久化数据中加载灵力数据
     * @param player 玩家对象
     * @return 灵力数据，如果没有保存的数据则返回null
     */
    public static SpiritualEnergy loadPlayerEnergy(Player player) {
        if (player instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer) player;
            CompoundTag persistentData = serverPlayer.getPersistentData();
            
            if (persistentData.contains(SPIRITUAL_ENERGY_KEY)) {
                CompoundTag energyTag = persistentData.getCompound(SPIRITUAL_ENERGY_KEY);
                return new SpiritualEnergy(energyTag);
            }
        }
        return null;
    }
    
    /**
     * 保存玩家的灵力数据到持久化存储
     * @param player 玩家对象
     */
    public static void savePlayerEnergy(Player player) {
        if (player instanceof ServerPlayer) {
            UUID playerUUID = player.getUUID();
            
            if (playerEnergyData.containsKey(playerUUID)) {
                ServerPlayer serverPlayer = (ServerPlayer) player;
                CompoundTag persistentData = serverPlayer.getPersistentData();
                SpiritualEnergy energy = playerEnergyData.get(playerUUID);
                
                // 更新灵力恢复
                energy.updateRecovery(System.currentTimeMillis());
                
                // 保存到持久化数据
                persistentData.put(SPIRITUAL_ENERGY_KEY, energy.save());
            }
        }
    }
    
    /**
     * 消耗玩家灵力
     * @param player 玩家对象
     * @param amount 消耗量
     * @return 是否成功消耗（灵力是否足够）
     */
    public static boolean consumeEnergy(Player player, float amount) {
        SpiritualEnergy energy = getPlayerEnergy(player);
        boolean result = energy.consumeEnergy(amount);
        
        if (result) {
            Tiandao.LOGGER.debug("玩家 {} 消耗了 {} 点灵力，剩余灵力: {}", 
                player.getName().getString(), amount, energy.getCurrentEnergy());
        } else {
            Tiandao.LOGGER.debug("玩家 {} 灵力不足，无法消耗 {} 点灵力，当前灵力: {}", 
                player.getName().getString(), amount, energy.getCurrentEnergy());
        }
        
        return result;
    }
    
    /**
     * 恢复玩家灵力
     * @param player 玩家对象
     * @param amount 恢复量
     */
    public static void restoreEnergy(Player player, float amount) {
        SpiritualEnergy energy = getPlayerEnergy(player);
        float beforeRestore = energy.getCurrentEnergy();
        energy.restoreEnergy(amount);
        
        Tiandao.LOGGER.debug("玩家 {} 恢复了 {} 点灵力，灵力从 {} 恢复到 {}", 
            player.getName().getString(), amount, beforeRestore, energy.getCurrentEnergy());
    }
    
    /**
     * 更新所有在线玩家的灵力恢复
     * 这个方法应该在每个游戏刻或定期调用
     */
    public static void updateAllPlayersEnergy() {
        long currentTime = System.currentTimeMillis();
        
        for (Map.Entry<UUID, SpiritualEnergy> entry : playerEnergyData.entrySet()) {
            SpiritualEnergy energy = entry.getValue();
            energy.updateRecovery(currentTime);
        }
    }
    
    /**
     * 增加玩家的最大灵力值
     * @param player 玩家对象
     * @param amount 增加量
     */
    public static void increaseMaxEnergy(Player player, float amount) {
        SpiritualEnergy energy = getPlayerEnergy(player);
        float beforeMax = energy.getMaxEnergy();
        energy.increaseMaxEnergy(amount);
        
        Tiandao.LOGGER.debug("玩家 {} 最大灵力值增加了 {} 点，从 {} 增加到 {}", 
            player.getName().getString(), amount, beforeMax, energy.getMaxEnergy());
    }
    
    /**
     * 设置玩家的灵力恢复速度
     * @param player 玩家对象
     * @param rate 恢复速度（每游戏分钟恢复量）
     */
    public static void setRecoveryRate(Player player, float rate) {
        SpiritualEnergy energy = getPlayerEnergy(player);
        energy.setRecoveryRate(rate);
        
        Tiandao.LOGGER.debug("玩家 {} 灵力恢复速度设置为每分钟 {} 点", 
            player.getName().getString(), rate);
    }
    
    /**
     * 更新玩家的最大灵力值
     * @param player 玩家对象
     * @param newMaxEnergy 新的最大灵力值
     */
    public static void updateMaxEnergy(Player player, float newMaxEnergy) {
        SpiritualEnergy energy = getPlayerEnergy(player);
        float oldMaxEnergy = energy.getMaxEnergy();
        energy.setMaxEnergy(newMaxEnergy);
        
        Tiandao.LOGGER.debug("玩家 {} 最大灵力值从 {} 更新到 {}", 
            player.getName().getString(), oldMaxEnergy, newMaxEnergy);
    }
    
    /**
     * 设置玩家的灵力数据
     * @param player 玩家对象
     * @param energy 灵力数据
     */
    public static void setPlayerEnergy(Player player, SpiritualEnergy energy) {
        UUID playerUUID = player.getUUID();
        playerEnergyData.put(playerUUID, energy);
    }
    
    /**
     * 移除下线玩家的灵力数据（从内存中，但数据已保存到持久化存储）
     * @param playerUUID 玩家UUID
     */
    public static void removePlayerEnergy(UUID playerUUID) {
        if (playerEnergyData.containsKey(playerUUID)) {
            playerEnergyData.remove(playerUUID);
            Tiandao.LOGGER.debug("已移除玩家 {} 的灵力数据", playerUUID);
        }
    }
    
    /**
     * 更新玩家的灵力恢复
     * @param player 玩家
     */
    public static void updateRecovery(ServerPlayer player) {
        SpiritualEnergy energy = getPlayerEnergy(player);
        if (energy != null) {
            // 基础恢复速度
            double baseRecoveryRate = 1.0;
            
            // 获取玩家的修仙能力
            LazyOptional<ICultivation> cultivationOpt = player.getCapability(Tiandao.CULTIVATION_CAPABILITY);
            if (cultivationOpt.isPresent()) {
                ICultivation cultivation = cultivationOpt.orElse(null);
                if (cultivation != null) {
                    // 应用灵根和境界的恢复加成
                    baseRecoveryRate *= cultivation.getSpiritPowerRecoveryRate();
                    
                    // 使用CultivationCapability中的getIntensityBasedRecoveryBonus方法
                    if (cultivation instanceof CultivationCapability) {
                        CultivationCapability cap = (CultivationCapability) cultivation;
                        double intensityBonus = cap.getIntensityBasedRecoveryBonus(player);
                        baseRecoveryRate *= intensityBonus;
                    }
                    
                    // 设置新的恢复速度
                    energy.setRecoveryRate((float) baseRecoveryRate);
                }
            }
            
            // 更新灵力恢复
            energy.updateRecovery(System.currentTimeMillis());
            savePlayerEnergy(player);
        }
    }
}