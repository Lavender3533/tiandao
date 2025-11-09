package org.example.Kangnaixi.tiandao.cultivation;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.capability.ICultivation;

import java.util.Random;

/**
 * 经验转化系统
 * 处理灵气转化为修炼经验
 */
public class ExperienceConversionSystem {
    
    private static final Random RANDOM = new Random();
    private static final int SPIRIT_PER_CONVERSION = 99; // 每99点灵气转化一次
    private static final int MIN_EXPERIENCE = 1; // 最小经验值
    private static final int MAX_EXPERIENCE = 3; // 最大经验值
    
    /**
     * 追踪玩家消耗的灵气总量
     */
    private static final java.util.Map<java.util.UUID, Double> playerSpiritConsumed = new java.util.HashMap<>();
    
    /**
     * 处理灵气消耗，转化为经验
     * @param player 玩家
     * @param amount 消耗的灵气量
     */
    public static void onSpiritConsumed(ServerPlayer player, double amount) {
        if (amount <= 0) {
            return;
        }
        
        player.getCapability(org.example.Kangnaixi.tiandao.Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            java.util.UUID playerUuid = player.getUUID();
            
            // 累加消耗的灵气
            double totalConsumed = playerSpiritConsumed.getOrDefault(playerUuid, 0.0) + amount;
            playerSpiritConsumed.put(playerUuid, totalConsumed);
            
            // 每99点灵气转化一次经验
            int conversionCount = (int) (totalConsumed / SPIRIT_PER_CONVERSION);
            if (conversionCount > 0) {
                // 计算获得的经验
                int totalExperience = 0;
                for (int i = 0; i < conversionCount; i++) {
                    int experience = MIN_EXPERIENCE + RANDOM.nextInt(MAX_EXPERIENCE - MIN_EXPERIENCE + 1);
                    totalExperience += experience;
                }
                
                // 添加经验
                cultivation.addCultivationExperience(totalExperience);
                
                // 重置累加值（减去已转化的部分）
                totalConsumed -= conversionCount * SPIRIT_PER_CONVERSION;
                playerSpiritConsumed.put(playerUuid, totalConsumed);
                
                // 显示提示消息（只在获得经验时显示，避免刷屏）
                // 打坐时，每获得经验才显示一次提示
                player.sendSystemMessage(Component.literal(
                    String.format("§a修炼获得 %d 点经验 (消耗 %d 点灵气)", 
                        totalExperience, conversionCount * SPIRIT_PER_CONVERSION)
                ), false);
                
                Tiandao.LOGGER.debug("玩家 {} 消耗 {} 点灵气，获得 {} 点修炼经验", 
                    player.getName().getString(), conversionCount * SPIRIT_PER_CONVERSION, totalExperience);
                
                // 检查是否达到突破条件
                checkBreakthrough(player, cultivation);
            }
        });
    }
    
    /**
     * 检查是否达到突破条件
     */
    private static void checkBreakthrough(ServerPlayer player, ICultivation cultivation) {
        int currentExp = cultivation.getCultivationExperience();
        int requiredExp = cultivation.getRequiredExperienceForSubRealm();
        
        if (currentExp >= requiredExp && requiredExp > 0) {
            player.sendSystemMessage(Component.literal(
                "§e修炼经验已满！可以尝试突破到下一小境界"
            ), false);
        }
    }
    
    /**
     * 清理玩家数据（玩家离线时）
     */
    public static void clearPlayerData(java.util.UUID playerUuid) {
        playerSpiritConsumed.remove(playerUuid);
    }
}

