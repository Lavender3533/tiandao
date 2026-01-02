package org.example.Kangnaixi.tiandao.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.Kangnaixi.tiandao.Tiandao;

/**
 * 灵力条 HUD 渲染器
 *
 * 遵循 TARE 渲染协议：
 * - 5 层分层渲染（阴影 → 底色 → 纹理 → 填充 → 高光）
 * - 程序化生成（无外部纹理）
 * - 统一动画系统
 * - 响应式布局
 *
 * @author Kangnaixi
 */
@Mod.EventBusSubscriber(modid = Tiandao.MODID, value = Dist.CLIENT)
public final class QiHudOverlay {

    private QiHudOverlay() {}

    // ==================== 状态追踪 ====================

    /** 上一帧的灵力值（用于检测回蓝） */
    private static double lastSpiritPower = -1;

    /** 流光效果开始时间 */
    private static long lastFlashTime = 0;

    // ==================== 渲染配置 ====================

    /** 灵力条宽度 */
    private static final int BAR_WIDTH = 120;

    /** 灵力条高度 */
    private static final int BAR_HEIGHT = 10;

    /** 低灵力警告阈值（20%） */
    private static final double LOW_SPIRIT_THRESHOLD = 0.2;

    // ==================== 主渲染方法 ====================

    @SubscribeEvent
    public static void onRender(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) {
            return;
        }

        mc.player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            // 获取灵力数据
            double current = cultivation.getSpiritPower();
            double max = Math.max(1.0, cultivation.getMaxSpiritPower());
            double ratio = current / max;
            boolean isLow = ratio < LOW_SPIRIT_THRESHOLD;

            // 检测回蓝（触发流光效果）
            if (current > lastSpiritPower && lastSpiritPower >= 0) {
                lastFlashTime = System.currentTimeMillis();
            }
            lastSpiritPower = current;

            // 计算位置（响应式布局）
            GuiGraphics g = event.getGuiGraphics();
            int screenWidth = mc.getWindow().getGuiScaledWidth();
            int screenHeight = mc.getWindow().getGuiScaledHeight();

            HudRenderHelper.Position pos = HudRenderHelper.getHudPosition(
                HudRenderHelper.HudElement.SPIRIT_BAR,
                screenWidth,
                screenHeight
            );

            // 使用 HudRenderHelper 渲染灵力条（TARE 协议 5 层渲染）
            HudRenderHelper.renderSpiritBar(
                g,
                pos.x(),
                pos.y(),
                BAR_WIDTH,
                BAR_HEIGHT,
                ratio,
                isLow,
                lastFlashTime
            );

            // 渲染文本信息
            renderText(g, mc, pos.x(), pos.y(), current, max, isLow);
        });
    }

    // ==================== 文本渲染 ====================

    /**
     * 渲染灵力数值和警告文本
     */
    private static void renderText(GuiGraphics g, Minecraft mc, int x, int y,
                                   double current, double max, boolean isLow) {
        // 主文本（灵力数值）
        String mainText = String.format("灵力 %.0f / %.0f", current, max);
        g.drawString(mc.font, mainText, x, y - 12, 0xFFFFFFFF);

        // 低灵力警告
        if (isLow) {
            String warningText = String.format("%.0f / %.0f ⚠ 灵力低于20%%", current, max);

            // 呼吸闪烁效果
            float alpha = HudRenderHelper.breathingAlpha(System.currentTimeMillis(), 0.6f, 1.0f);
            int warningAlpha = (int)(alpha * 255);
            int warningColor = (warningAlpha << 24) | 0x00FF5555; // 红色警告

            g.drawString(mc.font, warningText, x + 10, y - 5, warningColor);
        }
    }
}
