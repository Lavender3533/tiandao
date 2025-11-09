package org.example.Kangnaixi.tiandao.client.gui;

import net.minecraft.client.Minecraft;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * HUD动画管理器
 * 
 * 负责管理HUD元素的平滑过渡动画，包括：
 * - 灵力条平滑过渡
 * - 数值平滑更新
 * - 冷却旋转动画
 */
public class HudAnimationManager {
    
    private static final HudAnimationManager INSTANCE = new HudAnimationManager();
    
    // 每个玩家的动画状态
    private final Map<UUID, PlayerHudState> playerStates = new HashMap<>();
    
    // 动画参数
    private static final float TRANSITION_SPEED = 0.15f; // 每tick的过渡速度（0.15 = 约0.3秒完成过渡）
    private static final float MIN_DIFF = 0.01f; // 最小差值，小于此值直接设置为目标值
    
    private HudAnimationManager() {
    }
    
    public static HudAnimationManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * 获取平滑过渡后的灵力值
     * 
     * @param playerUuid 玩家UUID
     * @param targetValue 目标值
     * @param partialTick 部分tick
     * @return 平滑过渡后的显示值
     */
    public double getSmoothSpiritPower(UUID playerUuid, double targetValue, float partialTick) {
        PlayerHudState state = getOrCreateState(playerUuid);
        
        // 如果差值很小，直接设置为目标值
        double diff = Math.abs(targetValue - state.displaySpiritPower);
        if (diff < MIN_DIFF) {
            state.displaySpiritPower = targetValue;
            return targetValue;
        }
        
        // 线性插值平滑过渡
        double lerpSpeed = TRANSITION_SPEED * (1.0 + partialTick);
        if (targetValue > state.displaySpiritPower) {
            state.displaySpiritPower = Math.min(targetValue, state.displaySpiritPower + diff * lerpSpeed);
        } else {
            state.displaySpiritPower = Math.max(targetValue, state.displaySpiritPower - diff * lerpSpeed);
        }
        
        return state.displaySpiritPower;
    }
    
    /**
     * 获取平滑过渡后的最大灵力值
     * 
     * @param playerUuid 玩家UUID
     * @param targetValue 目标值
     * @param partialTick 部分tick
     * @return 平滑过渡后的显示值
     */
    public double getSmoothMaxSpiritPower(UUID playerUuid, double targetValue, float partialTick) {
        PlayerHudState state = getOrCreateState(playerUuid);
        
        double diff = Math.abs(targetValue - state.displayMaxSpiritPower);
        if (diff < MIN_DIFF) {
            state.displayMaxSpiritPower = targetValue;
            return targetValue;
        }
        
        double lerpSpeed = TRANSITION_SPEED * (1.0 + partialTick);
        if (targetValue > state.displayMaxSpiritPower) {
            state.displayMaxSpiritPower = Math.min(targetValue, state.displayMaxSpiritPower + diff * lerpSpeed);
        } else {
            state.displayMaxSpiritPower = Math.max(targetValue, state.displayMaxSpiritPower - diff * lerpSpeed);
        }
        
        return state.displayMaxSpiritPower;
    }
    
    /**
     * 获取冷却旋转角度
     * 
     * @param playerUuid 玩家UUID
     * @param spellSlot 术法槽位索引（0-3）
     * @param cooldownProgress 冷却进度（0.0-1.0）
     * @param partialTick 部分tick
     * @return 旋转角度（0-360度）
     */
    public float getCooldownRotation(UUID playerUuid, int spellSlot, float cooldownProgress, float partialTick) {
        PlayerHudState state = getOrCreateState(playerUuid);
        
        // 计算旋转角度：冷却进度 * 360度
        float targetRotation = cooldownProgress * 360.0f;
        
        // 存储每个槽位的旋转角度
        if (!state.cooldownRotations.containsKey(spellSlot)) {
            state.cooldownRotations.put(spellSlot, 0.0f);
        }
        
        float currentRotation = state.cooldownRotations.get(spellSlot);
        
        // 平滑过渡旋转角度
        float diff = targetRotation - currentRotation;
        if (Math.abs(diff) < 1.0f) {
            state.cooldownRotations.put(spellSlot, targetRotation);
            return targetRotation;
        }
        
        // 处理360度循环
        if (diff > 180.0f) {
            diff -= 360.0f;
        } else if (diff < -180.0f) {
            diff += 360.0f;
        }
        
        float newRotation = currentRotation + diff * TRANSITION_SPEED * (1.0f + partialTick);
        
        // 归一化到0-360度
        while (newRotation < 0.0f) newRotation += 360.0f;
        while (newRotation >= 360.0f) newRotation -= 360.0f;
        
        state.cooldownRotations.put(spellSlot, newRotation);
        return newRotation;
    }
    
    /**
     * 清理不再存在的玩家的状态
     */
    public void cleanup() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        
        playerStates.keySet().removeIf(uuid -> 
            mc.level.players().stream().noneMatch(p -> p.getUUID().equals(uuid))
        );
    }
    
    /**
     * 获取或创建玩家的HUD状态
     */
    private PlayerHudState getOrCreateState(UUID playerUuid) {
        return playerStates.computeIfAbsent(playerUuid, uuid -> new PlayerHudState());
    }
    
    /**
     * 玩家的HUD动画状态
     */
    private static class PlayerHudState {
        double displaySpiritPower = 0.0;      // 当前显示的灵力值（平滑过渡）
        double displayMaxSpiritPower = 100.0; // 当前显示的最大灵力值（平滑过渡）
        Map<Integer, Float> cooldownRotations = new HashMap<>(); // 每个槽位的冷却旋转角度
    }
}

