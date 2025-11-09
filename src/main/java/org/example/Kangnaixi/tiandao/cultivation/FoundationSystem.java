package org.example.Kangnaixi.tiandao.cultivation;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.capability.ICultivation;

/**
 * 根基系统管理器
 * 处理根基值的受损、恢复和影响
 */
public class FoundationSystem {
    
    /**
     * 处理玩家死亡事件
     */
    public static void onPlayerDeath(ServerPlayer player) {
        player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            int currentFoundation = cultivation.getFoundation();
            int reduction = 10; // 基础减少10点
            
            // 检查是否连续死亡（简化处理，可以根据需要增强）
            // 这里暂时只做基础减少
            
            cultivation.reduceFoundation(reduction);
            
            int newFoundation = cultivation.getFoundation();
            player.sendSystemMessage(Component.literal(
                String.format("§c根基受损！根基值: %d → %d (-%d)", currentFoundation, newFoundation, reduction)
            ), false);
            
            Tiandao.LOGGER.info("玩家 {} 死亡，根基值减少: {} → {}", 
                player.getName().getString(), currentFoundation, newFoundation);
        });
    }
    
    /**
     * 处理玩家重伤事件（生命值低于10%）
     */
    public static void onPlayerCriticalHealth(ServerPlayer player) {
        player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            // 检查是否在重伤冷却期内（30秒）
            long currentTime = System.currentTimeMillis();
            long lastCriticalTime = cultivation.getLastCombatTime();
            long criticalCooldown = 30000; // 30秒
            
            if (currentTime - lastCriticalTime > criticalCooldown) {
                int currentFoundation = cultivation.getFoundation();
                int reduction = 5; // 减少5点
                
                cultivation.reduceFoundation(reduction);
                cultivation.setLastCombatTime(currentTime);
                
                int newFoundation = cultivation.getFoundation();
                player.sendSystemMessage(Component.literal(
                    String.format("§e重伤导致根基受损！根基值: %d → %d (-%d)", currentFoundation, newFoundation, reduction)
                ), false);
                
                Tiandao.LOGGER.info("玩家 {} 重伤，根基值减少: {} → {}", 
                    player.getName().getString(), currentFoundation, newFoundation);
            }
        });
    }
    
    /**
     * 处理突破失败事件
     * @param isMajorBreakthrough 是否为大境界突破
     */
    public static void onBreakthroughFailed(ServerPlayer player, boolean isMajorBreakthrough) {
        player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            int currentFoundation = cultivation.getFoundation();
            int reduction = isMajorBreakthrough ? 10 : 3; // 大境界失败-10，小境界失败-3
            
            cultivation.reduceFoundation(reduction);
            
            int newFoundation = cultivation.getFoundation();
            String message = isMajorBreakthrough ? 
                "§c突破失败，根基严重受损！" : "§e突破失败，根基受损！";
            
            player.sendSystemMessage(Component.literal(
                String.format("%s根基值: %d → %d (-%d)", message, currentFoundation, newFoundation, reduction)
            ), false);
            
            Tiandao.LOGGER.info("玩家 {} 突破失败（{}），根基值减少: {} → {}", 
                player.getName().getString(), 
                isMajorBreakthrough ? "大境界" : "小境界",
                currentFoundation, newFoundation);
        });
    }
    
    /**
     * 检查是否可以突破小境界
     */
    public static boolean canBreakthroughSubRealm(ICultivation cultivation) {
        return cultivation.getFoundation() >= 50;
    }
    
    /**
     * 检查是否可以突破大境界
     */
    public static boolean canBreakthroughMajorRealm(ICultivation cultivation) {
        return cultivation.getFoundation() >= 30;
    }
    
    /**
     * 获取根基状态描述
     */
    public static String getFoundationStatus(int foundation) {
        if (foundation >= 100) {
            return "§a稳固";
        } else if (foundation >= 80) {
            return "§e良好";
        } else if (foundation >= 50) {
            return "§6不稳";
        } else if (foundation >= 30) {
            return "§c受损";
        } else {
            return "§4严重受损";
        }
    }
    
    /**
     * 获取根基对修炼速度的影响系数
     */
    public static double getCultivationSpeedMultiplier(int foundation) {
        if (foundation < 30) {
            return 0.5; // -50%
        } else if (foundation < 50) {
            return 0.75; // -25%
        } else if (foundation < 80) {
            return 0.9; // -10%
        } else {
            return 1.0; // 无影响
        }
    }
    
    /**
     * 获取根基对灵力恢复速度的影响系数
     */
    public static double getSpiritRecoveryMultiplier(int foundation) {
        if (foundation < 30) {
            return 0.7; // -30%
        } else if (foundation < 50) {
            return 0.85; // -15%
        } else {
            return 1.0; // 无影响
        }
    }
}

