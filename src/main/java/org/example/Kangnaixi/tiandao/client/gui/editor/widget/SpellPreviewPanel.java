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

/**
 * 术法预览面板 - 显示当前选择的组件和术法摘要
 */
public class SpellPreviewPanel extends AbstractWidget {
    private final SpellEditorViewModel viewModel;

    public SpellPreviewPanel(int x, int y, int width, int height, SpellEditorViewModel viewModel) {
        super(x, y, width, height, Component.literal("Spell Preview"));
        this.viewModel = viewModel;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;

        // 1. 渲染面板背景
        SpellEditorRenderUtils.renderPanelWithShadow(
            graphics, getX(), getY(), width, height,
            SpellEditorColors.BG_PANEL, SpellEditorColors.BORDER_DARK
        );

        int yOffset = getY() + 10;

        // 2. 渲染标题
        String title = "§6§l当前术法预览";
        graphics.drawString(font, title, getX() + 10, yOffset, SpellEditorColors.TEXT_GOLD, false);
        yOffset += 20;

        // 3. 渲染骨架部分
        yOffset = renderSection(graphics, font, "§e【骨架】", yOffset);
        yOffset += 2;
        yOffset = renderItem(graphics, font, "起手式", getSourceName(), yOffset);
        yOffset = renderItem(graphics, font, "载体", getCarrierName(), yOffset);
        yOffset = renderItem(graphics, font, "术式", getFormName(), yOffset);
        yOffset += 10;

        // 4. 渲染属性部分
        yOffset = renderSection(graphics, font, "§e【属性】", yOffset);
        yOffset += 2;
        String attrs = getAttributesText();
        yOffset = renderItem(graphics, font, "已选", attrs, yOffset);
        yOffset += 10;

        // 5. 渲染效果部分
        yOffset = renderSection(graphics, font, "§e【效果】", yOffset);
        yOffset += 2;
        String effs = getEffectsText();
        yOffset = renderItem(graphics, font, "已选", effs, yOffset);
        yOffset += 15;

        // 6. 渲染分隔线
        graphics.fill(getX() + 10, yOffset, getX() + width - 10, yOffset + 1, SpellEditorColors.BORDER_DARK);
        yOffset += 10;

        // 7. 渲染术法摘要
        yOffset = renderSection(graphics, font, "§e【术法摘要】", yOffset);
        yOffset += 2;

        double spiritCost = calculateSpiritCost();
        yOffset = renderItem(graphics, font, "灵力消耗", String.format("%.1f", spiritCost), yOffset);

        double cooldown = viewModel.getCooldown();
        yOffset = renderItem(graphics, font, "冷却时间", String.format("%.1fs", cooldown), yOffset);

        double damage = viewModel.getBaseDamage();
        yOffset = renderItem(graphics, font, "基础伤害", String.format("%.1f", damage), yOffset);

        yOffset += 10;

        // 8. 渲染完成度进度条
        double completion = calculateCompletion();
        int completionPercent = (int) (completion * 100);

        graphics.drawString(font, "§7完成度: §f" + completionPercent + "%", getX() + 10, yOffset, SpellEditorColors.TEXT_DARK, false);
        yOffset += 12;

        SpellEditorRenderUtils.renderProgressBar(
            graphics, getX() + 10, yOffset, width - 20, 12, completion
        );
    }

    private int renderSection(GuiGraphics graphics, Font font, String title, int y) {
        graphics.drawString(font, title, getX() + 10, y, SpellEditorColors.TEXT_GOLD, false);
        return y + 12;
    }

    private int renderItem(GuiGraphics graphics, Font font, String label, String value, int y) {
        String text = "§7" + label + ": §f" + value;
        graphics.drawString(font, text, getX() + 15, y, SpellEditorColors.TEXT_DARK, false);
        return y + 11;
    }

    private String getSourceName() {
        if (viewModel.getSource() == null) {
            return "§c未选择";
        }
        return viewModel.getSource().label;
    }

    private String getCarrierName() {
        if (viewModel.getCarrier() == null) {
            return "§c未选择";
        }
        return viewModel.getCarrier().label;
    }

    private String getFormName() {
        if (viewModel.getForm() == null) {
            return "§c未选择";
        }
        return viewModel.getForm().label;
    }

    private String getAttributesText() {
        int count = viewModel.getAttributes().size();
        if (count == 0) {
            return "§7无";
        }
        return "§f" + count + " 个";
    }

    private String getEffectsText() {
        int count = viewModel.getEffects().size();
        if (count == 0) {
            return "§7无";
        }
        return "§f" + count + " 个";
    }

    private double calculateSpiritCost() {
        // 简化计算：基础10 + 属性数*5 + 效果数*3
        double base = 10.0;
        double attrCost = viewModel.getAttributes().size() * 5.0;
        double effectCost = viewModel.getEffects().size() * 3.0;
        return base + attrCost + effectCost;
    }

    private double calculateCompletion() {
        double total = 0.0;
        double completed = 0.0;

        // 骨架：60%权重
        total += 0.6;
        if (hasValidSkeleton()) {
            completed += 0.6;
        }

        // 属性：20%权重（可选）
        total += 0.2;
        if (viewModel.getAttributes().size() > 0) {
            completed += 0.2;
        }

        // 效果：10%权重（可选）
        total += 0.1;
        if (viewModel.getEffects().size() > 0) {
            completed += 0.1;
        }

        // 命名：10%权重
        total += 0.1;
        if (hasValidName()) {
            completed += 0.1;
        }

        return completed / total;
    }

    private boolean hasValidSkeleton() {
        return viewModel.getSource() != null &&
               viewModel.getCarrier() != null &&
               viewModel.getForm() != null;
    }

    private boolean hasValidName() {
        String name = viewModel.getDisplayName();
        return name != null && !name.isEmpty() && !"未命名术法".equals(name);
    }

    public void updatePreview() {
        // 刷新显示（触发重新渲染）
        // Minecraft会自动调用renderWidget，所以这里不需要额外操作
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        // 预览面板不需要旁白
    }
}
