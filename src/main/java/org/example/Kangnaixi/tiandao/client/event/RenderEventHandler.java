package org.example.Kangnaixi.tiandao.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.client.renderer.SpellEffectRenderer;

/**
 * 渲染事件处理器
 * 
 * 负责监听游戏渲染事件并调用自定义渲染器：
 * - 灵气护盾球体渲染
 * - 其他术法特效渲染
 * - 在正确的渲染阶段（半透明方块之后）进行渲染
 */
@Mod.EventBusSubscriber(modid = Tiandao.MOD_ID, value = Dist.CLIENT)
public class RenderEventHandler {
    
    /**
     * 监听世界渲染事件
     * 
     * 在渲染半透明方块之后渲染术法特效，确保正确的渲染顺序
     * 
     * @param event 渲染事件
     */
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        // 只在半透明方块渲染阶段之后处理
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        
        // 检查客户端状态
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        
        // 获取姿态栈和部分 tick
        PoseStack poseStack = event.getPoseStack();
        float partialTick = event.getPartialTick();
        
        try {
            // 渲染所有术法特效
            SpellEffectRenderer.getInstance().renderAll(poseStack, partialTick);
        } catch (Exception e) {
            // 捕获异常以防止游戏崩溃
            Tiandao.LOGGER.error("术法特效渲染时发生错误", e);
        }
    }
}
