package org.example.Kangnaixi.tiandao.cultivation;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.Kangnaixi.tiandao.Config;
import org.example.Kangnaixi.tiandao.Tiandao;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 蹲伏打坐处理器
 * 检测玩家蹲伏3秒后自动触发打坐修炼
 */
@Mod.EventBusSubscriber(modid = Tiandao.MODID)
public class CrouchMeditationHandler {
    
    // 记录玩家蹲伏开始时间
    private static final Map<UUID, Long> crouchStartTimes = new HashMap<>();
    // 记录玩家上一次的蹲伏状态
    private static final Map<UUID, Boolean> lastCrouchState = new HashMap<>();
    
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        // 只在tick结束阶段处理
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        
        // 检查配置是否启用
        if (!Config.crouchToMeditateEnabled) {
            return;
        }
        
        long currentTime = System.currentTimeMillis() / 50; // 转换为tick
        
        // 遍历所有在线玩家
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            UUID playerId = player.getUUID();
            boolean isCrouching = player.isShiftKeyDown();
            boolean wasСrouching = lastCrouchState.getOrDefault(playerId, false);
            
            // 获取玩家修仙数据
            player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
                // 如果玩家已经在修炼中，跳过
                if (cultivation.isPracticing()) {
                    crouchStartTimes.remove(playerId);
                    lastCrouchState.put(playerId, isCrouching);
                    return;
                }
                
                // 检测蹲伏状态变化
                if (isCrouching && !wasСrouching) {
                    // 刚开始蹲伏，记录时间
                    crouchStartTimes.put(playerId, currentTime);
                    Tiandao.LOGGER.debug("玩家 {} 开始蹲伏", player.getName().getString());
                } else if (!isCrouching && wasСrouching) {
                    // 停止蹲伏，清除记录
                    crouchStartTimes.remove(playerId);
                    Tiandao.LOGGER.debug("玩家 {} 停止蹲伏", player.getName().getString());
                } else if (isCrouching && crouchStartTimes.containsKey(playerId)) {
                    // 持续蹲伏中，检查是否达到触发时间
                    long crouchStartTime = crouchStartTimes.get(playerId);
                    long crouchDuration = currentTime - crouchStartTime;
                    int requiredDuration = Config.crouchToMeditateDuration;
                    
                    if (crouchDuration >= requiredDuration) {
                        // 达到触发时间，尝试开始打坐
                        boolean success = PracticeTickHandler.startPractice(player, "meditation");
                        
                        if (success) {
                            Tiandao.LOGGER.info("玩家 {} 蹲伏 {} tick，自动开始打坐", 
                                player.getName().getString(), crouchDuration);
                        }
                        
                        // 无论成功与否，都清除记录以避免重复触发
                        crouchStartTimes.remove(playerId);
                    }
                }
            });
            
            // 更新上一次的蹲伏状态
            lastCrouchState.put(playerId, isCrouching);
        }
    }
    
    /**
     * 玩家离开服务器时清理数据
     */
    @SubscribeEvent
    public static void onPlayerLogout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID playerId = player.getUUID();
            crouchStartTimes.remove(playerId);
            lastCrouchState.remove(playerId);
        }
    }
}

