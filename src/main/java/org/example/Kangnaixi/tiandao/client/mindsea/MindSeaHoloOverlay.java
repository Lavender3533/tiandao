package org.example.Kangnaixi.tiandao.client.mindsea;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.joml.Matrix4f;

/**
 * 识海内视模式 - HUD 覆盖层
 * 渲染背景遮罩、星空粒子、提示文字
 */
public class MindSeaHoloOverlay implements IGuiOverlay {

    private static final int STAR_COUNT = 50; // 星空粒子数量
    private static final Star[] stars = new Star[STAR_COUNT];

    static {
        // 初始化星空粒子
        for (int i = 0; i < STAR_COUNT; i++) {
            stars[i] = new Star();
        }
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        MindSeaHoloState state = MindSeaHoloState.getInstance();
        if (!state.isEnabled()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        // 1. 渲染全屏半透明黑遮罩（40%）
        renderBackgroundVeil(graphics, screenWidth, screenHeight);

        // 2. 渲染星空粒子
        renderStarField(graphics, screenWidth, screenHeight, partialTick);

        // 3. 渲染提示文字（屏幕下方）
        renderHintText(graphics, mc.font, screenWidth, screenHeight, state);
    }

    /**
     * 渲染背景遮罩（半透明黑 + 径向渐变）
     */
    private void renderBackgroundVeil(GuiGraphics graphics, int screenWidth, int screenHeight) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f matrix = graphics.pose().last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        // 全屏半透明黑（alpha 30%，保持世界可见）
        int alpha = (int) (0.3f * 255);
        buffer.vertex(matrix, 0, 0, 0).color(0, 0, 0, alpha).endVertex();
        buffer.vertex(matrix, 0, screenHeight, 0).color(0, 0, 0, alpha).endVertex();
        buffer.vertex(matrix, screenWidth, screenHeight, 0).color(0, 0, 0, alpha).endVertex();
        buffer.vertex(matrix, screenWidth, 0, 0).color(0, 0, 0, alpha).endVertex();

        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.disableBlend();
    }

    /**
     * 渲染星空粒子（缓慢移动的白点）
     */
    private void renderStarField(GuiGraphics graphics, int screenWidth, int screenHeight, float partialTick) {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                               com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE); // 加法混合
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f matrix = graphics.pose().last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        long time = System.currentTimeMillis();
        for (int i = 0; i < STAR_COUNT; i++) {
            Star star = stars[i];
            star.update(time, screenWidth, screenHeight);

            // 绘制小方形粒子（2x2 像素）
            float x = star.x;
            float y = star.y;
            float size = star.size;
            int alpha = (int) (star.brightness * 255);

            buffer.vertex(matrix, x, y, 0).color(255, 255, 255, alpha).endVertex();
            buffer.vertex(matrix, x, y + size, 0).color(255, 255, 255, alpha).endVertex();
            buffer.vertex(matrix, x + size, y + size, 0).color(255, 255, 255, alpha).endVertex();
            buffer.vertex(matrix, x + size, y, 0).color(255, 255, 255, alpha).endVertex();
        }

        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    /**
     * 渲染提示文字（贴近界面下方，小字号）
     */
    private void renderHintText(GuiGraphics graphics, Font font, int screenWidth, int screenHeight, MindSeaHoloState state) {
        // 单行简洁控制提示
        String controls = "视角+鼠标悬停选中，左键确认，滚轮远近，ESC退出";

        // 全息青绿色文字
        int textColor = 0xB340E0D0;

        int textX = screenWidth / 2 - font.width(controls) / 2;
        int textY = screenHeight / 2 + 80; // 界面下方

        graphics.drawString(font, controls, textX, textY, textColor, false);
    }

    /**
     * 星空粒子类
     */
    private static class Star {
        float x, y;
        float vx, vy;
        float size;
        float brightness;
        long lastUpdateTime;

        Star() {
            // 随机初始化
            this.x = (float) (Math.random() * 1920);
            this.y = (float) (Math.random() * 1080);
            this.vx = (float) (Math.random() * 0.5 - 0.25); // 缓慢移动
            this.vy = (float) (Math.random() * 0.5 - 0.25);
            this.size = (float) (Math.random() * 2 + 1); // 1-3 像素
            this.brightness = (float) (Math.random() * 0.3 + 0.2); // 20%-50% 亮度
            this.lastUpdateTime = System.currentTimeMillis();
        }

        void update(long currentTime, int screenWidth, int screenHeight) {
            // 更新位置（基于时间增量）
            long dt = currentTime - lastUpdateTime;
            if (dt > 0) {
                x += vx * dt / 16.0f; // 假设 60fps
                y += vy * dt / 16.0f;
                lastUpdateTime = currentTime;
            }

            // 边界循环
            if (x < 0) x = screenWidth;
            if (x > screenWidth) x = 0;
            if (y < 0) y = screenHeight;
            if (y > screenHeight) y = 0;

            // 呼吸效果
            brightness = (float) (0.3 + 0.2 * Math.sin(currentTime / 500.0 + x * 0.01));
        }
    }
}
