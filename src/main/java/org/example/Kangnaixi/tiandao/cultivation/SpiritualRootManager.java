package org.example.Kangnaixi.tiandao.cultivation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.example.Kangnaixi.tiandao.Tiandao;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 灵根系统管理器，负责处理所有玩家的灵根相关操作
 */
public class SpiritualRootManager {
    private static final String SPIRITUAL_ROOT_KEY = "SpiritualRoot";
    private static final Map<UUID, SpiritualRoot> playerRootData = new HashMap<>();
    
    /**
     * 获取玩家的灵根数据，如果不存在则创建新的
     * @param player 玩家对象
     * @return 灵根数据
     */
    public static SpiritualRoot getPlayerSpiritualRoot(Player player) {
        UUID playerUUID = player.getUUID();
        
        if (!playerRootData.containsKey(playerUUID)) {
            // 尝试从玩家持久化数据中加载
            SpiritualRoot root = loadPlayerSpiritualRoot(player);
            if (root == null) {
                // 从配置系统获取灵根类型
                SpiritualRootType rootType = SpiritualRootConfig.getInstance().assignSpiritualRoot((ServerPlayer) player);
                
                // 创建灵根对象
                root = new SpiritualRoot(rootType);
                
                // 记录新生成的灵根信息
                Tiandao.LOGGER.info("玩家 {} 获得了新的灵根：{}", 
                    player.getName().getString(), 
                    root.getType().getDisplayName());
            }
            playerRootData.put(playerUUID, root);
        }
        
        return playerRootData.get(playerUUID);
    }
    
    /**
     * 从玩家的持久化数据中加载灵根数据
     * @param player 玩家对象
     * @return 灵根数据，如果没有保存的数据则返回null
     */
    public static SpiritualRoot loadPlayerSpiritualRoot(Player player) {
        if (player instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer) player;
            CompoundTag persistentData = serverPlayer.getPersistentData();
            
            if (persistentData.contains(SPIRITUAL_ROOT_KEY)) {
                CompoundTag rootTag = persistentData.getCompound(SPIRITUAL_ROOT_KEY);
                return new SpiritualRoot(rootTag);
            }
        }
        return null;
    }
    
    /**
     * 保存玩家的灵根数据到持久化存储
     * @param player 玩家对象
     */
    public static void savePlayerSpiritualRoot(Player player) {
        if (player instanceof ServerPlayer) {
            UUID playerUUID = player.getUUID();
            
            if (playerRootData.containsKey(playerUUID)) {
                ServerPlayer serverPlayer = (ServerPlayer) player;
                CompoundTag persistentData = serverPlayer.getPersistentData();
                SpiritualRoot root = playerRootData.get(playerUUID);
                
                // 保存到持久化数据
                persistentData.put(SPIRITUAL_ROOT_KEY, root.save());
            }
        }
    }
    
    /**
     * 重新分配玩家的灵根（管理员功能）
     * @param player 玩家对象
     * @param type 新的灵根类型，如果为null则随机生成
     * @param quality 新的灵根品质，如果为null则随机生成
     */
    public static void reassignSpiritualRoot(Player player, SpiritualRootType type, SpiritualRootQuality quality) {
        UUID playerUUID = player.getUUID();
        
        // 如果未指定类型或品质，则随机生成
        if (type == null) {
            type = SpiritualRootType.values()[(int) (Math.random() * SpiritualRootType.values().length)];
        }
        if (quality == null) {
            quality = SpiritualRootQuality.randomQuality();
        }
        
        // 创建新的灵根数据
        SpiritualRoot newRoot = new SpiritualRoot(type, quality);
        
        // 替换旧的灵根数据
        playerRootData.put(playerUUID, newRoot);
        
        Tiandao.LOGGER.info("玩家 {} 的灵根已被重新分配为：{} {}", 
            player.getName().getString(), 
            newRoot.getQuality().getDisplayName(), 
            newRoot.getType().getDisplayName());
    }
    
    /**
     * 获取玩家的修炼速度加成
     * @param player 玩家对象
     * @return 修炼速度加成倍数
     */
    public static float getCultivationSpeedBonus(Player player) {
        SpiritualRoot root = getPlayerSpiritualRoot(player);
        return root.getCultivationSpeedBonus();
    }
    
    /**
     * 获取玩家的特殊能力描述
     * @param player 玩家对象
     * @return 特殊能力描述
     */
    public static String getSpecialAbility(Player player) {
        SpiritualRoot root = getPlayerSpiritualRoot(player);
        return root.getSpecialAbility();
    }
    
    /**
     * 设置玩家的灵根数据
     * @param player 玩家对象
     * @param root 灵根数据
     */
    public static void setPlayerSpiritualRoot(Player player, SpiritualRoot root) {
        UUID playerUUID = player.getUUID();
        playerRootData.put(playerUUID, root);
    }
    
    /**
     * 移除下线玩家的灵根数据（从内存中，但数据已保存到持久化存储）
     * @param playerUUID 玩家UUID
     */
    public static void removePlayerSpiritualRoot(UUID playerUUID) {
        if (playerRootData.containsKey(playerUUID)) {
            playerRootData.remove(playerUUID);
            Tiandao.LOGGER.debug("已移除玩家 {} 的灵根数据", playerUUID);
        }
    }
}