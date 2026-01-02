package org.example.Kangnaixi.tiandao.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.client.gui.editor.DaoTheme;
import org.example.Kangnaixi.tiandao.client.starchart.StarChartClientManager;
import org.example.Kangnaixi.tiandao.handwheel.HandWheelCombination;
import org.example.Kangnaixi.tiandao.handwheel.HandWheelManager;
import org.example.Kangnaixi.tiandao.handwheel.HandWheelSlot;
import org.example.Kangnaixi.tiandao.spell.blueprint.SpellBlueprint;
import org.example.Kangnaixi.tiandao.spell.blueprint.SpellBlueprintLibrary;
import org.example.Kangnaixi.tiandao.starchart.StarNode;

import java.util.List;

/**
 * 手盘组合状态 HUD
 *
 * 功能：
 * - 在星盘开启时显示当前手盘组合信息（右上角）
 * - 始终显示已学习的术法列表（左下角，用于施法选择）
 *
 * 渲染：
 * - 使用务实的分层渲染（3 层：阴影 → 背景 → 内容）
 * - 使用 DaoTheme 主题色
 * - 响应式布局
 *
 * @author Kangnaixi
 */
@Mod.EventBusSubscriber(modid = Tiandao.MOD_ID, value = Dist.CLIENT)
public class HandWheelComboHud {

    // ==================== 布局常量 ====================

    private static final int PADDING = 10;
    private static final int LINE_HEIGHT = 12;
    private static final int PANEL_PADDING = 8;  // 面板内边距

    // ==================== 颜色常量（使用 DaoTheme）====================

    private static final int TITLE_COLOR = DaoTheme.TEXT_GOLD;        // 金色标题
    private static final int LABEL_COLOR = 0xFF88CCFF;                // 浅蓝标签
    private static final int VALUE_COLOR = DaoTheme.TEXT_WHITE;       // 白色值
    private static final int EMPTY_COLOR = DaoTheme.TEXT_SECONDARY;   // 灰色空值
    private static final int SELECTED_COLOR = DaoTheme.ACCENT_JADE;   // 玉绿选中
    private static final int HINT_COLOR = 0xFF888888;                 // 灰色提示

    // 当前选中的术法索引
    private static int selectedSpellIndex = 0;
    
    // 获取/设置当前选中索引
    public static int getSelectedSpellIndex() { return selectedSpellIndex; }
    public static void setSelectedSpellIndex(int index) { selectedSpellIndex = index; }
    
    public static void selectNext() {
        List<SpellBlueprint> spells = new java.util.ArrayList<>(SpellBlueprintLibrary.getAll());
        if (!spells.isEmpty()) {
            selectedSpellIndex = (selectedSpellIndex + 1) % spells.size();
        }
    }
    
    public static void selectPrev() {
        List<SpellBlueprint> spells = new java.util.ArrayList<>(SpellBlueprintLibrary.getAll());
        if (!spells.isEmpty()) {
            selectedSpellIndex = (selectedSpellIndex - 1 + spells.size()) % spells.size();
        }
    }
    
    public static SpellBlueprint getSelectedSpell() {
        List<SpellBlueprint> spells = new java.util.ArrayList<>(SpellBlueprintLibrary.getAll());
        if (spells.isEmpty() || selectedSpellIndex >= spells.size()) {
            return null;
        }
        return spells.get(selectedSpellIndex);
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        // 只在热栏渲染后绘制
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        GuiGraphics gui = event.getGuiGraphics();
        Font font = mc.font;
        int screenWidth = mc.getWindow().getGuiScaledWidth();

        // 星盘开启时显示手盘组合
        StarChartClientManager starChart = StarChartClientManager.getInstance();
        if (starChart.isEnabled()) {
            renderComboPanel(gui, font, screenWidth);
        }

        // 始终显示已学习的术法列表（用于施法选择）
        renderSpellListPanel(gui, font, screenWidth);
    }

    /**
     * 渲染手盘组合面板（右上角）
     * 使用 3 层渲染：阴影 → 背景 → 内容
     */
    private static void renderComboPanel(GuiGraphics gui, Font font, int screenWidth) {
        HandWheelManager handWheel = HandWheelManager.getClientInstance();
        HandWheelCombination combo = handWheel.getCombination();

        // 计算尺寸和位置
        int boxWidth = 180;  // 增加宽度以容纳更长的文本
        int boxHeight = LINE_HEIGHT * 5 + PANEL_PADDING * 2;
        int x = screenWidth - boxWidth - PADDING - 10;
        int y = PADDING;

        // 渲染分层面板
        renderPanel(gui, x, y, boxWidth, boxHeight);

        // 渲染内容
        int contentX = x + PANEL_PADDING;
        int contentY = y + PANEL_PADDING;

        // 标题
        gui.drawString(font, "【手盘组合】", contentX, contentY, TITLE_COLOR, false);
        contentY += LINE_HEIGHT + 4;

        // 效果
        contentY = drawSlotLine(gui, font, contentX, contentY, "效果", combo.getEffectSlot());

        // 形态
        contentY = drawSlotLine(gui, font, contentX, contentY, "形态", combo.getFormSlot());

        // 调制（可能有多个）
        StringBuilder modNames = new StringBuilder();
        for (HandWheelSlot modSlot : combo.getModifierSlots()) {
            if (!modSlot.isEmpty() && modSlot.getNode() != null) {
                if (modNames.length() > 0) modNames.append(", ");
                modNames.append(modSlot.getNode().getName());
            }
        }
        String modValue = modNames.length() > 0 ? modNames.toString() : "空";
        int modColor = modNames.length() > 0 ? VALUE_COLOR : EMPTY_COLOR;
        gui.drawString(font, "调制: ", contentX, contentY, LABEL_COLOR, false);
        gui.drawString(font, modValue, contentX + font.width("调制: "), contentY, modColor, false);
        contentY += LINE_HEIGHT;

        // 提示
        contentY += 4;
        gui.drawString(font, "R=填充 Enter=编译", contentX, contentY, HINT_COLOR, false);
    }

    /**
     * 渲染术法列表面板（左下角）
     * 使用 3 层渲染：阴影 → 背景 → 内容
     */
    private static void renderSpellListPanel(GuiGraphics gui, Font font, int screenWidth) {
        List<SpellBlueprint> spells = new java.util.ArrayList<>(SpellBlueprintLibrary.getAll());

        if (spells.isEmpty()) {
            return; // 没有术法时不显示
        }

        Minecraft mc = Minecraft.getInstance();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // 计算尺寸和位置
        int boxWidth = 150;
        int maxDisplay = Math.min(5, spells.size());
        int boxHeight = LINE_HEIGHT * (maxDisplay + 1) + PANEL_PADDING * 2;
        int x = PADDING;
        int y = screenHeight - boxHeight - 60; // 热栏上方

        // 渲染分层面板
        renderPanel(gui, x, y, boxWidth, boxHeight);

        // 渲染内容
        int contentX = x + PANEL_PADDING;
        int contentY = y + PANEL_PADDING;

        // 标题
        gui.drawString(font, "【已学术法】(↑↓选择 R施放)", contentX, contentY, TITLE_COLOR, false);
        contentY += LINE_HEIGHT + 2;

        // 显示术法列表
        int startIndex = Math.max(0, selectedSpellIndex - 2);
        int endIndex = Math.min(spells.size(), startIndex + maxDisplay);

        for (int i = startIndex; i < endIndex; i++) {
            SpellBlueprint spell = spells.get(i);
            boolean isSelected = (i == selectedSpellIndex);

            String prefix = isSelected ? "▶ " : "  ";
            String name = spell.getName();
            String info = String.format(" (%.0f)", spell.getBasePower());

            int color = isSelected ? SELECTED_COLOR : VALUE_COLOR;
            gui.drawString(font, prefix + name + info, contentX, contentY, color, false);
            contentY += LINE_HEIGHT;
        }

        // 显示总数
        if (spells.size() > maxDisplay) {
            gui.drawString(font, "... 共" + spells.size() + "个术法", contentX, contentY, EMPTY_COLOR, false);
        }
    }

    // ==================== 渲染辅助方法 ====================

    /**
     * 渲染分层面板（3 层：阴影 → 背景 → 边框）
     *
     * @param gui GuiGraphics 对象
     * @param x 左上角 X 坐标
     * @param y 左上角 Y 坐标
     * @param width 面板宽度
     * @param height 面板高度
     */
    private static void renderPanel(GuiGraphics gui, int x, int y, int width, int height) {
        // Layer 1: Shadow (阴影 - 2px 偏移)
        gui.fill(x + 2, y + 2, x + width + 2, y + height + 2, DaoTheme.SHADOW_OUTER);

        // Layer 2: Background (背景渐变 - 羊皮纸)
        gui.fillGradient(x, y, x + width, y + height,
                        DaoTheme.BG_CONTAINER,
                        DaoTheme.BG_PARCHMENT_EDGE);

        // Layer 3: Border (边框 - 深棕色)
        drawRect(gui, x, y, width, height, DaoTheme.BORDER_BROWN, 1);
    }

    /**
     * 绘制矩形边框
     */
    private static void drawRect(GuiGraphics g, int x, int y, int width, int height, int color, int thickness) {
        // 上
        g.fill(x, y, x + width, y + thickness, color);
        // 下
        g.fill(x, y + height - thickness, x + width, y + height, color);
        // 左
        g.fill(x, y, x + thickness, y + height, color);
        // 右
        g.fill(x + width - thickness, y, x + width, y + height, color);
    }

    private static int drawSlotLine(GuiGraphics gui, Font font, int x, int y, String label, HandWheelSlot slot) {
        String value;
        int valueColor;
        
        if (slot.isEmpty() || slot.getNode() == null) {
            value = "空";
            valueColor = EMPTY_COLOR;
        } else {
            StarNode node = slot.getNode();
            value = node.getName();
            valueColor = VALUE_COLOR;
        }

        gui.drawString(font, "§b" + label + ": ", x, y, LABEL_COLOR, false);
        gui.drawString(font, value, x + font.width(label + ": "), y, valueColor, false);
        
        return y + LINE_HEIGHT;
    }
}
