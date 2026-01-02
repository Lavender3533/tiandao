package org.example.Kangnaixi.tiandao.client.event;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.client.renderer.SpellHandStarRenderer;
import org.example.Kangnaixi.tiandao.client.starchart.StarChartInputHandler;
import org.lwjgl.glfw.GLFW;

/**
 * 鼠标输入事件处理器
 * 用于处理手持法盘界面和星盘的鼠标交互
 *
 * 交互方式：
 * - 滚轮滚动：旋转选择扇区（手持法盘）
 * - 左键点击：确认当前选择（手持法盘）/ 解锁星盘节点（星盘）
 * - 鼠标移动：更新悬停状态（星盘）
 */
@Mod.EventBusSubscriber(modid = Tiandao.MOD_ID, value = Dist.CLIENT)
public class MouseInputHandler {

    private static final String[] SECTOR_NAMES = {"调制", "形态", "效果"};  // 3扇区

    /**
     * 处理鼠标滚轮事件 - 旋转选择扇区
     */
    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        // 只在手盘开启且没有GUI时处理
        if (!SpellHandStarRenderer.isEnabled()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return;
        }

        // 传递滚轮增量给渲染器
        double scrollDelta = event.getScrollDelta();
        if (SpellHandStarRenderer.handleMouseScroll(scrollDelta)) {
            // 取消事件传播（防止切换物品栏等默认行为）
            event.setCanceled(true);
        }
    }

    /**
     * 处理鼠标点击事件 - 确认选择
     */
    @SubscribeEvent
    public static void onMouseClick(InputEvent.MouseButton.Pre event) {
        Minecraft mc = Minecraft.getInstance();

        // 只处理左键点击
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return;
        }

        // 只在按下时触发（不处理释放）
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }

        // 优先处理星盘点击（会自动取消事件）
        try {
            StarChartInputHandler.onMouseClick(event);
            if (event.isCanceled()) {
                return; // 星盘已处理，不再继续
            }
        } catch (Exception e) {
            Tiandao.LOGGER.error("星盘鼠标点击处理时发生错误", e);
        }

        // 如果手持法盘界面开启，处理点击
        if (SpellHandStarRenderer.isEnabled()) {
            if (mc.player != null && mc.screen == null) {
                int selectedIndex = SpellHandStarRenderer.getSelectedIndex();

                if (SpellHandStarRenderer.handleClick()) {
                    // 显示选择消息
                    String message = String.format("§e【选择】§f%s 类型", SECTOR_NAMES[selectedIndex]);
                    mc.player.sendSystemMessage(Component.literal(message));

                    // 取消事件传播（防止破坏方块等默认行为）
                    event.setCanceled(true);
                }
            }
        }
    }
}
