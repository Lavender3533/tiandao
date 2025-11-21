package org.example.Kangnaixi.tiandao.client.gui.editor.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.example.Kangnaixi.tiandao.client.gui.editor.ComponentData;
import org.example.Kangnaixi.tiandao.client.gui.editor.SpellEditorColors;
import org.example.Kangnaixi.tiandao.client.gui.editor.SpellEditorRenderUtils;

import java.util.List;

/**
 * 组件卡片 Widget - 显示单个可选组件
 */
public class ComponentCard extends AbstractWidget {
    private final ComponentData data;
    private boolean selected;
    private Runnable onClickCallback;

    public ComponentCard(int x, int y, int width, int height, ComponentData data) {
        super(x, y, width, height, Component.literal(data.getDisplayName()));
        this.data = data;
        this.selected = false;
    }

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
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;

        // 1. 确定背景色
        int bgColor;
        if (selected) {
            bgColor = SpellEditorColors.CARD_BG_SELECTED;
        } else if (isHovered) {
            bgColor = SpellEditorColors.CARD_BG_HOVER;
        } else {
            bgColor = SpellEditorColors.CARD_BG;
        }

        // 2. 渲染背景和边框
        int borderColor;
        if (selected) {
            borderColor = SpellEditorColors.CARD_BORDER_SELECTED;
        } else if (isHovered) {
            borderColor = SpellEditorColors.CARD_BORDER_HOVER;
        } else {
            borderColor = SpellEditorColors.CARD_BORDER_NORMAL;
        }
        SpellEditorRenderUtils.renderPanelBackground(
            graphics, getX(), getY(), width, height, bgColor, borderColor
        );

        // 3. 渲染图标（左上，大字体）
        String icon = data.getIcon();
        if (icon != null && !icon.isEmpty()) {
            graphics.drawString(font, icon, getX() + 5, getY() + 5, SpellEditorColors.TEXT_DARK, false);
        }

        // 4. 渲染名称（图标右侧，粗体）
        String name = "§l" + data.getDisplayName();
        graphics.drawString(font, name, getX() + 20, getY() + 5, SpellEditorColors.TEXT_DARK, false);

        // 5. 渲染简介（名称下方，小字灰色）
        String shortDesc = data.getShortDesc();
        if (shortDesc != null && !shortDesc.isEmpty()) {
            // 自动换行
            List<String> lines = wrapText(shortDesc, width - 10);
            int yOffset = getY() + 20;
            for (String line : lines) {
                graphics.drawString(font, line, getX() + 5, yOffset, SpellEditorColors.TEXT_GRAY, false);
                yOffset += 9;
                if (yOffset - getY() > height - 5) break; // 防止溢出
            }
        }

        // 6. 如果悬停，渲染Tooltip
        if (isHovered && !data.getTooltipLines().isEmpty()) {
            renderTooltip(graphics, mouseX, mouseY);
        }
    }

    private List<String> wrapText(String text, int maxWidth) {
        Font font = Minecraft.getInstance().font;
        List<String> lines = new java.util.ArrayList<>();
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

    private void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        List<String> tooltipLines = data.getTooltipLines();

        if (!tooltipLines.isEmpty()) {
            Minecraft mc = Minecraft.getInstance();
            int screenWidth = mc.getWindow().getGuiScaledWidth();
            int screenHeight = mc.getWindow().getGuiScaledHeight();
            int navBarBottom = 60; // StepNavigationBar的底部位置 (y=10 + height=50)

            SpellEditorRenderUtils.renderSmartTooltip(
                graphics, mc.font, tooltipLines,
                mouseX, mouseY, screenWidth, screenHeight, navBarBottom
            );
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (onClickCallback != null) {
            onClickCallback.run();
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
