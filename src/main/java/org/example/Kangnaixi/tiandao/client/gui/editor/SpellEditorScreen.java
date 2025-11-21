package org.example.Kangnaixi.tiandao.client.gui.editor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.client.gui.editor.widget.DaoCardWidget;
import org.example.Kangnaixi.tiandao.client.gui.editor.widget.SpellPreviewPanel;
import org.example.Kangnaixi.tiandao.network.NetworkHandler;
import org.example.Kangnaixi.tiandao.network.packet.SpellEditorLearnPacket;
import org.example.Kangnaixi.tiandao.network.packet.SpellEditorSavePacket;
import org.example.Kangnaixi.tiandao.spell.runtime.Spell;

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
    private static final int BOTTOM_BAR_HEIGHT = 30;
    private static final float LEFT_PANEL_WIDTH_RATIO = 0.3f;

    private final SpellEditorViewModel viewModel;

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
        int topY = 5;

        // 术法名输入（中间）
        int nameWidth = 200;
        int nameX = this.width / 2 - nameWidth / 2;
        nameField = new EditBox(font, nameX, topY + 10, nameWidth, 20, Component.literal("术法名称"));
        nameField.setValue(viewModel.getDisplayName());
        nameField.setResponder(value -> {
            viewModel.setDisplayName(value);
            updatePreview();
        });
        addRenderableWidget(nameField);

        // ID输入（右侧）
        int idWidth = 120;
        int idX = this.width - idWidth - 80;
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

        // 生成ID按钮
        generateIdButton = Button.builder(Component.literal("生成"), b -> {
            String newId = SpellEditorViewModel.generateUniqueId();
            idField.setValue(newId);
            try {
                viewModel.setSpellId(newId);
            } catch (IllegalArgumentException ex) {
                Tiandao.LOGGER.error("生成的ID无效: {}", ex.getMessage());
            }
        }).bounds(idX + idWidth + 5, topY + 10, 50, 20).build();
        addRenderableWidget(generateIdButton);
    }

    private void initLeftPanel() {
        int leftX = 10;
        int leftY = TOP_BAR_HEIGHT + 5;
        int leftWidth = (int)(this.width * LEFT_PANEL_WIDTH_RATIO) - 15;
        int leftHeight = this.height - TOP_BAR_HEIGHT - BOTTOM_BAR_HEIGHT - 10;

        previewPanel = new SpellPreviewPanel(leftX, leftY, leftWidth, leftHeight, viewModel);
        addRenderableWidget(previewPanel);
    }

    private void initRightPanel() {
        int rightX = (int)(this.width * LEFT_PANEL_WIDTH_RATIO) + 5;
        int rightY = TOP_BAR_HEIGHT + 5;
        int rightWidth = this.width - rightX - 10;

        int yOffset = 0;

        // Source区域
        yOffset = createCardSection("起手式 (Source)", rightX, yOffset, rightWidth,
            ComponentDataProvider.getSources(),
            viewModel.getSource() != null ? viewModel.getSource().id : null,
            this::onSourceSelected, true);

        yOffset += 15; // 区域间隙

        // Carrier区域
        yOffset = createCardSection("载体 (Carrier)", rightX, yOffset, rightWidth,
            ComponentDataProvider.getCarriers(),
            viewModel.getCarrier() != null ? viewModel.getCarrier().id : null,
            this::onCarrierSelected, true);

        yOffset += 15;

        // Form区域
        yOffset = createCardSection("术式 (Form)", rightX, yOffset, rightWidth,
            ComponentDataProvider.getForms(),
            viewModel.getForm() != null ? viewModel.getForm().id : null,
            this::onFormSelected, true);

        yOffset += 15;

        // Attributes区域（多选）
        yOffset = createAttributesSection(rightX, yOffset, rightWidth);

        yOffset += 15;

        // Effects区域（多选）
        yOffset = createEffectsSection(rightX, yOffset, rightWidth);

        // 计算内容总高度和最大滚动偏移
        int rightHeight = this.height - TOP_BAR_HEIGHT - BOTTOM_BAR_HEIGHT - 10;
        rightPanelContentHeight = yOffset;
        maxScrollOffset = Math.max(0, rightPanelContentHeight - rightHeight);
    }

    private void initBottomBar() {
        int bottomY = this.height - BOTTOM_BAR_HEIGHT + 5;
        int buttonWidth = 100;
        int spacing = 10;
        int totalWidth = buttonWidth * 3 + spacing * 2;
        int startX = (this.width - totalWidth) / 2;

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
     */
    private int createCardSection(String sectionTitle, int x, int y, int width,
                                  List<ComponentData> components, String selectedId,
                                  java.util.function.Consumer<String> onSelect, boolean singleSelect) {
        // 添加区域标题（相对坐标）
        sectionTitles.add(new SectionTitle(sectionTitle, x, y));

        // 标题高度
        int titleHeight = 20;
        int cardStartY = y + titleHeight;

        // 卡片布局：2列，间距12px（使用DaoTheme尺寸）
        int spacing = DaoTheme.CARD_SPACING;
        int cardWidth = DaoTheme.CARD_WIDTH;
        int cardHeight = DaoTheme.CARD_HEIGHT;

        int col = 0;
        int currentX = x;
        int currentY = cardStartY;

        for (ComponentData data : components) {
            boolean isSelected = selectedId != null && selectedId.equals(data.getId());

            // 考虑滚动偏移计算实际Y坐标
            int rightY = TOP_BAR_HEIGHT + 5;
            int actualY = currentY + rightY - scrollOffset;

            DaoCardWidget card = new DaoCardWidget(currentX, actualY, cardWidth, cardHeight, data);
            card.setSelected(isSelected);
            card.setOnClickCallback(() -> {
                onSelect.accept(data.getId());
                refreshCards();
            });

            addRenderableWidget(card);
            allCards.add(card);

            col++;
            if (col >= 2) { // 2列布局
                col = 0;
                currentX = x;
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

        // 卡片布局：2列，间距12px（使用DaoTheme尺寸）
        int spacing = DaoTheme.CARD_SPACING;
        int cardWidth = DaoTheme.CARD_WIDTH;
        int cardHeight = DaoTheme.CARD_HEIGHT;

        int col = 0;
        int currentX = x;
        int currentY = cardStartY;

        for (SpellEditorViewModel.SpellAttribute attr : allAttrs) {
            boolean isSelected = selectedAttrs.stream().anyMatch(a -> a.id.equals(attr.id));
            boolean canAdd = selectedAttrs.size() < 3;

            // 创建ComponentData
            ComponentData data = ComponentData.create(attr.id, attr.label, "◆", attr.description);

            int rightY = TOP_BAR_HEIGHT + 5;
            int actualY = currentY + rightY - scrollOffset;

            DaoCardWidget card = new DaoCardWidget(currentX, actualY, cardWidth, cardHeight, data);
            card.setSelected(isSelected);
            // Note: Active state is managed by whether the card is added to the widget list
            // If canAdd is false and not selected, we still add it but don't respond to clicks
            card.setOnClickCallback(() -> {
                if (isSelected) {
                    viewModel.removeAttribute(attr.id);
                } else if (canAdd) {
                    viewModel.addAttribute(attr.id);
                }
                refreshCards();
            });

            addRenderableWidget(card);
            allCards.add(card);

            col++;
            if (col >= 2) { // 2列布局
                col = 0;
                currentX = x;
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

        // 卡片布局：2列，间距12px（使用DaoTheme尺寸）
        int spacing = DaoTheme.CARD_SPACING;
        int cardWidth = DaoTheme.CARD_WIDTH;
        int cardHeight = DaoTheme.CARD_HEIGHT;

        int col = 0;
        int currentX = x;
        int currentY = cardStartY;

        for (SpellEditorViewModel.SpellEffect effect : allEffects) {
            boolean isSelected = selectedEffects.stream().anyMatch(e -> e.id.equals(effect.id));
            boolean canAdd = selectedEffects.size() < 4;

            // 创建ComponentData
            ComponentData data = ComponentData.create(effect.id, effect.label, "★", effect.description);

            int rightY = TOP_BAR_HEIGHT + 5;
            int actualY = currentY + rightY - scrollOffset;

            DaoCardWidget card = new DaoCardWidget(currentX, actualY, cardWidth, cardHeight, data);
            card.setSelected(isSelected);
            // Note: Active state is managed by whether the card is added to the widget list
            // If canAdd is false and not selected, we still add it but don't respond to clicks
            card.setOnClickCallback(() -> {
                if (isSelected) {
                    viewModel.removeEffect(effect.id);
                } else if (canAdd) {
                    viewModel.addEffect(effect.id);
                }
                refreshCards();
            });

            addRenderableWidget(card);
            allCards.add(card);

            col++;
            if (col >= 2) { // 2列布局
                col = 0;
                currentX = x;
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
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // 只在右侧面板区域响应滚动
        int rightX = (int)(this.width * LEFT_PANEL_WIDTH_RATIO) + 5;
        if (mouseX >= rightX) {
            // 向上滚动为正值，向下滚动为负值
            // 每次滚动移动20像素
            int scrollAmount = (int)(delta * 20);
            scrollOffset = Math.max(0, Math.min(maxScrollOffset, scrollOffset - scrollAmount));

            // 刷新卡片位置
            refreshCards();
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    // ==================== 渲染方法 ====================

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 1. 渲染背景渐变（中心亮 → 边缘暗）+ 暗角效果
        DaoTheme.renderGradientBackground(guiGraphics, this.width, this.height);

        // 2. 渲染居中容器 + 双层边框 + 投影
        // 注意：这个容器只是装饰性的，不影响实际组件位置
        // 实际的左右面板仍然使用全屏布局
        // int[] container = DaoTheme.renderCenteredContainer(guiGraphics, this.width, this.height);
        // int panelX = container[0];
        // int panelY = container[1];
        // int panelW = container[2];
        // int panelH = container[3];

        // 3. 计算右侧面板区域（保持原有布局）
        int rightX = (int)(this.width * LEFT_PANEL_WIDTH_RATIO) + 5;
        int rightY = TOP_BAR_HEIGHT + 5;
        int rightWidth = this.width - rightX - 10;
        int rightHeight = this.height - TOP_BAR_HEIGHT - BOTTOM_BAR_HEIGHT - 10;

        // 4. 启用scissor裁剪（右侧滚动区域）
        guiGraphics.enableScissor(rightX, rightY, rightX + rightWidth, rightY + rightHeight);

        // 5. 渲染区域标题
        for (SectionTitle section : sectionTitles) {
            int actualY = section.y + rightY - scrollOffset;
            // 只渲染可见的标题
            if (actualY >= rightY - 20 && actualY <= rightY + rightHeight) {
                guiGraphics.drawString(font, section.title, section.x, actualY, DaoTheme.TEXT_CINNABAR, false);
            }
        }

        // 6. 渲染所有Widget（包括卡片）
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // 7. 禁用scissor裁剪
        guiGraphics.disableScissor();

        // 8. 渲染边缘渐隐遮罩（Fade Mask）- 制造"展开卷轴"效果
        boolean hasScroll = maxScrollOffset > 0;
        if (hasScroll) {
            DaoTheme.renderFadeMask(guiGraphics, rightX, rightY, rightWidth, rightHeight,
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

