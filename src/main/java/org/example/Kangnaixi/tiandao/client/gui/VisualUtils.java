package org.example.Kangnaixi.tiandao.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

public class VisualUtils {
    public static void drawFrostedGlassPanel(GuiGraphics g, int x, int y, int w, int h) {
        int top = 0x80262626;
        int bottom = 0x80141414;
        g.fillGradient(x, y, x + w, y + h, top, bottom);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f m = g.pose().last().pose();
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        int rim = 3;
        int a = 70;
        int aw = 0x00FFFFFF;
        int c = (a << 24) | aw;
        buf.vertex(m, x, y, 0).color(c).endVertex();
        buf.vertex(m, x + w, y, 0).color(c).endVertex();
        buf.vertex(m, x + w, y + rim, 0).color(c).endVertex();
        buf.vertex(m, x, y + rim, 0).color(c).endVertex();

        buf.vertex(m, x, y + h - rim, 0).color(c).endVertex();
        buf.vertex(m, x + w, y + h - rim, 0).color(c).endVertex();
        buf.vertex(m, x + w, y + h, 0).color(c).endVertex();
        buf.vertex(m, x, y + h, 0).color(c).endVertex();

        buf.vertex(m, x, y, 0).color(c).endVertex();
        buf.vertex(m, x + rim, y, 0).color(c).endVertex();
        buf.vertex(m, x + rim, y + h, 0).color(c).endVertex();
        buf.vertex(m, x, y + h, 0).color(c).endVertex();

        buf.vertex(m, x + w - rim, y, 0).color(c).endVertex();
        buf.vertex(m, x + w, y, 0).color(c).endVertex();
        buf.vertex(m, x + w, y + h, 0).color(c).endVertex();
        buf.vertex(m, x + w - rim, y + h, 0).color(c).endVertex();

        BufferUploader.drawWithShader(buf.end());
        RenderSystem.disableBlend();

        int steps = 12;
        int dotA = 22;
        int dotC = (dotA << 24) | 0x00FFFFFF;
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int cell = Math.max(8, w / 40);
        for (int i = 0; i < steps; i++) {
            int px = x + (i * cell) % (w - 2);
            int py = y + ((i * cell * 3) % (h - 2));
            buf.vertex(m, px, py, 0).color(dotC).endVertex();
            buf.vertex(m, px, py + 1, 0).color(dotC).endVertex();
            buf.vertex(m, px + 1, py + 1, 0).color(dotC).endVertex();
            buf.vertex(m, px + 1, py, 0).color(dotC).endVertex();
        }
        BufferUploader.drawWithShader(buf.end());
    }

    public static void drawGlowText(GuiGraphics g, Font font, String text, int x, int y, int color) {
        int o1 = 0x4010FFFF;
        int o2 = 0x40FF10FF;
        int sh = 0x40000000;
        g.drawString(font, text, x + 1, y + 1, sh);
        g.drawString(font, text, x - 1, y, o1);
        g.drawString(font, text, x + 1, y, o2);
        g.drawString(font, text, x, y, color);
    }

    public static float easeInOutExpo(float t) {
        if (t <= 0) return 0;
        if (t >= 1) return 1;
        if (t < 0.5f) return (float) (Math.pow(2, 20 * t - 10) / 2);
        return (float) ((2 - Math.pow(2, -20 * t + 10)) / 2);
    }
}