package org.example.Kangnaixi.tiandao.client.gui.editor;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 术法编辑器渲染工具类
 */
public class SpellEditorRenderUtils {

    /**
     * 渲染带阴影的面板
     */
    public static void renderPanelWithShadow(GuiGraphics graphics, int x, int y, int width, int height,
                                             int bgColor, int borderColor) {
        // 渲染阴影
        graphics.fill(x + 2, y + 2, x + width + 2, y + height + 2, SpellEditorColors.SHADOW);

        // 渲染背景
        graphics.fill(x, y, x + width, y + height, bgColor);

        // 渲染边框
        renderBorder(graphics, x, y, width, height, borderColor);
    }

    /**
     * 渲染边框
     */
    public static void renderBorder(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        // 上边框
        graphics.fill(x, y, x + width, y + 1, color);
        // 下边框
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        // 左边框
        graphics.fill(x, y, x + 1, y + height, color);
        // 右边框
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    /**
     * 渲染带边框的背景
     */
    public static void renderPanelBackground(GuiGraphics graphics, int x, int y, int width, int height,
                                             int bgColor, int borderColor) {
        // 背景
        graphics.fill(x, y, x + width, y + height, bgColor);
        // 边框
        renderBorder(graphics, x, y, width, height, borderColor);
    }

    /**
     * 渲染进度条
     */
    public static void renderProgressBar(GuiGraphics graphics, int x, int y, int width, int height, double progress) {
        // 背景
        graphics.fill(x, y, x + width, y + height, SpellEditorColors.PROGRESS_BG);

        // 填充（限制在0-1之间）
        progress = Math.max(0, Math.min(1, progress));
        int fillWidth = (int) (width * progress);
        if (fillWidth > 0) {
            graphics.fill(x, y, x + fillWidth, y + height, SpellEditorColors.PROGRESS_FILL);
        }

        // 边框
        renderBorder(graphics, x, y, width, height, SpellEditorColors.BORDER_DARK);
    }

    /**
     * 渲染图标 + 文本
     */
    public static void renderIconText(GuiGraphics graphics, Font font, String icon, String text,
                                      int x, int y, int color) {
        // 渲染图标（假设使用16x16的图标空间）
        if (icon != null && !icon.isEmpty()) {
            graphics.drawString(font, icon, x, y, color, false);
        }

        // 渲染文本（偏移16像素给图标留空间）
        if (text != null && !text.isEmpty()) {
            graphics.drawString(font, text, x + 16, y, color, false);
        }
    }

    /**
     * 渲染居中文本
     */
    public static void renderCenteredText(GuiGraphics graphics, Font font, String text,
                                          int centerX, int y, int color) {
        if (text != null && !text.isEmpty()) {
            int textWidth = font.width(text);
            graphics.drawString(font, text, centerX - textWidth / 2, y, color, false);
        }
    }

    /**
     * 渲染多行文本
     */
    public static void renderMultilineText(GuiGraphics graphics, Font font, String[] lines,
                                           int x, int y, int lineHeight, int color) {
        if (lines == null) return;

        int currentY = y;
        for (String line : lines) {
            if (line != null && !line.isEmpty()) {
                graphics.drawString(font, line, x, currentY, color, false);
            }
            currentY += lineHeight;
        }
    }

    /**
     * 检查鼠标是否在区域内
     */
    public static boolean isMouseOver(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    /**
     * 渲染Tooltip背景
     */
    public static void renderTooltipBackground(GuiGraphics graphics, int x, int y, int width, int height) {
        // 带透明度的深色背景
        graphics.fill(x, y, x + width, y + height, SpellEditorColors.ALPHA_TOOLTIP);
        // 金色边框
        renderBorder(graphics, x, y, width, height, SpellEditorColors.BORDER_GOLD);
    }
}
