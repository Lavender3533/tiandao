package org.example.Kangnaixi.tiandao.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.capability.ICultivation;

@Mod.EventBusSubscriber(modid = Tiandao.MODID, value = Dist.CLIENT)
public final class QiHudOverlay {

    private QiHudOverlay() {}
    private static double lastSpiritPower = -1;
    private static long lastFlashTime = 0;
    @SubscribeEvent
    public static void onRender(RenderGuiOverlayEvent.Post event) {

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) {
            return; // 没有玩家，直接跳过
        }


        mc.player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            double current = cultivation.getSpiritPower();
            double max = Math.max(1.0, cultivation.getMaxSpiritPower());
            // 如果这是第一帧，就先记下来，不做比较
            if (lastSpiritPower < 0) {
                lastSpiritPower = current;
            }
            // 如果当前灵力比上一帧多，说明刚回蓝，记录一下时间
            if (current > lastSpiritPower) {
                lastFlashTime = System.currentTimeMillis();
            }
            // 渲染完条后，把当前值保存，下一帧可以继续比较
            lastSpiritPower = current;
            double ratio = current / max;
            int barWidth = 120;
            int startColor = 0xFF55FFFF;  // 满蓝
            int endColor   = 0xFFFF5555;  // 空蓝
            int filled = (int) (ratio * barWidth);
            int color = lerpColor(startColor, endColor, 1.0 - Math.min(1.0, ratio));




            GuiGraphics g = event.getGuiGraphics();
            int screenWidth = mc.getWindow().getGuiScaledWidth();
            int screenHeight = mc.getWindow().getGuiScaledHeight();

            // 灵力条尺寸/位置（屏幕左下角）

            int barHeight = 10;
            int x = 10;
            int y = screenHeight - 40;

            // 背景条
            g.fill(x, y, x + barWidth, y + barHeight, 0xAA1E1E1E);
            
            g.fill(x, y, x + filled, y + barHeight, color);

            long elapsed = System.currentTimeMillis() - lastFlashTime;
            if (elapsed < 300) { // 300ms 内显示流光
                int alpha = (int) (255 * (1 - elapsed / 300.0));
                int flashColor = (alpha << 24) | 0x00FFFF; // 浅蓝
                g.fill(x, y, x + filled, y + barHeight, flashColor);
            }


            // 外框
            RenderSystem.setShaderColor(1, 1, 1, 1);
            g.renderOutline(x, y, barWidth, barHeight, 0xFF000000);

            // 文本
            String text = String.format("灵力 %.0f / %.0f", current, max);
            g.drawString(mc.font, Component.literal(text), x, y - 12, 0xFFFFFFFF);
        });
    }
    // 颜色插值
    private static int lerpColor(int from, int to, double t) {
        int r = (int) (((from >> 16) & 0xFF) * (1 - t) + ((to >> 16) & 0xFF) * t);
        int g = (int) (((from >> 8) & 0xFF) * (1 - t) + ((to >> 8) & 0xFF) * t);
        int b = (int) ((from & 0xFF) * (1 - t) + (to & 0xFF) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }


}
