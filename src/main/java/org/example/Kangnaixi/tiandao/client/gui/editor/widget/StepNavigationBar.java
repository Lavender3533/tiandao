package org.example.Kangnaixi.tiandao.client.gui.editor.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.example.Kangnaixi.tiandao.client.gui.editor.SpellEditorColors;
import org.example.Kangnaixi.tiandao.client.gui.editor.SpellEditorRenderUtils;
import org.example.Kangnaixi.tiandao.client.gui.editor.SpellEditorViewModel;

import java.util.function.Consumer;

/**
 * 步骤导航栏 - 显示4个步骤并支持点击切换
 */
public class StepNavigationBar extends AbstractWidget {
    private final SpellEditorViewModel viewModel;
    private int currentStep; // 0-3
    private Consumer<Integer> onStepChange; // 步骤切换回调

    private static final String[] STEP_NAMES = {
        "骨架", "属性", "效果", "命名"
    };

    private static final String[] STEP_ICONS = {
        "⚡", "🔥", "✨", "📝"
    };

    public StepNavigationBar(int x, int y, int width, int height,
                            SpellEditorViewModel viewModel, int currentStep) {
        super(x, y, width, height, Component.literal("Step Navigation"));
        this.viewModel = viewModel;
        this.currentStep = currentStep;
    }

    public void setOnStepChange(Consumer<Integer> callback) {
        this.onStepChange = callback;
    }

    public void setCurrentStep(int step) {
        this.currentStep = step;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;

        // 渲染背景
        SpellEditorRenderUtils.renderPanelBackground(
            graphics, getX(), getY(), width, height,
            SpellEditorColors.BG_PANEL, SpellEditorColors.BORDER_DARK
        );

        // 计算每个步骤按钮的宽度（留出间隙）
        int stepWidth = (width - 50) / 4; // 减去左右边距和间隙
        int stepHeight = height - 10;
        int yOffset = getY() + 5;

        for (int i = 0; i < 4; i++) {
            int xOffset = getX() + 10 + i * (stepWidth + 5);

            // 检查是否悬停在此步骤上
            boolean hovered = SpellEditorRenderUtils.isMouseOver(
                mouseX, mouseY, xOffset, yOffset, stepWidth, stepHeight
            );

            // 渲染步骤按钮
            renderStepButton(graphics, font, i, xOffset, yOffset, stepWidth, stepHeight, hovered);
        }
    }

    private void renderStepButton(GuiGraphics graphics, Font font, int stepIndex,
                                  int x, int y, int width, int height, boolean hovered) {
        boolean isCurrent = stepIndex == currentStep;
        boolean isCompleted = isStepCompleted(stepIndex);

        // 确定背景色
        int bgColor;
        int borderColor;
        if (isCurrent) {
            bgColor = SpellEditorColors.BUTTON_SELECTED;
            borderColor = SpellEditorColors.BORDER_GOLD;
        } else if (hovered) {
            bgColor = SpellEditorColors.BUTTON_HOVER;
            borderColor = SpellEditorColors.BORDER_LIGHT;
        } else {
            bgColor = SpellEditorColors.BUTTON_NORMAL;
            borderColor = SpellEditorColors.BORDER_DARK;
        }

        // 渲染背景
        graphics.fill(x, y, x + width, y + height, bgColor);
        SpellEditorRenderUtils.renderBorder(graphics, x, y, width, height, borderColor);

        // 渲染步骤编号
        String stepNum = "Step " + (stepIndex + 1);
        int stepNumWidth = font.width(stepNum);
        graphics.drawString(font, stepNum,
            x + (width - stepNumWidth) / 2, y + 3,
            SpellEditorColors.TEXT_DARK, false);

        // 渲染图标和名称
        String icon = STEP_ICONS[stepIndex];
        String name = STEP_NAMES[stepIndex];
        String displayText = icon + " " + name;
        int textWidth = font.width(displayText);
        graphics.drawString(font, displayText,
            x + (width - textWidth) / 2, y + 15,
            isCurrent ? SpellEditorColors.TEXT_GOLD : SpellEditorColors.TEXT_DARK,
            false);

        // 渲染状态指示
        if (isCompleted) {
            // 完成标记
            String checkmark = "§a✓ 已完成";
            int checkWidth = font.width(checkmark);
            graphics.drawString(font, checkmark,
                x + (width - checkWidth) / 2, y + 27,
                SpellEditorColors.TEXT_SUCCESS, false);
        } else if (!isCurrent) {
            // 未完成提示
            String status = getStepStatus(stepIndex);
            int statusWidth = font.width(status);
            graphics.drawString(font, status,
                x + (width - statusWidth) / 2, y + 27,
                SpellEditorColors.TEXT_WARNING, false);
        }
    }

    private boolean isStepCompleted(int stepIndex) {
        switch (stepIndex) {
            case 0: // 骨架
                return viewModel.getSource() != null &&
                       viewModel.getCarrier() != null &&
                       viewModel.getForm() != null;
            case 1: // 属性（可选）
                return viewModel.getAttributes().size() > 0;
            case 2: // 效果（可选）
                return viewModel.getEffects().size() > 0;
            case 3: // 命名
                String name = viewModel.getDisplayName();
                return name != null && !name.isEmpty() && !"未命名术法".equals(name);
            default:
                return false;
        }
    }

    private String getStepStatus(int stepIndex) {
        switch (stepIndex) {
            case 0: // 骨架
                int skeletonCount = 0;
                if (viewModel.getSource() != null) skeletonCount++;
                if (viewModel.getCarrier() != null) skeletonCount++;
                if (viewModel.getForm() != null) skeletonCount++;
                if (skeletonCount == 0) return "§7未开始";
                return "§e待选择" + (3 - skeletonCount) + "项";
            case 1: // 属性
                return viewModel.getAttributes().isEmpty() ? "§7可选" : "§f已选" + viewModel.getAttributes().size() + "个";
            case 2: // 效果
                return viewModel.getEffects().isEmpty() ? "§7可选" : "§f已选" + viewModel.getEffects().size() + "个";
            case 3: // 命名
                return "§7未命名";
            default:
                return "";
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active || !visible) {
            return false;
        }

        // 计算点击的是哪个步骤
        int stepWidth = (width - 50) / 4;
        int stepHeight = height - 10;
        int yOffset = getY() + 5;

        for (int i = 0; i < 4; i++) {
            int xOffset = getX() + 10 + i * (stepWidth + 5);

            if (SpellEditorRenderUtils.isMouseOver((int)mouseX, (int)mouseY,
                                                   xOffset, yOffset, stepWidth, stepHeight)) {
                // 点击了步骤 i
                if (onStepChange != null) {
                    onStepChange.accept(i);
                }
                playDownSound(Minecraft.getInstance().getSoundManager());
                return true;
            }
        }

        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
