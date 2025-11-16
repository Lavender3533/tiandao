package org.example.Kangnaixi.tiandao.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.example.Kangnaixi.tiandao.spell.rune.Rune;
import org.example.Kangnaixi.tiandao.spell.rune.RuneChainExecutor;
import org.example.Kangnaixi.tiandao.spell.rune.RuneRegistry;
import org.example.Kangnaixi.tiandao.item.SpellJadeSlipItem;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 符文编辑器GUI - 最简原型
 * 功能: 拖拽符文到槽位
 */
public class RuneEditorScreen extends Screen {

    // 布局常量
    private static final int RUNE_LIBRARY_WIDTH = 150;
    private static final int RUNE_SLOT_SIZE = 48;
    private static final int SLOT_SPACING = 8;

    // 符文库
    private final List<Rune> availableRunes = new ArrayList<>();

    // 符文槽位 (最多10个)
    private final RuneSlot[] spellSlots = new RuneSlot[10];

    // 拖拽状态
    @Nullable
    private Rune draggingRune;
    private double dragX, dragY;

    // UI按钮
    private Button clearButton;
    private Button testButton;
    private Button saveButton;  // 新增保存按钮
    private Button closeButton;

    public RuneEditorScreen() {
        super(Component.literal("§6符文构筑台"));

        // 从RuneRegistry加载所有已解锁的符文
        availableRunes.addAll(RuneRegistry.getInstance().getAllRunes());

        // 初始化符文槽位
        for (int i = 0; i < spellSlots.length; i++) {
            spellSlots[i] = new RuneSlot(i);
        }
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;

        // 清空按钮
        clearButton = Button.builder(Component.literal("清空"),
            btn -> clearAllSlots())
            .bounds(centerX - 100, this.height - 35, 60, 24).build();

        // 测试按钮
        testButton = Button.builder(Component.literal("§a测试"),
            btn -> testSpell())
            .bounds(centerX - 30, this.height - 35, 60, 24).build();

        // 保存按钮
        saveButton = Button.builder(Component.literal("§6保存"),
            btn -> saveToJadeSlip())
            .bounds(centerX + 40, this.height - 35, 60, 24).build();

        // 关闭按钮
        closeButton = Button.builder(Component.literal("关闭"),
            btn -> onClose())
            .bounds(centerX + 110, this.height - 35, 60, 24).build();

        this.addRenderableWidget(clearButton);
        this.addRenderableWidget(testButton);
        this.addRenderableWidget(saveButton);
        this.addRenderableWidget(closeButton);

        // 更新槽位位置
        updateSlotPositions();
    }

    private void updateSlotPositions() {
        int startX = RUNE_LIBRARY_WIDTH + 40;
        int startY = 80;

        for (int i = 0; i < spellSlots.length; i++) {
            int row = i / 5;
            int col = i % 5;
            spellSlots[i].x = startX + col * (RUNE_SLOT_SIZE + SLOT_SPACING);
            spellSlots[i].y = startY + row * (RUNE_SLOT_SIZE + SLOT_SPACING);
        }
    }

    private void clearAllSlots() {
        for (RuneSlot slot : spellSlots) {
            slot.rune = null;
        }
    }

    private void testSpell() {
        if (minecraft == null || minecraft.player == null) {
            return;
        }

        // 收集槽位中的符文
        List<Rune> runeChain = new ArrayList<>();
        for (RuneSlot slot : spellSlots) {
            if (slot.rune != null) {
                runeChain.add(slot.rune);
            }
        }

        // 验证符文链
        RuneChainExecutor.ValidationResult validation = RuneChainExecutor.validate(runeChain);
        if (!validation.isValid()) {
            minecraft.player.sendSystemMessage(Component.literal("§c验证失败: " + validation.getErrorMessage()));
            return;
        }

        // 显示符文链信息
        double totalCost = RuneChainExecutor.calculateTotalCost(runeChain);
        double cooldown = RuneChainExecutor.calculateCooldown(runeChain);

        minecraft.player.sendSystemMessage(Component.literal("§6===== 术法测试 ====="));
        minecraft.player.sendSystemMessage(Component.literal("§b符文数量: §f" + runeChain.size()));
        minecraft.player.sendSystemMessage(Component.literal("§b总灵力消耗: §f" + String.format("%.1f", totalCost)));
        minecraft.player.sendSystemMessage(Component.literal("§b冷却时间: §f" + String.format("%.1f", cooldown) + "秒"));

        // 执行符文链
        RuneChainExecutor.ExecutionResult result = RuneChainExecutor.execute(runeChain, minecraft.player);

        if (result.isSuccess()) {
            minecraft.player.sendSystemMessage(Component.literal("§a执行成功！"));
            if (result.getContext() != null) {
                int affected = result.getContext().getAffectedEntities().size();
                minecraft.player.sendSystemMessage(Component.literal("§7影响实体数: " + affected));
            }
        } else {
            minecraft.player.sendSystemMessage(Component.literal("§c执行失败: " + result.getMessage()));
        }
    }

    private void saveToJadeSlip() {
        if (minecraft == null || minecraft.player == null) {
            return;
        }

        // 收集槽位中的符文
        List<Rune> runeChain = new ArrayList<>();
        for (RuneSlot slot : spellSlots) {
            if (slot.rune != null) {
                runeChain.add(slot.rune);
            }
        }

        // 检查是否为空
        if (runeChain.isEmpty()) {
            minecraft.player.sendSystemMessage(Component.literal("§c请先添加符文！"));
            return;
        }

        // 验证符文链
        RuneChainExecutor.ValidationResult validation = RuneChainExecutor.validate(runeChain);
        if (!validation.isValid()) {
            minecraft.player.sendSystemMessage(Component.literal("§c验证失败: " + validation.getErrorMessage()));
            return;
        }

        // 创建玉简
        ItemStack slip = SpellJadeSlipItem.createRuneSlip("自定义术法", runeChain);

        // 给予玩家
        if (!minecraft.player.addItem(slip)) {
            // 如果背包满了，掉落在地上
            minecraft.player.drop(slip, false);
        }

        // 显示成功信息
        double totalCost = RuneChainExecutor.calculateTotalCost(runeChain);
        double cooldown = RuneChainExecutor.calculateCooldown(runeChain);

        minecraft.player.sendSystemMessage(Component.literal("§a§l已创建玉简！"));
        minecraft.player.sendSystemMessage(Component.literal("§b符文数量: §f" + runeChain.size()));
        minecraft.player.sendSystemMessage(Component.literal("§b灵力消耗: §f" + String.format("%.1f", totalCost)));
        minecraft.player.sendSystemMessage(Component.literal("§b冷却时间: §f" + String.format("%.1f", cooldown) + "秒"));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (button == 0) { // 左键
            // 检查是否点击符文库
            for (int i = 0; i < availableRunes.size(); i++) {
                if (isMouseOverRuneLibrary(mouseX, mouseY, i)) {
                    draggingRune = availableRunes.get(i);
                    dragX = mouseX;
                    dragY = mouseY;
                    return true;
                }
            }

            // 检查是否点击符文槽位
            for (RuneSlot slot : spellSlots) {
                if (slot.isMouseOver(mouseX, mouseY) && slot.rune != null) {
                    draggingRune = slot.rune;
                    slot.rune = null;
                    dragX = mouseX;
                    dragY = mouseY;
                    return true;
                }
            }
        } else if (button == 1) { // 右键移除
            for (RuneSlot slot : spellSlots) {
                if (slot.isMouseOver(mouseX, mouseY)) {
                    slot.rune = null;
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingRune != null) {
            this.dragX = mouseX;
            this.dragY = mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingRune != null && button == 0) {
            // 尝试放置到槽位
            for (RuneSlot slot : spellSlots) {
                if (slot.isMouseOver(mouseX, mouseY)) {
                    slot.rune = draggingRune;
                    draggingRune = null;
                    return true;
                }
            }
            draggingRune = null;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        // 标题
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFD700);

        // 符文库背景
        graphics.fill(10, 50, RUNE_LIBRARY_WIDTH + 10, this.height - 50, 0x88000000);
        graphics.drawString(this.font, Component.literal("§b符文库").withStyle(ChatFormatting.BOLD),
            15, 55, 0xFFFFFFFF, false);

        // 绘制符文库
        renderRuneLibrary(graphics, mouseX, mouseY);

        // 符文槽区域背景
        int slotAreaX = RUNE_LIBRARY_WIDTH + 30;
        int slotAreaY = 70;
        int slotAreaW = this.width - slotAreaX - 30;
        int slotAreaH = this.height - slotAreaY - 60;
        graphics.fill(slotAreaX, slotAreaY, slotAreaX + slotAreaW, slotAreaY + slotAreaH, 0x66000000);
        graphics.drawString(this.font, Component.literal("§e符文槽 - 拖拽符文到这里"),
            slotAreaX + 10, slotAreaY + 10, 0xFFFFFFFF, false);

        // 绘制符文槽
        renderRuneSlots(graphics, mouseX, mouseY);

        // 绘制连线
        renderConnections(graphics);

        // UI组件
        super.render(graphics, mouseX, mouseY, partialTick);

        // 拖拽中的符文
        if (draggingRune != null) {
            renderDraggingRune(graphics, dragX, dragY);
        }
    }

    private void renderRuneLibrary(GuiGraphics graphics, int mouseX, int mouseY) {
        int startX = 15;
        int startY = 75;
        int slotSize = 40;
        int spacing = 6;

        for (int i = 0; i < availableRunes.size(); i++) {
            Rune rune = availableRunes.get(i);
            int slotY = startY + i * (slotSize + spacing);

            boolean hovered = isMouseOverRuneLibrary(mouseX, mouseY, i);

            // 背景
            int bgColor = hovered ? 0xAA333333 : 0x88222222;
            graphics.fill(startX, slotY, startX + slotSize, slotY + slotSize, bgColor);

            // 边框
            int borderColor = rune.getColor() | (hovered ? 0xFF000000 : 0x88000000);
            graphics.fill(startX, slotY, startX + slotSize, slotY + 2, borderColor);

            // 符文名称
            graphics.drawString(this.font, Component.literal(rune.getName()),
                startX + 4, slotY + slotSize / 2 - 4, 0xFFFFFFFF, false);

            // 悬停提示
            if (hovered) {
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(Component.literal(rune.getName()).withStyle(ChatFormatting.GOLD));
                tooltip.add(Component.literal(rune.getDescription()).withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.literal(rune.getTier().getDisplayName()).withStyle(ChatFormatting.AQUA));
                graphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
            }
        }
    }

    private void renderRuneSlots(GuiGraphics graphics, int mouseX, int mouseY) {
        for (RuneSlot slot : spellSlots) {
            boolean hovered = slot.isMouseOver(mouseX, mouseY);

            // 背景
            int bgColor = hovered ? 0x88444444 : 0x66333333;
            graphics.fill(slot.x, slot.y, slot.x + RUNE_SLOT_SIZE, slot.y + RUNE_SLOT_SIZE, bgColor);

            // 边框
            int borderColor = hovered ? 0xFFFFFFFF : 0x88888888;
            graphics.fill(slot.x, slot.y, slot.x + RUNE_SLOT_SIZE, slot.y + 2, borderColor);
            graphics.fill(slot.x, slot.y, slot.x + 2, slot.y + RUNE_SLOT_SIZE, borderColor);
            graphics.fill(slot.x + RUNE_SLOT_SIZE - 2, slot.y, slot.x + RUNE_SLOT_SIZE, slot.y + RUNE_SLOT_SIZE, borderColor);
            graphics.fill(slot.x, slot.y + RUNE_SLOT_SIZE - 2, slot.x + RUNE_SLOT_SIZE, slot.y + RUNE_SLOT_SIZE, borderColor);

            // 槽位编号
            graphics.drawString(this.font, String.valueOf(slot.index + 1),
                slot.x + 4, slot.y + 4, 0x88FFFFFF, false);

            // 符文内容
            if (slot.rune != null) {
                // 符文颜色背景
                graphics.fill(slot.x + 4, slot.y + 4, slot.x + RUNE_SLOT_SIZE - 4, slot.y + RUNE_SLOT_SIZE - 4,
                    slot.rune.getColor() & 0x55FFFFFF);

                // 符文名称
                graphics.drawCenteredString(this.font, Component.literal(slot.rune.getName()),
                    slot.x + RUNE_SLOT_SIZE / 2, slot.y + RUNE_SLOT_SIZE / 2 - 4, 0xFFFFFFFF);
            }
        }
    }

    private void renderConnections(GuiGraphics graphics) {
        // 绘制符文之间的连线
        for (int i = 0; i < spellSlots.length - 1; i++) {
            RuneSlot current = spellSlots[i];
            RuneSlot next = spellSlots[i + 1];

            if (current.rune != null && next.rune != null) {
                int x1 = current.x + RUNE_SLOT_SIZE / 2;
                int y1 = current.y + RUNE_SLOT_SIZE / 2;
                int x2 = next.x + RUNE_SLOT_SIZE / 2;
                int y2 = next.y + RUNE_SLOT_SIZE / 2;

                drawLine(graphics, x1, y1, x2, y2, 0xFF4FC3F7);
            }
        }
    }

    private void drawLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        // 简单直线绘制
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;

        int steps = 0;
        while (steps++ < 500) {
            graphics.fill(x1, y1, x1 + 2, y1 + 2, color);

            if (x1 == x2 && y1 == y2) break;

            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x1 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y1 += sy;
            }
        }
    }

    private void renderDraggingRune(GuiGraphics graphics, double x, double y) {
        int size = 44;
        int startX = (int) x - size / 2;
        int startY = (int) y - size / 2;

        // 半透明背景
        graphics.fill(startX, startY, startX + size, startY + size,
            (draggingRune.getColor() & 0x00FFFFFF) | 0xCC000000);

        // 符文名称
        graphics.drawCenteredString(this.font, Component.literal(draggingRune.getName()),
            (int) x, (int) y - 4, 0xFFFFFFFF);
    }

    private boolean isMouseOverRuneLibrary(double mouseX, double mouseY, int index) {
        int startX = 15;
        int startY = 75;
        int slotSize = 40;
        int spacing = 6;
        int slotY = startY + index * (slotSize + spacing);

        return mouseX >= startX && mouseX <= startX + slotSize
            && mouseY >= slotY && mouseY <= slotY + slotSize;
    }

    // 符文槽位内部类
    private static class RuneSlot {
        int index;
        int x, y;
        @Nullable Rune rune;

        RuneSlot(int index) {
            this.index = index;
        }

        boolean isMouseOver(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + RUNE_SLOT_SIZE
                && mouseY >= y && mouseY <= y + RUNE_SLOT_SIZE;
        }
    }
}
