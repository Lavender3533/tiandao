package org.example.Kangnaixi.tiandao.client.gui.editor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.client.gui.editor.widget.DaoCardWidget;
import org.example.Kangnaixi.tiandao.client.gui.editor.widget.SpellPreviewPanel;
import org.example.Kangnaixi.tiandao.network.NetworkHandler;
import org.example.Kangnaixi.tiandao.network.packet.SpellEditorLearnPacket;
import org.example.Kangnaixi.tiandao.network.packet.SpellEditorSavePacket;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 修仙术法编辑器界面 - 分步式Tab设计
 * 基于HTML原型的四个分页（骨架、属性、效果、命名与预览）
 */
public class SpellEditorScreen extends Screen {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int TOP_BAR_HEIGHT = 40;
    private static final int BOTTOM_BAR_HEIGHT = 24; // 从30降到24
    private static final float LEFT_PANEL_WIDTH_RATIO = 0.26f; // 从0.3改为0.26

    private final SpellEditorViewModel viewModel;

    // 容器坐标（居中容器）
    private int panelX, panelY, panelW, panelH;
    private int leftX, leftY, leftW, leftH;
    private int rightX, rightY, rightW, rightH;

    // 顶部栏组件
    private EditBox nameField;
    private EditBox idField;
    private Button generateIdButton;

    // 左侧预览面板
    private SpellPreviewPanel previewPanel;

    // 右侧所有卡片
    private List<DaoCardWidget> allCards = new ArrayList<>();

    // 区域标题
    private static class SectionTitle {
        String title;
        int x, y;
        SectionTitle(String title, int x, int y) {
            this.title = title;
            this.x = x;
            this.y = y;
        }
    }
    private List<SectionTitle> sectionTitles = new ArrayList<>();

    // 滚动相关
    private int scrollOffset = 0;
    private int maxScrollOffset = 0;
    private int rightPanelContentHeight = 0;

    // 预览文本
    private String previewText = "";

    public SpellEditorScreen(SpellEditorViewModel viewModel) {
        super(Component.literal("修仙术法编辑器"));
        this.viewModel = viewModel;
    }

    @Override
    protected void init() {
        super.init();
        allCards.clear();
        sectionTitles.clear();
        scrollOffset = 0;

        // 计算居中容器坐标（与DaoTheme.renderCenteredContainer一致）
        // 布局：80%~85%屏宽，上方24px，下方72px，最大1100px
        panelW = Math.min((int)(this.width * 0.85), 1100);
        panelX = (this.width - panelW) / 2;
        panelY = 24;
        panelH = this.height - 96; // 上24px + 下72px = 96px

        // 基于容器计算左右栏坐标（左26%，右74%）
        leftX = panelX + 12;
        leftW = (int)(panelW * LEFT_PANEL_WIDTH_RATIO) - 12;
        leftY = panelY + TOP_BAR_HEIGHT + 8;
        leftH = panelH - TOP_BAR_HEIGHT - BOTTOM_BAR_HEIGHT - 16;

        rightX = leftX + leftW + 12;
        rightW = panelW - leftW - 36; // 调整间距
        rightY = panelY + TOP_BAR_HEIGHT + 8;
        rightH = panelH - TOP_BAR_HEIGHT - BOTTOM_BAR_HEIGHT - 16;

        // 初始化顶部栏
        initTopBar();

        // 初始化左侧预览面板
        initLeftPanel();

        // 初始化右侧组件卡片
        initRightPanel();

        // 初始化底部按钮
        initBottomBar();
    }

    private void initTopBar() {
        int topY = panelY + 5;

        // 术法名输入（容器中间）
        int nameWidth = 200;
        int nameX = panelX + (panelW - nameWidth) / 2;
        nameField = new EditBox(font, nameX, topY + 10, nameWidth, 20, Component.literal("术法名称"));
        nameField.setValue(viewModel.getDisplayName());
        nameField.setResponder(value -> {
            viewModel.setDisplayName(value);
            updatePreview();
        });
        addRenderableWidget(nameField);

        // ID输入（容器右侧，留出更多空间给生成按钮）
        int idWidth = 120;
        int buttonWidth = 60; // 从50增加到60，确保"生成"两字能完整显示
        int idX = panelX + panelW - idWidth - buttonWidth - 20; // 调整位置，确保按钮在容器内
        idField = new EditBox(font, idX, topY + 10, idWidth, 20, Component.literal("术法ID"));
        String currentId = viewModel.getSpellIdRaw();
        idField.setValue(currentId != null ? currentId : "");
        idField.setResponder(value -> {
            if (!value.isBlank()) {
                try {
                    viewModel.setSpellId(value);
                } catch (IllegalArgumentException ex) {
                    Tiandao.LOGGER.warn("ID验证失败: {}", ex.getMessage());
                }
            }
        });
        addRenderableWidget(idField);

        // 生成ID按钮（增加宽度，确保可点击）
        generateIdButton = Button.builder(Component.literal("生成"), b -> {
            String newId = SpellEditorViewModel.generateUniqueId();
            Tiandao.LOGGER.info("生成随机ID: {}", newId); // 添加日志
            idField.setValue(newId);
            try {
                viewModel.setSpellId(newId);
                Tiandao.LOGGER.info("ID已设置到ViewModel: {}", newId);
            } catch (IllegalArgumentException ex) {
                Tiandao.LOGGER.error("生成的ID无效: {}", ex.getMessage());
            }
        }).bounds(idX + idWidth + 8, topY + 10, buttonWidth, 20).build();
        addRenderableWidget(generateIdButton);
    }

    private void initLeftPanel() {
        // 使用已计算的容器坐标
        previewPanel = new SpellPreviewPanel(leftX, leftY, leftW, leftH, viewModel);
        addRenderableWidget(previewPanel);
    }

    private void initRightPanel() {
        // 使用已计算的容器坐标
        int yOffset = 0;
        int padding = 12;

        // Source区域
        yOffset = createCardSection("起手式 (Source)", padding, yOffset, rightW,
            ComponentDataProvider.getSources(),
            viewModel.getSource() != null ? viewModel.getSource().id : null,
            this::onSourceSelected, true);

        yOffset += 15; // 区域间隙

        // Carrier区域
        yOffset = createCardSection("载体 (Carrier)", padding, yOffset, rightW,
            ComponentDataProvider.getCarriers(),
            viewModel.getCarrier() != null ? viewModel.getCarrier().id : null,
            this::onCarrierSelected, true);

        yOffset += 15;

        // Form区域
        yOffset = createCardSection("术式 (Form)", padding, yOffset, rightW,
            ComponentDataProvider.getForms(),
            viewModel.getForm() != null ? viewModel.getForm().id : null,
            this::onFormSelected, true);

        yOffset += 15;

        // Attributes区域（多选）
        yOffset = createAttributesSection(padding, yOffset, rightW);

        yOffset += 15;

        // Effects区域（多选）
        yOffset = createEffectsSection(padding, yOffset, rightW);

        // 计算内容总高度和最大滚动偏移
        rightPanelContentHeight = yOffset;
        maxScrollOffset = Math.max(0, rightPanelContentHeight - rightH);
    }

    private void initBottomBar() {
        int bottomY = panelY + panelH - BOTTOM_BAR_HEIGHT + 5;
        int buttonWidth = 100;
        int spacing = 10;
        int totalWidth = buttonWidth * 3 + spacing * 2;
        int startX = panelX + (panelW - totalWidth) / 2;

        // 导出JSON按钮
        Button exportBtn = Button.builder(Component.literal("§a导出JSON"), b -> exportDefinition())
            .bounds(startX, bottomY, buttonWidth, 20).build();
        addRenderableWidget(exportBtn);

        // 保存术法按钮
        Button saveBtn = Button.builder(Component.literal("§b保存术法"), b -> saveSpellToPlayer())
            .bounds(startX + buttonWidth + spacing, bottomY, buttonWidth, 20).build();
        addRenderableWidget(saveBtn);

        // 关闭按钮
        Button closeBtn = Button.builder(Component.literal("§c关闭"), b -> onClose())
            .bounds(startX + (buttonWidth + spacing) * 2, bottomY, buttonWidth, 20).build();
        addRenderableWidget(closeBtn);
    }

    /**
     * 创建单个组件区域的卡片（单选）
     * @param x 相对X坐标（相对于右栏起始）
     * @param y 相对Y坐标（相对于内容起始）
     */
    private int createCardSection(String sectionTitle, int x, int y, int width,
                                  List<ComponentData> components, String selectedId,
                                  java.util.function.Consumer<String> onSelect, boolean singleSelect) {
        // 添加区域标题（相对坐标）
        sectionTitles.add(new SectionTitle(sectionTitle, x, y));

        // 标题高度
        int titleHeight = 20;
        int cardStartY = y + titleHeight;

        // 卡片布局：2列，固定宽度320px，居中对齐
        int spacing = DaoTheme.CARD_SPACING;
        int cardWidth = DaoTheme.CARD_WIDTH; // 固定320px
        int cardHeight = DaoTheme.CARD_HEIGHT;

        // 计算两列总宽度，居中对齐
        int totalCardsWidth = cardWidth * 2 + spacing;
        int startX = (width - totalCardsWidth) / 2; // 居中起始位置
        if (startX < x) startX = x; // 确保不小于最小边距

        int col = 0;
        int currentX = startX; // 使用居中后的起始位置
        int currentY = cardStartY;

        for (ComponentData data : components) {
            boolean isSelected = selectedId != null && selectedId.equals(data.getId());

            // 计算实际屏幕坐标：右栏起始 + 相对位置 - 滚动偏移
            int actualX = rightX + currentX;
            int actualY = rightY + currentY - scrollOffset;

            DaoCardWidget card = new DaoCardWidget(actualX, actualY, cardWidth, cardHeight, data);
            card.setSelected(isSelected);
            card.setOnClickCallback(() -> {
                onSelect.accept(data.getId());
                refreshCards();
            });

            // 不添加到 renderableWidget 列表，手动渲染
            allCards.add(card);

            col++;
            if (col >= 2) { // 2列布局
                col = 0;
                currentX = startX; // 重置到居中起始位置
                currentY += cardHeight + spacing;
            } else {
                currentX += cardWidth + spacing;
            }
        }

        if (col > 0) {
            currentY += cardHeight + spacing;
        }

        return currentY + 10;
    }

    /**
     * 创建Attributes区域（多选）
     */
    private int createAttributesSection(int x, int y, int width) {
        // 添加区域标题
        sectionTitles.add(new SectionTitle("§6§l属性 (Attributes)", x, y));

        int titleHeight = 20;
        int cardStartY = y + titleHeight;

        List<SpellEditorViewModel.SpellAttribute> allAttrs = SpellEditorViewModel.getAttributeOptions();
        List<SpellEditorViewModel.SpellAttribute> selectedAttrs = viewModel.getAttributes();

        // 卡片布局：2列，固定宽度320px，居中对齐
        int spacing = DaoTheme.CARD_SPACING;
        int cardWidth = DaoTheme.CARD_WIDTH; // 固定320px
        int cardHeight = DaoTheme.CARD_HEIGHT;

        // 计算两列总宽度，居中对齐
        int totalCardsWidth = cardWidth * 2 + spacing;
        int startX = (width - totalCardsWidth) / 2;
        if (startX < x) startX = x;

        int col = 0;
        int currentX = startX;
        int currentY = cardStartY;

        for (SpellEditorViewModel.SpellAttribute attr : allAttrs) {
            boolean isSelected = selectedAttrs.stream().anyMatch(a -> a.id.equals(attr.id));
            boolean canAdd = selectedAttrs.size() < 3;

            // 创建ComponentData
            ComponentData data = ComponentData.create(attr.id, attr.label, "◆", attr.description);

            // 计算实际屏幕坐标：右栏起始 + 相对位置 - 滚动偏移
            int actualX = rightX + currentX;
            int actualY = rightY + currentY - scrollOffset;

            DaoCardWidget card = new DaoCardWidget(actualX, actualY, cardWidth, cardHeight, data);
            card.setSelected(isSelected);
            card.setOnClickCallback(() -> {
                if (isSelected) {
                    viewModel.removeAttribute(attr.id);
                } else if (canAdd) {
                    viewModel.addAttribute(attr.id);
                }
                refreshCards();
            });

            // 不添加到 renderableWidget 列表，手动渲染
            allCards.add(card);

            col++;
            if (col >= 2) { // 2列布局
                col = 0;
                currentX = startX;
                currentY += cardHeight + spacing;
            } else {
                currentX += cardWidth + spacing;
            }
        }

        if (col > 0) {
            currentY += cardHeight + spacing;
        }

        return currentY + 10;
    }

    /**
     * 创建Effects区域（多选）
     */
    private int createEffectsSection(int x, int y, int width) {
        // 添加区域标题
        sectionTitles.add(new SectionTitle("§6§l效果 (Effects)", x, y));

        int titleHeight = 20;
        int cardStartY = y + titleHeight;

        List<SpellEditorViewModel.SpellEffect> allEffects = SpellEditorViewModel.getEffectOptions();
        List<SpellEditorViewModel.SpellEffect> selectedEffects = viewModel.getEffects();

        // 卡片布局：2列，固定宽度320px，居中对齐
        int spacing = DaoTheme.CARD_SPACING;
        int cardWidth = DaoTheme.CARD_WIDTH; // 固定320px
        int cardHeight = DaoTheme.CARD_HEIGHT;

        // 计算两列总宽度，居中对齐
        int totalCardsWidth = cardWidth * 2 + spacing;
        int startX = (width - totalCardsWidth) / 2;
        if (startX < x) startX = x;

        int col = 0;
        int currentX = startX;
        int currentY = cardStartY;

        for (SpellEditorViewModel.SpellEffect effect : allEffects) {
            boolean isSelected = selectedEffects.stream().anyMatch(e -> e.id.equals(effect.id));
            boolean canAdd = selectedEffects.size() < 4;

            // 创建ComponentData
            ComponentData data = ComponentData.create(effect.id, effect.label, "★", effect.description);

            // 计算实际屏幕坐标：右栏起始 + 相对位置 - 滚动偏移
            int actualX = rightX + currentX;
            int actualY = rightY + currentY - scrollOffset;

            DaoCardWidget card = new DaoCardWidget(actualX, actualY, cardWidth, cardHeight, data);
            card.setSelected(isSelected);
            card.setOnClickCallback(() -> {
                if (isSelected) {
                    viewModel.removeEffect(effect.id);
                } else if (canAdd) {
                    viewModel.addEffect(effect.id);
                }
                refreshCards();
            });

            // 不添加到 renderableWidget 列表，手动渲染
            allCards.add(card);

            col++;
            if (col >= 2) { // 2列布局
                col = 0;
                currentX = startX;
                currentY += cardHeight + spacing;
            } else {
                currentX += cardWidth + spacing;
            }
        }

        if (col > 0) {
            currentY += cardHeight + spacing;
        }

        return currentY + 10;
    }

    /**
     * 刷新所有卡片（重新初始化）
     */
    private void refreshCards() {
        // 移除所有旧卡片
        allCards.forEach(card -> this.removeWidget(card));
        allCards.clear();
        sectionTitles.clear();

        // 重新初始化右侧面板
        initRightPanel();

        // 更新预览面板
        if (previewPanel != null) {
            previewPanel.updatePreview();
        }

        updatePreview();
    }


    private void onSourceSelected(String id) {
        viewModel.setSource(id);
        updatePreview();
        if (previewPanel != null) {
            previewPanel.updatePreview();
        }
    }

    private void onCarrierSelected(String id) {
        viewModel.setCarrier(id);
        updatePreview();
        if (previewPanel != null) {
            previewPanel.updatePreview();
        }
    }

    private void onFormSelected(String id) {
        viewModel.setForm(id);
        updatePreview();
        if (previewPanel != null) {
            previewPanel.updatePreview();
        }
    }


    /**
     * 导出JSON定义
     */
    private void exportDefinition() {
        if (!viewModel.isValid()) {
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.sendSystemMessage(Component.literal("§c请先完成三段式骨架的选择！"));
            }
            return;
        }

        String json = GSON.toJson(viewModel.toJson());
        NetworkHandler.sendSpellEditorSaveToServer(new SpellEditorSavePacket(viewModel.getSpellId(), json));

        if (minecraft != null && minecraft.player != null) {
            minecraft.player.sendSystemMessage(Component.literal("§a已提交术法学习请求"));
            minecraft.player.sendSystemMessage(Component.literal("§7JSON: " + json));
        }
    }

    private void saveSpellToPlayer() {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        try {
            // 确保有有效的术法ID
            String spellId = viewModel.getSpellId(); // 如果为null会自动生成

            // 验证ID合法性
            SpellEditorViewModel.ValidationResult validationResult =
                SpellEditorViewModel.validateSpellId(spellId);

            if (!validationResult.isValid()) {
                this.minecraft.player.sendSystemMessage(
                    Component.literal("§c术法ID不合法: " + validationResult.getErrorMessage())
                );
                return;
            }

            // 更新ID字段显示（如果是自动生成的）
            if (idField != null && !spellId.equals(idField.getValue())) {
                idField.setValue(spellId);
            }

            // 生成 SpellDefinition 并序列化为 JSON
            org.example.Kangnaixi.tiandao.spell.definition.SpellDefinition definition = viewModel.toRuntimeDefinition();
            String jsonString = definition.toJson().toString();

            // 发送 SpellDefinition JSON 到服务器
            NetworkHandler.sendSpellEditorLearnToServer(new SpellEditorLearnPacket(jsonString));
            this.minecraft.player.sendSystemMessage(
                Component.literal("§e已提交术法学习请求 §7(ID: " + spellId + ")")
            );
        } catch (IllegalStateException ex) {
            this.minecraft.player.sendSystemMessage(Component.literal("§c" + ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            this.minecraft.player.sendSystemMessage(Component.literal("§c" + ex.getMessage()));
        }
    }

    /**
     * 更新预览文本
     */
    private void updatePreview() {
        previewText = viewModel.getPreviewText();
        // Spirit cost is displayed in the preview panel, no separate label needed
    }

    // ==================== 滚动方法 ====================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 先检查是否点击了右栏卡片
        for (DaoCardWidget card : allCards) {
            if (card.isMouseOver(mouseX, mouseY)) {
                card.onClick(mouseX, mouseY);
                return true;
            }
        }

        // 否则调用父类处理（处理其他 widget）
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // 只在右侧面板区域响应滚动（使用容器坐标）
        if (mouseX >= rightX && mouseX <= rightX + rightW &&
            mouseY >= rightY && mouseY <= rightY + rightH) {
            // 向上滚动为正值，向下滚动为负值
            // 每次滚动移动20像素
            int scrollAmount = (int)(delta * 20);
            int oldScrollOffset = scrollOffset;
            scrollOffset = Math.max(0, Math.min(maxScrollOffset, scrollOffset - scrollAmount));

            // 只更新卡片Y坐标，不重建卡片
            int deltaY = oldScrollOffset - scrollOffset;
            if (deltaY != 0) {
                for (DaoCardWidget card : allCards) {
                    card.setY(card.getY() + deltaY);
                }
            }

            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    // ==================== 渲染方法 ====================

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 1. 渲染全屏背景（术法编辑器已禁用，使用程序化背景）
        // guiGraphics.blit(DaoTheme.BG_BASE, 0, 0, 0, 0, this.width, this.height, this.width, this.height);
        guiGraphics.fill(0, 0, this.width, this.height, 0xFF1A1A1A); // 暗色背景

        // 2. 渲染居中容器（九宫格纹理 container_frame.png）
        DaoTheme.renderCenteredContainer(guiGraphics, this.width, this.height);

        // 3. 渲染所有Widget（顶栏、左栏、底栏）- 不包括右栏卡片
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // 4. 启用scissor裁剪，手动渲染右栏内容
        guiGraphics.enableScissor(rightX, rightY, rightX + rightW, rightY + rightH);

        // 5. 渲染区域标题
        for (SectionTitle section : sectionTitles) {
            int actualY = section.y + rightY - scrollOffset;
            int actualX = section.x + rightX;
            // 只渲染可见的标题
            if (actualY >= rightY - 20 && actualY <= rightY + rightH) {
                guiGraphics.drawString(font, section.title, actualX, actualY, DaoTheme.TEXT_CINNABAR, false);
            }
        }

        // 6. 手动渲染右栏卡片
        for (DaoCardWidget card : allCards) {
            // 检查卡片是否在可见区域
            int cardY = card.getY();
            if (cardY + card.getHeight() >= rightY && cardY <= rightY + rightH) {
                card.renderCard(guiGraphics, mouseX, mouseY, partialTick);
            }
        }

        // 7. 禁用scissor裁剪
        guiGraphics.disableScissor();

        // 8. 渲染边缘渐隐遮罩（Fade Mask）- 制造"展开卷轴"效果
        boolean hasScroll = maxScrollOffset > 0;
        if (hasScroll) {
            DaoTheme.renderFadeMask(guiGraphics, rightX, rightY, rightW, rightH,
                scrollOffset > 0,  // 上边缘：当可向上滚动时显示
                scrollOffset < maxScrollOffset  // 下边缘：当可向下滚动时显示
            );
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

