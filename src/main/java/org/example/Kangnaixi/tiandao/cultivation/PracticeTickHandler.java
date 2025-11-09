package org.example.Kangnaixi.tiandao.cultivation;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.practice.PracticeMethod;
import org.example.Kangnaixi.tiandao.practice.PracticeRegistry;

/**
 * 修炼系统Tick处理器
 * 处理所有正在修炼的玩家
 */
@Mod.EventBusSubscriber(modid = Tiandao.MODID)
public class PracticeTickHandler {
    
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        
        // 遍历所有玩家
        event.getServer().getPlayerList().getPlayers().forEach(player -> {
            player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
                // 只处理正在修炼的玩家
                if (cultivation.isPracticing()) {
                    String methodId = cultivation.getCurrentPracticeMethod();
                    PracticeMethod method = PracticeRegistry.getInstance().getPracticeMethod(methodId);
                    
                    if (method != null) {
                        // 调用修炼方式的tick方法
                        boolean shouldContinue = method.onTick(player, cultivation);
                        
                        if (!shouldContinue) {
                            // 修炼方法返回false，停止修炼
                            cultivation.setPracticing(false);
                            cultivation.setCurrentPracticeMethod("");
                        }
                    } else {
                        // 修炼方式不存在，停止修炼
                        Tiandao.LOGGER.warn("玩家 {} 使用的修炼方式不存在: {}", 
                            player.getName().getString(), methodId);
                        cultivation.setPracticing(false);
                        cultivation.setCurrentPracticeMethod("");
                    }
                }
            });
        });
    }
    
    /**
     * 开始修炼
     * @param player 玩家
     * @param methodId 修炼方式ID
     * @return true如果成功开始，false否则
     */
    public static boolean startPractice(ServerPlayer player, String methodId) {
        PracticeMethod method = PracticeRegistry.getInstance().getPracticeMethod(methodId);
        if (method == null) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c修炼方式不存在: " + methodId));
            return false;
        }
        
        return player.getCapability(Tiandao.CULTIVATION_CAPABILITY).map(cultivation -> {
            // 检查是否已在修炼
            if (cultivation.isPracticing()) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c你已经在修炼中"));
                return false;
            }
            
            // 检查是否可以开始
            if (!method.canStart(player, cultivation)) {
                String reason = method.getCannotStartReason(player, cultivation);
                if (reason != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c" + reason));
                }
                return false;
            }
            
            // 开始修炼
            method.onStart(player, cultivation);
            return true;
        }).orElse(false);
    }
    
    /**
     * 停止修炼
     * @param player 玩家
     * @param reason 停止原因
     */
    public static void stopPractice(ServerPlayer player, String reason) {
        player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            if (!cultivation.isPracticing()) {
                return;
            }
            
            String methodId = cultivation.getCurrentPracticeMethod();
            PracticeMethod method = PracticeRegistry.getInstance().getPracticeMethod(methodId);
            
            if (method != null) {
                method.onStop(player, cultivation, reason);
            } else {
                // 直接清除状态
                cultivation.setPracticing(false);
                cultivation.setCurrentPracticeMethod("");
            }
        });
    }
}

