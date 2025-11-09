package org.example.Kangnaixi.tiandao.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.config.Config;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 术法特效渲染管理器
 * 
 * 负责管理所有术法的视觉特效渲染，包括：
 * - 灵气护盾球体
 * - 其他术法的自定义渲染效果
 * - 性能优化和资源管理
 */
public class SpellEffectRenderer {
    
    private static final SpellEffectRenderer INSTANCE = new SpellEffectRenderer();
    
    // 每个玩家的护盾渲染器实例
    private final Map<UUID, ShieldRenderer> shieldRenderers = new HashMap<>();
    
    private SpellEffectRenderer() {
    }
    
    public static SpellEffectRenderer getInstance() {
        return INSTANCE;
    }
    
    /**
     * 渲染所有术法特效
     * 
     * @param poseStack 姿态栈
     * @param partialTick 部分 tick
     */
    public void renderAll(PoseStack poseStack, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        
        // 遍历所有玩家，渲染他们的术法特效
        for (Player player : mc.level.players()) {
            renderPlayerSpells(poseStack, player, partialTick);
        }
        
        // 清理不再存在的玩家的渲染器
        cleanupRenderers();
    }
    
    /**
     * 渲染单个玩家的术法特效
     * 
     * @param poseStack 姿态栈
     * @param player 玩家
     * @param partialTick 部分 tick
     */
    private void renderPlayerSpells(PoseStack poseStack, Player player, float partialTick) {
        boolean configEnabled = Config.ENABLE_CUSTOM_SHIELD_RENDERER.get();
        
        // 直接检查玩家身上的药水效果，而不是通过capability
        // 护盾是通过 ABSORPTION 效果实现的
        boolean hasShield = player.hasEffect(MobEffects.ABSORPTION);
        
        // 调试日志（只在护盾激活时输出，避免日志过多）
        if (hasShield) {
            Tiandao.LOGGER.debug("检测到护盾效果 - 玩家: {}, 配置启用: {}", player.getName().getString(), configEnabled);
        }
        
        // 检查灵气护盾是否激活，并且自定义渲染器已启用
        if (configEnabled && hasShield) {
            renderShield(poseStack, player, partialTick);
        }
        
        // TODO: 添加其他术法的渲染
    }
    
    /**
     * 渲染灵气护盾
     * 
     * @param poseStack 姿态栈
     * @param player 玩家
     * @param partialTick 部分 tick
     */
    private void renderShield(PoseStack poseStack, Player player, float partialTick) {
        UUID playerUuid = player.getUUID();
        
        // 获取或创建护盾渲染器
        ShieldRenderer renderer = shieldRenderers.computeIfAbsent(playerUuid, uuid -> new ShieldRenderer());
        
        // 渲染护盾
        renderer.renderShield(poseStack, player, partialTick);
    }
    
    /**
     * 清理不再存在的玩家的渲染器（性能优化）
     */
    private void cleanupRenderers() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        
        // 移除不在世界中的玩家的渲染器
        shieldRenderers.keySet().removeIf(uuid -> 
            mc.level.players().stream().noneMatch(p -> p.getUUID().equals(uuid))
        );
    }
    
    /**
     * 重置指定玩家的所有渲染器
     * 
     * @param playerUuid 玩家 UUID
     */
    public void resetPlayer(UUID playerUuid) {
        ShieldRenderer shieldRenderer = shieldRenderers.get(playerUuid);
        if (shieldRenderer != null) {
            shieldRenderer.reset();
        }
    }
    
    /**
     * 清除所有渲染器（用于世界切换等情况）
     */
    public void clearAll() {
        shieldRenderers.clear();
    }
}
