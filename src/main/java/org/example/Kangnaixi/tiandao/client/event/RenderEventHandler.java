package org.example.Kangnaixi.tiandao.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.client.renderer.SpellEffectRenderer;
import org.example.Kangnaixi.tiandao.client.starchart.StarChartRenderer;

/**
 * 渲染事件处理器：
 * - 术法特效（AFTER_TRANSLUCENT_BLOCKS）
 * - 识海全息（单独由 MindSeaHoloRenderer 控制开关）
 * - 星盘3D渲染（AFTER_TRANSLUCENT_BLOCKS）
 */
@Mod.EventBusSubscriber(modid = Tiandao.MOD_ID, value = Dist.CLIENT)
public class RenderEventHandler {

    private static boolean mindSeaEnabled = false;

    public static void toggleMindSeaRendering() {
        mindSeaEnabled = !mindSeaEnabled;
        Tiandao.LOGGER.info("识海渲染状态: {}", mindSeaEnabled ? "启用" : "禁用");
    }

    public static void setMindSeaEnabled(boolean enabled) {
        mindSeaEnabled = enabled;
    }

    public static boolean isMindSeaEnabled() {
        return mindSeaEnabled;
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        float partialTick = event.getPartialTick();

        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            try {
                SpellEffectRenderer.getInstance().renderAll(poseStack, partialTick);
            } catch (Exception e) {
                Tiandao.LOGGER.error("术法特效渲染时发生错误", e);
            }

            // 渲染星盘3D节点
            try {
                StarChartRenderer.render(event);
            } catch (Exception e) {
                Tiandao.LOGGER.error("星盘3D渲染时发生错误", e);
            }
        }
    }
}
