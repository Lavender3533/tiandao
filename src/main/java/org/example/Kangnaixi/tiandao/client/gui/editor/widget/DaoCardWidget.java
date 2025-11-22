package org.example.Kangnaixi.tiandao.client.gui.editor.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.example.Kangnaixi.tiandao.client.gui.editor.ComponentData;
import org.example.Kangnaixi.tiandao.client.gui.editor.DaoTheme;

import java.util.ArrayList;
import java.util.List;

/**
 * 道法卡片组件 - 简洁版
 *
 * 核心交互：
 * - Hover: 边框颜色Lerp过渡（深褐→朱砂）
 * - Click: 印章顿挫效果（下沉+墨迹扩散）
 * - Selected: 角标缩放淡入动画
 *
 * 去除过度工程化：无粒子、无光晕、无旋转
 */
public class DaoCardWidget extends AbstractWidget {

    private final ComponentData data;
    private boolean selected = false;
    private Runnable onClickCallback;

    // 动画状态
    private float hoverProgress = 0.0f;      // Hover进度 [0.0, 1.0]
    private float badgeProgress = 0.0f;      // 角标进度 [0.0, 1.0]
    private float sealProgress = 0.0f;       // 印章进度 [0.0, 1.0]
    private long lastClickTime = 0;          // 上次点击时间
    private int clickX = 0, clickY = 0;      // 点击位置

    // 文本换行缓存
    private List<String> cachedWrappedText = null;
    private int cachedWrapWidth = -1;

    // Tooltip缓存
    private int cachedTooltipWidth = -1;
    private int cachedTooltipHeight = -1;

    public DaoCardWidget(int x, int y, int width, int height, ComponentData data) {
        super(x, y, width, height, Component.literal(data.getDisplayName()));
        this.data = data;
    }

    /**
     * 公共渲染方法（供外部调用）
     */
    public void renderCard(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderWidget(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;

        // 更新动画状态
        updateAnimations(partialTick);

        // 计算印章偏移（点击下沉）
        int sealOffsetY = (int)(sealProgress * DaoTheme.SEAL_STAMP_OFFSET);
        float sealScale = 1.0f - sealProgress * (1.0f - DaoTheme.SEAL_STAMP_SCALE);

        // 计算渲染位置（带印章偏移）
        int renderX = getX();
        int renderY = getY() + sealOffsetY;
        int renderW = (int)(width * sealScale);
        int renderH = (int)(height * sealScale);

        // 居中缩放
        if (sealScale < 1.0f) {
            renderX += (width - renderW) / 2;
            renderY += (height - renderH) / 2;
        }

        // 1. 渲染轻阴影（偏移2px，40%透明黑）
        graphics.fill(renderX + 2, renderY + 2, renderX + renderW + 2, renderY + renderH + 2, DaoTheme.CARD_SHADOW);

        // 2. 渲染背景（根据状态选择颜色，仅颜色Lerp）
        int bgColor;
        if (selected) {
            bgColor = DaoTheme.CARD_BG_SELECTED;
        } else if (isHovered && hoverProgress > 0.3f) {
            // Hover时背景提亮10-15%
            bgColor = DaoTheme.lerpColor(DaoTheme.CARD_BG, DaoTheme.CARD_BG_HOVER, hoverProgress);
        } else {
            bgColor = DaoTheme.CARD_BG; // 默认羊皮纸色
        }
        graphics.fill(renderX, renderY, renderX + renderW, renderY + renderH, bgColor);

        // 3. 渲染单层边框（Hover时颜色Lerp）
        int borderColor = DaoTheme.lerpColor(
            DaoTheme.BORDER_BROWN,
            DaoTheme.BORDER_CINNABAR,
            hoverProgress
        );
        if (selected) {
            borderColor = DaoTheme.BORDER_GOLD; // 选中时金色边框
        }

        // 绘制单层边框（1px）
        graphics.fill(renderX, renderY, renderX + renderW, renderY + 1, borderColor); // 上
        graphics.fill(renderX, renderY + renderH - 1, renderX + renderW, renderY + renderH, borderColor); // 下
        graphics.fill(renderX, renderY, renderX + 1, renderY + renderH, borderColor); // 左
        graphics.fill(renderX + renderW - 1, renderY, renderX + renderW, renderY + renderH, borderColor); // 右

        // 4. 渲染图标（左上）
        String icon = data.getIcon();
        if (icon != null && !icon.isEmpty()) {
            // Hover时图标变朱砂色
            int iconColor = (isHovered && hoverProgress > 0.5f) ? DaoTheme.TEXT_CINNABAR : DaoTheme.TEXT_PRIMARY;
            graphics.drawString(font, icon, renderX + 5, renderY + 5, iconColor, false);
        }

        // 5. 渲染标题（粗体）
        String title = "§l" + data.getDisplayName();
        // Hover时标题变朱砂色
        int titleColor = (isHovered && hoverProgress > 0.5f) ? DaoTheme.TEXT_CINNABAR : DaoTheme.TEXT_PRIMARY;
        graphics.drawString(font, title, renderX + 20, renderY + 5, titleColor, false);

        // 6. 渲染标题下方细分割线（1px，30%透明度）
        int dividerColor = 0x4D000000 | (DaoTheme.DIVIDER_LINE & 0x00FFFFFF); // 30% alpha
        graphics.fill(renderX + 5, renderY + 18, renderX + renderW - 5, renderY + 19, dividerColor);

        // 7. 渲染描述（自动换行，使用缓存）
        String shortDesc = data.getShortDesc();
        if (shortDesc != null && !shortDesc.isEmpty()) {
            int wrapWidth = renderW - 10;
            // 检查缓存是否有效
            if (cachedWrappedText == null || cachedWrapWidth != wrapWidth) {
                cachedWrappedText = wrapText(shortDesc, wrapWidth);
                cachedWrapWidth = wrapWidth;
            }

            int yOffset = renderY + 22; // 增加偏移，避开分割线
            for (String line : cachedWrappedText) {
                graphics.drawString(font, line, renderX + 5, yOffset, DaoTheme.TEXT_SECONDARY, false);
                yOffset += 9;
                if (yOffset - renderY > renderH - 5) break; // 防止溢出
            }
        }

        // 8. 渲染"已选"角标（右上角）
        if (selected && badgeProgress > 0.0f) {
            DaoTheme.renderSelectedBadge(graphics, renderX + renderW, renderY, badgeProgress);
        }

        // 8. Hover时渲染Tooltip
        if (isHovered && !data.getTooltipLines().isEmpty()) {
            renderTooltip(graphics, mouseX, mouseY);
        }
    }

    /**
     * 更新动画状态（每帧调用）
     */
    private void updateAnimations(float partialTick) {
        // Hover动画（Lerp平滑过渡）
        float targetHover = isHovered ? 1.0f : 0.0f;
        hoverProgress = DaoTheme.lerp(hoverProgress, targetHover, DaoTheme.HOVER_SPEED);

        // 角标动画（选中时淡入）
        float targetBadge = selected ? 1.0f : 0.0f;
        badgeProgress = DaoTheme.lerp(badgeProgress, targetBadge, DaoTheme.BADGE_SPEED);

        // 印章动画（点击后自动回弹）
        long currentTime = System.currentTimeMillis();
        long timeSinceClick = currentTime - lastClickTime;
        if (timeSinceClick < DaoTheme.INK_SPLASH_DURATION) {
            // 点击后300ms内，进度从0→1
            sealProgress = Math.min(1.0f, timeSinceClick / (float)DaoTheme.INK_SPLASH_DURATION);
        } else {
            sealProgress = 0.0f; // 超时后重置
        }
    }

    /**
     * 自动换行工具
     */
    private List<String> wrapText(String text, int maxWidth) {
        Font font = Minecraft.getInstance().font;
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            if (font.width(testLine) <= maxWidth) {
                if (currentLine.length() > 0) currentLine.append(" ");
                currentLine.append(word);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    lines.add(word);
                }
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    /**
     * 渲染定制Tooltip（新样式：深色背景+棕色边框，缓存尺寸计算）
     */
    private void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        List<String> tooltipLines = data.getTooltipLines();
        if (!tooltipLines.isEmpty()) {
            Minecraft mc = Minecraft.getInstance();
            int screenWidth = mc.getWindow().getGuiScaledWidth();
            int screenHeight = mc.getWindow().getGuiScaledHeight();

            // 计算tooltip尺寸（使用缓存）
            if (cachedTooltipWidth == -1 || cachedTooltipHeight == -1) {
                int padding = 10;
                int lineHeight = 14;
                int maxWidth = 0;
                for (String line : tooltipLines) {
                    int lineWidth = mc.font.width(line);
                    if (lineWidth > maxWidth) {
                        maxWidth = lineWidth;
                    }
                }
                cachedTooltipWidth = maxWidth + padding * 2;
                cachedTooltipHeight = tooltipLines.size() * lineHeight + padding * 2;
            }

            int tooltipWidth = cachedTooltipWidth;
            int tooltipHeight = cachedTooltipHeight;
            int padding = 10;
            int lineHeight = 14;

            // 智能定位（鼠标+10,+10，溢出则向左/上偏移）
            int tooltipX = mouseX + 10;
            int tooltipY = mouseY + 10;

            if (tooltipX + tooltipWidth > screenWidth - 10) {
                tooltipX = mouseX - tooltipWidth - 10; // 左侧
            }

            if (tooltipY + tooltipHeight > screenHeight - 10) {
                tooltipY = mouseY - tooltipHeight - 10; // 上侧
            }

            if (tooltipX < 10) {
                tooltipX = 10;
            }
            if (tooltipY < 50) { // 避免遮挡顶部栏
                tooltipY = 50;
            }

            // 1. 渲染深色背景（0xFF201815）
            graphics.fill(tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + tooltipHeight, DaoTheme.TOOLTIP_BG);

            // 2. 渲染棕色边框（0xFF8D6E63）
            graphics.fill(tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + 1, DaoTheme.TOOLTIP_BORDER); // 上
            graphics.fill(tooltipX, tooltipY + tooltipHeight - 1, tooltipX + tooltipWidth, tooltipY + tooltipHeight, DaoTheme.TOOLTIP_BORDER); // 下
            graphics.fill(tooltipX, tooltipY, tooltipX + 1, tooltipY + tooltipHeight, DaoTheme.TOOLTIP_BORDER); // 左
            graphics.fill(tooltipX + tooltipWidth - 1, tooltipY, tooltipX + tooltipWidth, tooltipY + tooltipHeight, DaoTheme.TOOLTIP_BORDER); // 右

            // 3. 渲染文本（第一行金色标题，其余米白正文）
            int textY = tooltipY + padding;
            for (int i = 0; i < tooltipLines.size(); i++) {
                String line = tooltipLines.get(i);
                int textColor = (i == 0) ? DaoTheme.TEXT_GOLD : DaoTheme.TEXT_CREAM; // 第一行金色，其余米白
                graphics.drawString(mc.font, line, tooltipX + padding, textY, textColor, false);
                textY += lineHeight;
            }
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        // 触发印章顿挫效果
        lastClickTime = System.currentTimeMillis();
        clickX = (int)mouseX;
        clickY = (int)mouseY;

        // 执行回调
        if (onClickCallback != null) {
            onClickCallback.run();
        }
    }

    // ==================== Getters & Setters ====================

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return selected;
    }

    public ComponentData getData() {
        return data;
    }

    public void setOnClickCallback(Runnable callback) {
        this.onClickCallback = callback;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
