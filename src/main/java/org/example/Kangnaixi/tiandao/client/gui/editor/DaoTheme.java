package org.example.Kangnaixi.tiandao.client.gui.editor;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.opengl.GL11;

/**
 * 道法主题系统 - 古籍羊皮纸配色 + 渲染辅助
 *
 * 设计理念：
 * - 羊皮纸底 (#f6ecd1) + 墨色正文 + 朱砂高亮 + 玉绿进度
 * - 软阴影 + 细描边，避免纯黑
 * - 整体留白，符合古籍排版美学
 */
public final class DaoTheme {

    // ==================== 基础色板 ====================

    /** 羊皮纸背景（中心亮） */
    public static final int BG_PARCHMENT = 0xFFF6ECD1;

    /** 羊皮纸背景（边缘暗） */
    public static final int BG_PARCHMENT_EDGE = 0xFFD7CCC8;

    /** 容器背景（稍深） */
    public static final int BG_CONTAINER = 0xFFEBDCCC;

    /** 卡片默认背景（羊皮纸） */
    public static final int CARD_BG = 0xFFF6ECD1;

    /** 卡片Hover背景（提亮10-15%） */
    public static final int CARD_BG_HOVER = 0xFFFBF1E6;

    /** 卡片选中背景（加深） */
    public static final int CARD_BG_SELECTED = 0xFFE9DCBE;

    // ==================== 边框色 ====================

    /** 深棕色边框（默认） */
    public static final int BORDER_BROWN = 0xFF7A4B2A;

    /** 朱砂色边框（Hover） */
    public static final int BORDER_CINNABAR = 0xFFC0392B;

    /** 金色边框（Selected） */
    public static final int BORDER_GOLD = 0xFFD4A574;

    /** 内边框金色（双层边框内层） */
    public static final int BORDER_INNER_GOLD = 0xFFD4AF37;

    // ==================== 文本色 ====================

    /** 正文墨色 */
    public static final int TEXT_PRIMARY = 0xFF3E2723;

    /** 副文灰褐 */
    public static final int TEXT_SECONDARY = 0xFF6D4C41;

    /** 高亮/错误朱砂 */
    public static final int TEXT_CINNABAR = 0xFFC0392B;

    /** 可点击蓝 */
    public static final int TEXT_LINK = 0xFF1E88E5;

    /** 白色文本（用于深色背景） */
    public static final int TEXT_WHITE = 0xFFFFFFFF;

    /** 米白文本（Tooltip正文） */
    public static final int TEXT_CREAM = 0xFFF5F5DC;

    /** 金色文本（Tooltip标题） */
    public static final int TEXT_GOLD = 0xFFFFD700;

    // ==================== 强调色 ====================

    /** 玉绿进度 */
    public static final int ACCENT_JADE = 0xFF4CAF50;

    /** 玉绿渐变终点 */
    public static final int ACCENT_JADE_LIGHT = 0xFF66BB6A;

    /** 已选角标背景 */
    public static final int BADGE_SELECTED = 0xFF4CAF50;

    // ==================== 阴影层 ====================

    /** 外阴影（15%黑） */
    public static final int SHADOW_OUTER = 0x26000000;

    /** 近阴影（10%棕） */
    public static final int SHADOW_INNER = 0x1A7A4B2A;

    /** 内发光（浅色边缘） */
    public static final int GLOW_INNER = 0xFFE9DCBE;

    /** Tooltip深色背景 */
    public static final int TOOLTIP_BG = 0xFF201815;

    /** Tooltip棕色边框 */
    public static final int TOOLTIP_BORDER = 0xFF8D6E63;

    /** 墨迹扩散效果（半透明黑） */
    public static final int INK_SPLASH = 0x80000000;

    /** 卡片阴影（轻阴影） */
    public static final int CARD_SHADOW = 0x40000000;

    /** 暗角效果（径向渐变） */
    public static final int VIGNETTE_BLACK = 0x00000000; // Alpha 0
    public static final int VIGNETTE_BLACK_MAX = 0x64000000; // Alpha 100

    /** 标题分割线（渐变用） */
    public static final int DIVIDER_LINE = 0xFF3E2723;

    // ==================== 布局常量 ====================

    /** 容器圆角半径 */
    public static final int RADIUS_CONTAINER = 8;

    /** 卡片圆角半径 */
    public static final int RADIUS_CARD = 4;

    /** 角标圆角半径 */
    public static final int RADIUS_BADGE = 3;

    /** 容器最大宽度（响应式） */
    public static final int MAX_CONTAINER_WIDTH = 1200;

    /** 容器宽度百分比（大屏） */
    public static final float CONTAINER_WIDTH_RATIO = 0.92f;

    /** 小屏幕阈值 */
    public static final int SMALL_SCREEN_THRESHOLD = 1000;

    /** 卡片标准宽度（2列布局） */
    public static final int CARD_WIDTH = 300;

    /** 卡片标准高度 */
    public static final int CARD_HEIGHT = 120;

    /** 卡片间距 */
    public static final int CARD_SPACING = 12;

    // ==================== 动画常量 ====================

    /** 印章下沉缩放比例 */
    public static final float SEAL_STAMP_SCALE = 0.98f;

    /** 印章下沉Y偏移 */
    public static final int SEAL_STAMP_OFFSET = 2;

    /** 印章墨迹扩散持续时间(ms) */
    public static final int INK_SPLASH_DURATION = 300;

    /** Hover动画速度系数 */
    public static final float HOVER_SPEED = 0.15f;

    /** 角标动画速度系数 */
    public static final float BADGE_SPEED = 0.2f;

    // ==================== 渲染辅助方法 ====================

    /**
     * 线性插值颜色（RGBA全通道）
     * @param from 起始颜色
     * @param to 目标颜色
     * @param progress 进度 [0.0, 1.0]
     * @return 插值后的颜色
     */
    public static int lerpColor(int from, int to, float progress) {
        progress = Math.max(0.0f, Math.min(1.0f, progress));

        int a = (int) (((from >> 24) & 0xFF) * (1 - progress) + ((to >> 24) & 0xFF) * progress);
        int r = (int) (((from >> 16) & 0xFF) * (1 - progress) + ((to >> 16) & 0xFF) * progress);
        int g = (int) (((from >> 8) & 0xFF) * (1 - progress) + ((to >> 8) & 0xFF) * progress);
        int b = (int) ((from & 0xFF) * (1 - progress) + (to & 0xFF) * progress);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * 平滑插值（用于动画过渡）
     * @param current 当前值
     * @param target 目标值
     * @param speed 速度系数（通常 0.1~0.3）
     * @return 新的当前值
     */
    public static float lerp(float current, float target, float speed) {
        return current + (target - current) * speed;
    }

    /**
     * 渲染三层阴影（外阴影 + 近阴影）
     * 模拟羊皮纸的立体感
     */
    public static void renderLayeredShadow(GuiGraphics graphics, int x, int y, int width, int height) {
        // 外阴影（偏移 0,4px 模糊效果用多层fill模拟）
        graphics.fill(x + 2, y + 4, x + width + 2, y + height + 4, SHADOW_OUTER);
        graphics.fill(x + 1, y + 3, x + width + 1, y + height + 3, SHADOW_OUTER);

        // 近阴影（棕色阴影，偏移 0,1px）
        graphics.fill(x + 1, y + 1, x + width + 1, y + height + 1, SHADOW_INNER);
    }

    /**
     * 渲染圆角矩形背景 + 边框
     * （Minecraft 1.20.1使用近似方法绘制圆角）
     */
    public static void renderRoundedPanel(GuiGraphics graphics, int x, int y, int width, int height,
                                          int bgColor, int borderColor, int radius) {
        // 主体背景
        graphics.fill(x, y, x + width, y + height, bgColor);

        // 边框（四边）
        graphics.fill(x, y, x + width, y + 1, borderColor); // 上
        graphics.fill(x, y + height - 1, x + width, y + height, borderColor); // 下
        graphics.fill(x, y, x + 1, y + height, borderColor); // 左
        graphics.fill(x + width - 1, y, x + width, y + height, borderColor); // 右

        // 圆角修饰（简化版，柔化角落）
        if (radius > 0) {
            // 左上角
            graphics.fill(x, y, x + 1, y + 1, bgColor);
            // 右上角
            graphics.fill(x + width - 1, y, x + width, y + 1, bgColor);
            // 左下角
            graphics.fill(x, y + height - 1, x + 1, y + height, bgColor);
            // 右下角
            graphics.fill(x + width - 1, y + height - 1, x + width, y + height, bgColor);
        }
    }

    /**
     * 渲染"已选"角标（右上角）
     * @param progress 出现动画进度 [0.0, 1.0]
     */
    public static void renderSelectedBadge(GuiGraphics graphics, int x, int y, float progress) {
        if (progress <= 0.0f) return;

        // 角标尺寸
        final int badgeSize = 16;
        final int badgeX = x - badgeSize / 2;
        final int badgeY = y - badgeSize / 2;

        // 缩放动画（从0.5到1.0）
        float scale = 0.5f + progress * 0.5f;
        int scaledSize = (int)(badgeSize * scale);
        int offsetX = (badgeSize - scaledSize) / 2;
        int offsetY = (badgeSize - scaledSize) / 2;

        // Alpha淡入
        int alpha = (int)(progress * 255);
        int bgColor = (alpha << 24) | (BADGE_SELECTED & 0x00FFFFFF);

        // 绘制角标背景（圆角矩形）
        graphics.fill(badgeX + offsetX, badgeY + offsetY,
                     badgeX + offsetX + scaledSize, badgeY + offsetY + scaledSize, bgColor);

        // 绘制"✓"符号（简化为白色小矩形组成的勾）
        if (progress > 0.5f) {
            int checkAlpha = (int)((progress - 0.5f) * 2 * 255);
            int checkColor = (checkAlpha << 24) | 0x00FFFFFF;
            int checkX = badgeX + offsetX + scaledSize / 4;
            int checkY = badgeY + offsetY + scaledSize / 2;
            graphics.fill(checkX, checkY, checkX + 2, checkY + 4, checkColor); // 竖线
            graphics.fill(checkX + 2, checkY + 2, checkX + 6, checkY + 4, checkColor); // 横线
        }
    }

    /**
     * 渲染边缘渐隐遮罩（Fade Mask）
     * 用于滚动区域上下边缘，制造"展开卷轴"效果
     */
    public static void renderFadeMask(GuiGraphics graphics, int x, int y, int width, int height,
                                      boolean top, boolean bottom) {
        final int fadeHeight = 16; // 渐变高度

        if (top) {
            // 上边缘渐隐（从不透明到透明）
            for (int i = 0; i < fadeHeight; i++) {
                int alpha = (int)((1.0f - i / (float)fadeHeight) * 180);
                int color = (alpha << 24) | (BG_PARCHMENT & 0x00FFFFFF);
                graphics.fill(x, y + i, x + width, y + i + 1, color);
            }
        }

        if (bottom) {
            // 下边缘渐隐
            for (int i = 0; i < fadeHeight; i++) {
                int alpha = (int)((1.0f - i / (float)fadeHeight) * 180);
                int color = (alpha << 24) | (BG_PARCHMENT & 0x00FFFFFF);
                graphics.fill(x, y + height - fadeHeight + i, x + width, y + height - fadeHeight + i + 1, color);
            }
        }
    }

    /**
     * 渲染印章墨迹扩散效果（点击时）
     * @param progress 扩散进度 [0.0, 1.0]
     * @param centerX 中心X坐标
     * @param centerY 中心Y坐标
     */
    public static void renderInkSplash(GuiGraphics graphics, float progress, int centerX, int centerY) {
        if (progress <= 0.0f || progress >= 1.0f) return;

        // 墨迹半径随时间扩散
        int radius = (int)(progress * 30);

        // Alpha随时间衰减
        int alpha = (int)((1.0f - progress) * 128);
        int color = (alpha << 24) | (INK_SPLASH & 0x00FFFFFF);

        // 绘制圆形墨迹（用方形近似）
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (dx * dx + dy * dy <= radius * radius) {
                    graphics.fill(centerX + dx, centerY + dy,
                                 centerX + dx + 1, centerY + dy + 1, color);
                }
            }
        }
    }

    /**
     * 启用加法混合模式（用于发光效果）
     */
    public static void enableAdditiveBlend() {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_ONE, GL11.GL_ONE);
    }

    /**
     * 恢复默认混合模式
     */
    public static void resetBlend() {
        RenderSystem.defaultBlendFunc();
    }

    /**
     * 计算响应式容器宽度
     */
    public static int getResponsiveContainerWidth(int screenWidth) {
        if (screenWidth < SMALL_SCREEN_THRESHOLD) {
            return (int)(screenWidth * 0.95f); // 小屏95%
        } else {
            int calculated = (int)(screenWidth * CONTAINER_WIDTH_RATIO);
            return Math.min(calculated, MAX_CONTAINER_WIDTH); // 限制最大宽度
        }
    }

    /**
     * 获取基于状态的边框颜色
     */
    public static int getStateBorderColor(boolean selected, boolean hovered) {
        if (selected) {
            return BORDER_GOLD;
        } else if (hovered) {
            return BORDER_CINNABAR;
        } else {
            return BORDER_BROWN;
        }
    }

    /**
     * 渲染背景渐变（中心亮 → 边缘暗）+ 暗角效果
     * @param graphics GuiGraphics实例
     * @param width 屏幕宽度
     * @param height 屏幕高度
     */
    public static void renderGradientBackground(GuiGraphics graphics, int width, int height) {
        // 1. 渲染径向渐变背景（中心亮 → 边缘暗）
        // 使用fillGradient模拟，从中心向边缘过渡
        int centerX = width / 2;
        int centerY = height / 2;

        // 绘制多层矩形，从中心到边缘逐渐变暗
        for (int i = 0; i < 10; i++) {
            float progress = i / 10.0f;
            int color = lerpColor(BG_PARCHMENT, BG_PARCHMENT_EDGE, progress);

            int offsetX = (int)(centerX * progress);
            int offsetY = (int)(centerY * progress);

            // 绘制渐变矩形
            graphics.fillGradient(
                offsetX, offsetY,
                width - offsetX, height - offsetY,
                color, color
            );
        }

        // 2. 渲染四角暗角效果（径向渐变黑，alpha 0→100）
        renderVignetteCorners(graphics, width, height);
    }

    /**
     * 渲染四角暗角效果
     */
    private static void renderVignetteCorners(GuiGraphics graphics, int width, int height) {
        final int vignetteSize = 120; // 暗角范围

        // 左上角
        for (int y = 0; y < vignetteSize; y++) {
            for (int x = 0; x < vignetteSize; x++) {
                float distance = (float)Math.sqrt(x * x + y * y);
                float maxDistance = (float)Math.sqrt(vignetteSize * vignetteSize * 2);
                float alpha = Math.min(1.0f, distance / maxDistance);
                alpha = 1.0f - alpha; // 反转（近处暗，远处透明）

                int alphaInt = (int)(alpha * 100);
                int color = (alphaInt << 24) | 0x00000000;
                graphics.fill(x, y, x + 1, y + 1, color);
            }
        }

        // 右上角
        for (int y = 0; y < vignetteSize; y++) {
            for (int x = 0; x < vignetteSize; x++) {
                float distance = (float)Math.sqrt(x * x + y * y);
                float maxDistance = (float)Math.sqrt(vignetteSize * vignetteSize * 2);
                float alpha = Math.min(1.0f, distance / maxDistance);
                alpha = 1.0f - alpha;

                int alphaInt = (int)(alpha * 100);
                int color = (alphaInt << 24) | 0x00000000;
                graphics.fill(width - vignetteSize + x, y, width - vignetteSize + x + 1, y + 1, color);
            }
        }

        // 左下角
        for (int y = 0; y < vignetteSize; y++) {
            for (int x = 0; x < vignetteSize; x++) {
                float distance = (float)Math.sqrt(x * x + y * y);
                float maxDistance = (float)Math.sqrt(vignetteSize * vignetteSize * 2);
                float alpha = Math.min(1.0f, distance / maxDistance);
                alpha = 1.0f - alpha;

                int alphaInt = (int)(alpha * 100);
                int color = (alphaInt << 24) | 0x00000000;
                graphics.fill(x, height - vignetteSize + y, x + 1, height - vignetteSize + y + 1, color);
            }
        }

        // 右下角
        for (int y = 0; y < vignetteSize; y++) {
            for (int x = 0; x < vignetteSize; x++) {
                float distance = (float)Math.sqrt(x * x + y * y);
                float maxDistance = (float)Math.sqrt(vignetteSize * vignetteSize * 2);
                float alpha = Math.min(1.0f, distance / maxDistance);
                alpha = 1.0f - alpha;

                int alphaInt = (int)(alpha * 100);
                int color = (alphaInt << 24) | 0x00000000;
                graphics.fill(width - vignetteSize + x, height - vignetteSize + y,
                             width - vignetteSize + x + 1, height - vignetteSize + y + 1, color);
            }
        }
    }

    /**
     * 渲染居中容器 + 双层边框 + 投影
     * @param graphics GuiGraphics实例
     * @param screenWidth 屏幕宽度
     * @param screenHeight 屏幕高度
     * @return 容器边界数组 [panelX, panelY, panelW, panelH]
     */
    public static int[] renderCenteredContainer(GuiGraphics graphics, int screenWidth, int screenHeight) {
        // 计算容器尺寸和位置
        int panelW = Math.min(screenWidth - 40, 1000);
        int panelX = (screenWidth - panelW) / 2;
        int panelY = 20;
        int panelH = screenHeight - 80;

        // 1. 渲染投影（半透明黑，偏移Y+4）
        graphics.fill(panelX + 2, panelY + 4, panelX + panelW + 2, panelY + panelH + 4, 0x60000000);

        // 2. 渲染容器背景
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, BG_CONTAINER);

        // 3. 渲染外层边框（3px深褐色 #7a4b2a）
        for (int i = 0; i < 3; i++) {
            graphics.fill(panelX - i, panelY - i, panelX + panelW + i, panelY - i + 1, BORDER_BROWN); // 上
            graphics.fill(panelX - i, panelY + panelH + i - 1, panelX + panelW + i, panelY + panelH + i, BORDER_BROWN); // 下
            graphics.fill(panelX - i, panelY - i, panelX - i + 1, panelY + panelH + i, BORDER_BROWN); // 左
            graphics.fill(panelX + panelW + i - 1, panelY - i, panelX + panelW + i, panelY + panelH + i, BORDER_BROWN); // 右
        }

        // 4. 渲染内层边框（1px金色 #d4af37，内缩2px）
        int innerOffset = 2;
        graphics.fill(panelX + innerOffset, panelY + innerOffset,
                     panelX + panelW - innerOffset, panelY + innerOffset + 1, BORDER_INNER_GOLD); // 上
        graphics.fill(panelX + innerOffset, panelY + panelH - innerOffset - 1,
                     panelX + panelW - innerOffset, panelY + panelH - innerOffset, BORDER_INNER_GOLD); // 下
        graphics.fill(panelX + innerOffset, panelY + innerOffset,
                     panelX + innerOffset + 1, panelY + panelH - innerOffset, BORDER_INNER_GOLD); // 左
        graphics.fill(panelX + panelW - innerOffset - 1, panelY + innerOffset,
                     panelX + panelW - innerOffset, panelY + panelH - innerOffset, BORDER_INNER_GOLD); // 右

        return new int[]{panelX, panelY, panelW, panelH};
    }

    /**
     * 渲染卡片标题下方的渐变分割线
     * @param graphics GuiGraphics实例
     * @param x 起始X坐标
     * @param y Y坐标
     * @param width 线宽度
     */
    public static void renderTitleDivider(GuiGraphics graphics, int x, int y, int width) {
        // 1px渐变线：透明 → #3e2723 → 透明
        int segments = width;
        for (int i = 0; i < segments; i++) {
            float progress = i / (float)segments;
            float alpha;

            if (progress < 0.5f) {
                // 前半段：透明 → 不透明
                alpha = progress * 2.0f;
            } else {
                // 后半段：不透明 → 透明
                alpha = (1.0f - progress) * 2.0f;
            }

            int alphaInt = (int)(alpha * 255);
            int color = (alphaInt << 24) | (DIVIDER_LINE & 0x00FFFFFF);
            graphics.fill(x + i, y, x + i + 1, y + 1, color);
        }
    }

    // 私有构造器，防止实例化
    private DaoTheme() {}
}
