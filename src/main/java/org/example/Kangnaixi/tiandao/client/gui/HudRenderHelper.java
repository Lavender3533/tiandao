package org.example.Kangnaixi.tiandao.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import org.example.Kangnaixi.tiandao.client.gui.editor.DaoTheme;

/**
 * HUD 渲染辅助工具类
 *
 * 遵循 TARE 渲染协议：
 * - 分层渲染（阴影 → 底色 → 纹理 → 内容 → 高光）
 * - 程序化生成（无外部纹理）
 * - 统一动画系统
 *
 * @author Kangnaixi
 */
public final class HudRenderHelper {

    private HudRenderHelper() {}

    // ==================== 灵力条渲染 ====================

    /**
     * 渲染灵力条（务实的 4 层渲染 + 可选流光）
     *
     * 性能优化：去除噪点纹理层（每帧循环画像素会严重拖累性能）
     * 简单的渐变已经足够表现羊皮纸质感
     *
     * @param g GuiGraphics 对象
     * @param x 左上角 X 坐标
     * @param y 左上角 Y 坐标
     * @param width 条宽度
     * @param height 条高度
     * @param ratio 填充比例 (0.0 - 1.0)
     * @param isLow 是否低灵力警告
     * @param flashTime 流光效果开始时间（0 表示无流光）
     */
    public static void renderSpiritBar(GuiGraphics g, int x, int y, int width, int height,
                                       double ratio, boolean isLow, long flashTime) {
        // Layer 1: Shadow (阴影 - 2px 偏移模拟厚度)
        renderShadow(g, x, y, width, height);

        // Layer 2: Base (底色渐变 - 羊皮纸质感)
        renderBase(g, x, y, width, height);

        // Layer 3: Fill (灵力填充 + 渐变)
        renderFill(g, x, y, width, height, ratio, isLow);

        // Layer 4: Highlight (高光 - 顶部边缘反光)
        renderHighlight(g, x, y, width, height);

        // Layer 5: Border (双层边框 - 深棕 + 金色)
        renderBorder(g, x, y, width, height);

        // Layer 6: Glow (流光效果 - 可选，回蓝时触发)
        if (flashTime > 0) {
            renderFlowingLight(g, x, y, width, height, ratio, flashTime);
        }
    }

    // ==================== 分层渲染实现 ====================

    /**
     * Layer 1: 阴影层
     * 在底部和右侧偏移 2px 绘制半透明黑色，模拟厚度
     */
    private static void renderShadow(GuiGraphics g, int x, int y, int width, int height) {
        int shadowOffset = 2;
        int shadowColor = 0x40000000; // 25% 黑色
        g.fill(x + shadowOffset, y + shadowOffset,
               x + width + shadowOffset, y + height + shadowOffset,
               shadowColor);
    }

    /**
     * Layer 2: 底色层
     * 使用渐变模拟羊皮纸质感
     */
    private static void renderBase(GuiGraphics g, int x, int y, int width, int height) {
        // 羊皮纸渐变：顶部浅，底部深
        int topColor = 0xFFEBDCCC;    // 浅羊皮纸
        int bottomColor = 0xFFD7CCC8; // 深羊皮纸

        g.fillGradient(x, y, x + width, y + height, topColor, bottomColor);
    }

    // 注意：原 Layer 3 噪点纹理层已删除
    // 原因：每帧循环画像素严重影响性能（HUD 每帧渲染）
    // 简单的渐变已经足够表现羊皮纸质感
    // 如需复杂纹理，应该在静态界面（非 HUD）中使用，或预生成到缓存

    /**
     * Layer 3: 填充层
     * 根据灵力比例填充颜色，支持低灵力警告
     */
    private static void renderFill(GuiGraphics g, int x, int y, int width, int height,
                                   double ratio, boolean isLow) {
        int filled = (int) (ratio * width);

        if (filled <= 0) return;

        // 颜色计算：满灵力（青色）→ 空灵力（红色）
        int fullColor = 0xFF4DD0E1;  // 青色（满）
        int emptyColor = 0xFFE57373; // 红色（空）

        // 低灵力警告：添加呼吸闪烁效果
        if (isLow) {
            float breathAlpha = breathingAlpha(System.currentTimeMillis(), 0.6f, 1.0f);
            emptyColor = adjustAlpha(emptyColor, breathAlpha);
        }

        int fillColor = lerpColor(emptyColor, fullColor, ratio);

        // 渐变填充：左侧深，右侧浅
        int fillColorDark = darken(fillColor, 0.9f);
        g.fillGradient(x, y, x + filled, y + height, fillColorDark, fillColor);
    }

    /**
     * Layer 4: 高光层
     * 顶部边缘绘制白色半透明线条，模拟光泽
     */
    private static void renderHighlight(GuiGraphics g, int x, int y, int width, int height) {
        int highlightColor = 0x40FFFFFF; // 25% 白色
        g.fill(x + 1, y + 1, x + width - 1, y + 2, highlightColor);
    }

    /**
     * Layer 5: 边框层
     * 双层边框：外层深棕，内层金色
     */
    private static void renderBorder(GuiGraphics g, int x, int y, int width, int height) {
        // 外边框：深棕色
        int outerBorder = DaoTheme.BORDER_BROWN;
        drawRect(g, x - 1, y - 1, width + 2, height + 2, outerBorder, 1);

        // 内边框：金色（更细）
        int innerBorder = DaoTheme.BORDER_INNER_GOLD;
        drawRect(g, x, y, width, height, innerBorder, 1);
    }

    /**
     * Layer 6: 流光效果（可选）
     * 灵力恢复时的流光动画
     */
    private static void renderFlowingLight(GuiGraphics g, int x, int y, int width, int height,
                                          double ratio, long startTime) {
        long elapsed = System.currentTimeMillis() - startTime;
        int duration = 300; // 300ms 流光持续时间

        if (elapsed > duration) return;

        float progress = 1.0f - (float) elapsed / duration;
        int alpha = (int) (255 * progress);
        int flashColor = (alpha << 24) | 0x00E1F5FE; // 浅青色流光

        int filled = (int) (ratio * width);

        // 启用加法混合模式
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(
            org.lwjgl.opengl.GL11.GL_SRC_ALPHA,
            org.lwjgl.opengl.GL11.GL_ONE // 加法混合
        );

        g.fill(x, y, x + filled, y + height, flashColor);

        // 恢复默认混合模式
        RenderSystem.defaultBlendFunc();
    }

    // ==================== 辅助函数 ====================

    /**
     * 呼吸动画：基于时间的正弦波动
     *
     * @param time 当前时间（毫秒）
     * @param min 最小值
     * @param max 最大值
     * @return 在 min 和 max 之间波动的值
     */
    public static float breathingAlpha(long time, float min, float max) {
        double wave = Math.sin(time / 500.0); // 500ms 周期
        return (float) ((wave * 0.5 + 0.5) * (max - min) + min);
    }

    /**
     * 颜色插值（线性）
     *
     * @param from 起始颜色 (ARGB)
     * @param to 目标颜色 (ARGB)
     * @param t 插值参数 (0.0 - 1.0)
     * @return 插值后的颜色
     */
    public static int lerpColor(int from, int to, double t) {
        t = Math.max(0.0, Math.min(1.0, t)); // 限制范围

        int a = (int) (((from >> 24) & 0xFF) * (1 - t) + ((to >> 24) & 0xFF) * t);
        int r = (int) (((from >> 16) & 0xFF) * (1 - t) + ((to >> 16) & 0xFF) * t);
        int g = (int) (((from >> 8) & 0xFF) * (1 - t) + ((to >> 8) & 0xFF) * t);
        int b = (int) ((from & 0xFF) * (1 - t) + (to & 0xFF) * t);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * 调整颜色的 Alpha 通道
     */
    private static int adjustAlpha(int color, float alpha) {
        int a = (int) (((color >> 24) & 0xFF) * alpha);
        return (a << 24) | (color & 0x00FFFFFF);
    }

    /**
     * 使颜色变暗
     */
    private static int darken(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = (int) (((color >> 16) & 0xFF) * factor);
        int g = (int) (((color >> 8) & 0xFF) * factor);
        int b = (int) ((color & 0xFF) * factor);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * 绘制矩形边框
     */
    private static void drawRect(GuiGraphics g, int x, int y, int width, int height,
                                 int color, int thickness) {
        // 上
        g.fill(x, y, x + width, y + thickness, color);
        // 下
        g.fill(x, y + height - thickness, x + width, y + height, color);
        // 左
        g.fill(x, y, x + thickness, y + height, color);
        // 右
        g.fill(x + width - thickness, y, x + width, y + height, color);
    }

    // ==================== 响应式布局 ====================

    /**
     * 根据屏幕宽度缩放值
     * 基准：1920x1080
     */
    public static int scale(int value, int screenWidth) {
        return (int) (value * screenWidth / 1920.0);
    }

    /**
     * 获取 HUD 元素的响应式位置
     */
    public static Position getHudPosition(HudElement element, int screenWidth, int screenHeight) {
        return switch (element) {
            case SPIRIT_BAR -> new Position(scale(10, screenWidth), screenHeight - scale(40, screenHeight));
            case HOTBAR -> new Position(screenWidth / 2 - scale(60, screenWidth), screenHeight - scale(20, screenHeight));
            case STATUS -> new Position(scale(10, screenWidth), scale(10, screenHeight));
        };
    }

    /**
     * HUD 元素枚举
     */
    public enum HudElement {
        SPIRIT_BAR,  // 灵力条
        HOTBAR,      // 快捷栏
        STATUS       // 状态显示
    }

    /**
     * 位置数据类
     */
    public record Position(int x, int y) {}
}
