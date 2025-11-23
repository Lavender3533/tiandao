package org.example.Kangnaixi.tiandao.client.event;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.client.gui.CultivationHUD;
import org.example.Kangnaixi.tiandao.client.mindsea.MindSeaHoloOverlay;

/**
 * 客户端事件处理器
 * 用于处理游戏渲染事件，显示修仙系统HUD
 * 
 * 重要：使用 Forge 1.20.1+ 的正确 Overlay 注册方式
 */
@Mod.EventBusSubscriber(modid = Tiandao.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientEvents {
    
    /**
     * HUD 覆盖层定义 - 修仙系统
     * 注意：直接使用 Forge 提供的 GuiGraphics 实例，不要创建新实例
     */
    public static final IGuiOverlay CULTIVATION_HUD_OVERLAY = (gui, guiGraphics, partialTick, screenWidth, screenHeight) -> {
        // 只在主游戏界面渲染HUD（没有其他界面打开时）
            Minecraft minecraft = Minecraft.getInstance();
            
        // 确保玩家存在且没有其他界面
            if (minecraft.player == null || minecraft.screen != null) {
                return;
            }
            
        // 渲染HUD（使用 Forge 提供的 guiGraphics）
        CultivationHUD.render(guiGraphics, partialTick);
    };

    /**
     * 注册 GUI 覆盖层事件
     * 在客户端设置阶段注册 HUD overlay
     * 
     * @param event GUI 覆盖层注册事件
     */
    @SubscribeEvent
    public static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
        try {
            // 将修仙HUD注册在原版热栏之上
            event.registerAbove(
                VanillaGuiOverlay.HOTBAR.id(),
                "cultivation_hud",
                CULTIVATION_HUD_OVERLAY
            );

            // 注册识海内视模式背景遮罩（在所有GUI之下）
            event.registerBelow(
                VanillaGuiOverlay.CROSSHAIR.id(),
                "mindsea_holo_overlay",
                new MindSeaHoloOverlay()
            );

            // 新的术法快捷栏HUD由 SpellHotbarOverlay 自己注册
            // 旧的 SPELL_HUD_OVERLAY 已禁用

            Tiandao.LOGGER.info("修仙 HUD 覆盖层注册成功");
        } catch (Exception e) {
            Tiandao.LOGGER.error("HUD 覆盖层注册失败", e);
        }
    }
}
