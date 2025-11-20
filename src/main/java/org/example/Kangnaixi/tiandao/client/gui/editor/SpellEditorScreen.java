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
    private static final int PADDING = 10;
    private static final int BUTTON_HEIGHT = 20;
    private static final int TAB_HEIGHT = 30;

    private final SpellEditorViewModel viewModel;

    // 当前激活的Tab (0=骨架, 1=属性, 2=效果, 3=命名与预览)
    private int currentTab = 0;

    // Tab按钮
    private Button[] tabButtons = new Button[4];

    // 各Tab的组件容器
    private List<net.minecraft.client.gui.components.Renderable> coreTabWidgets = new ArrayList<>();
    private List<net.minecraft.client.gui.components.Renderable> attrTabWidgets = new ArrayList<>();
    private List<net.minecraft.client.gui.components.Renderable> effectTabWidgets = new ArrayList<>();
    private List<net.minecraft.client.gui.components.Renderable> finalTabWidgets = new ArrayList<>();

    // 命名Tab输入框
    private EditBox nameField;
    private EditBox idField;

    // 预览文本
    private String previewText = "";
    private StringWidget spiritCostLabel;

    public SpellEditorScreen(SpellEditorViewModel viewModel) {
        super(Component.literal("修仙术法编辑器"));
        this.viewModel = viewModel;
    }

    @Override
    protected void init() {
        super.init();
        clearAllTabWidgets();
        initTabButtons();
        switchToTab(currentTab);
        updateSpiritCostLabel();
    }

    /**
     * 初始化顶部Tab切换按钮
     */
    private void initTabButtons() {
        int tabCount = 4;
        int tabWidth = (this.width - 40) / tabCount;
        int tabX = 20;
        int tabY = 10;

        String[] tabLabels = {"①骨架", "②属性", "③效果", "④命名与预览"};

        for (int i = 0; i < tabCount; i++) {
            final int tabIndex = i;
            Button tabBtn = Button.builder(
                Component.literal(tabLabels[i]),
                b -> switchToTab(tabIndex)
            ).bounds(tabX + i * tabWidth, tabY, tabWidth - 5, TAB_HEIGHT).build();
            tabButtons[i] = tabBtn;
            addRenderableWidget(tabBtn);
        }

        updateTabButtonStyles();
    }

    /**
     * 切换到指定Tab
     */
    private void switchToTab(int tabIndex) {
        currentTab = tabIndex;
        clearContentWidgets();

        switch (currentTab) {
            case 0 -> initCoreTab();
            case 1 -> initAttributeTab();
            case 2 -> initEffectTab();
            case 3 -> initFinalTab();
        }

        updateTabButtonStyles();
        updatePreview();
    }

    /**
     * 更新Tab按钮样式（高亮当前Tab）
     */
    private void updateTabButtonStyles() {
        for (int i = 0; i < tabButtons.length; i++) {
            if (tabButtons[i] != null) {
                String[] labels = {"①骨架", "②属性", "③效果", "④命名与预览"};
                String prefix = (i == currentTab) ? "§e§l" : "§7";
                tabButtons[i].setMessage(Component.literal(prefix + labels[i]));
            }
        }
    }

    // ==================== Tab 0: 骨架选择 ====================

    /**
     * 初始化"骨架"Tab
     * 布局：左中右三列，分别是施法源、载体、生效方式
     */
    private void initCoreTab() {
        int contentY = 50;
        int columnWidth = (this.width - 80) / 3;
        int leftX = 20;
        int midX = leftX + columnWidth + 20;
        int rightX = midX + columnWidth + 20;

        // 左列：起手式
        renderCoreColumn("§6§l起手式", leftX, contentY, columnWidth,
            SpellEditorViewModel.getSourceOptions(),
            viewModel.getSource(),
            this::onSourceSelected);

        // 中列：载体
        renderCoreColumn("§6§l载体", midX, contentY, columnWidth,
            SpellEditorViewModel.getCarrierOptions(),
            viewModel.getCarrier(),
            this::onCarrierSelected);

        // 右列：术式
        renderCoreColumn("§6§l术式", rightX, contentY, columnWidth,
            SpellEditorViewModel.getFormOptions(),
            viewModel.getForm(),
            this::onFormSelected);

        // 底部预览区域
        renderCorePreview();

        // 底部导航按钮
        addNavigationButtons(false, true);
    }

    /**
     * 渲染单列骨架选项
     */
    private void renderCoreColumn(String title, int x, int y, int width,
                                 List<SpellEditorViewModel.SpellComponent> options,
                                 SpellEditorViewModel.SpellComponent current,
                                 java.util.function.Consumer<String> onSelect) {
        int buttonY = y + 20;

        for (SpellEditorViewModel.SpellComponent option : options) {
            boolean isSelected = current != null && current.id.equals(option.id);
            String prefix = isSelected ? "§a√" : "§7";

            Button btn = Button.builder(
                Component.literal(prefix + option.label),
                b -> {
                    onSelect.accept(option.id);
                    switchToTab(0); // 刷新当前Tab
                }
            ).bounds(x, buttonY, width, BUTTON_HEIGHT).build();

            addRenderableWidget(btn);
            coreTabWidgets.add(btn);
            buttonY += BUTTON_HEIGHT + 5;
        }
    }

    private void onSourceSelected(String id) {
        viewModel.setSource(id);
        updatePreview();
    }

    private void onCarrierSelected(String id) {
        viewModel.setCarrier(id);
        updatePreview();
    }

    private void onFormSelected(String id) {
        viewModel.setForm(id);
        updatePreview();
    }

    /**
     * 渲染骨架Tab底部预览
     */
    private void renderCorePreview() {
        // 在render()方法中绘制，此处仅占位
    }

    // ==================== Tab 1: 属性选择 ====================

    /**
     * 初始化"属性"Tab
     * 布局：左侧是五行/阴阳/意境按钮，右侧是已选属性列表 + 预览
     */
    private void initAttributeTab() {
        int contentY = 50;
        int leftWidth = (this.width - 60) * 2 / 3;
        int rightWidth = (this.width - 60) / 3;
        int leftX = 20;
        int rightX = leftX + leftWidth + 20;

        // 左侧：五行属性选择器
        int labelY = contentY;
        int buttonY = contentY + 20;

        // 所有属性（现在只有五行）
        List<SpellEditorViewModel.SpellAttribute> allAttrs = SpellEditorViewModel.getAttributeOptions();

        for (SpellEditorViewModel.SpellAttribute attr : allAttrs) {
            renderAttributeButton(attr, leftX, buttonY, 120);
            buttonY += BUTTON_HEIGHT + 5;
        }

        // 右侧：已选属性列表
        renderSelectedAttributesList(rightX, contentY, rightWidth);

        // 导航按钮
        addNavigationButtons(true, true);
    }

    /**
     * 渲染单个属性按钮
     */
    private void renderAttributeButton(SpellEditorViewModel.SpellAttribute attr, int x, int y, int width) {
        List<SpellEditorViewModel.SpellAttribute> current = viewModel.getAttributes();
        boolean isSelected = current.stream().anyMatch(a -> a.id.equals(attr.id));
        boolean canAdd = current.size() < 3;

        String prefix = isSelected ? "§a●" : (canAdd ? "§7" : "§8");

        Button btn = Button.builder(
            Component.literal(prefix + attr.label),
            b -> {
                if (isSelected) {
                    viewModel.removeAttribute(attr.id);
                } else if (canAdd) {
                    viewModel.addAttribute(attr.id);
                }
                switchToTab(1); // 刷新
            }
        ).bounds(x, y, width, BUTTON_HEIGHT).build();

        btn.active = isSelected || canAdd;
        addRenderableWidget(btn);
        attrTabWidgets.add(btn);
    }

    /**
     * 渲染右侧已选属性列表
     */
    private void renderSelectedAttributesList(int x, int y, int width) {
        // 在render()方法中绘制文本
    }

    // ==================== Tab 2: 效果选择 ====================

    /**
     * 初始化"效果"Tab
     * 布局：左侧是效果按钮，右侧是已选效果列表 + 描述
     */
    private void initEffectTab() {
        int contentY = 50;
        int leftWidth = (this.width - 60) * 2 / 3;
        int rightWidth = (this.width - 60) / 3;
        int leftX = 20;
        int rightX = leftX + leftWidth + 20;

        // 左侧：效果选择
        int buttonY = contentY + 20;
        List<SpellEditorViewModel.SpellEffect> allEffects = SpellEditorViewModel.getEffectOptions();

        int col = 0;
        int colWidth = 100;
        int colX = leftX;

        for (SpellEditorViewModel.SpellEffect effect : allEffects) {
            renderEffectButton(effect, colX, buttonY, colWidth);

            col++;
            if (col >= 3) {
                col = 0;
                buttonY += BUTTON_HEIGHT + 5;
                colX = leftX;
            } else {
                colX += colWidth + 10;
            }
        }

        // 右侧：已选效果列表
        renderSelectedEffectsList(rightX, contentY, rightWidth);

        // 导航按钮
        addNavigationButtons(true, true);
    }

    /**
     * 渲染单个效果按钮
     */
    private void renderEffectButton(SpellEditorViewModel.SpellEffect effect, int x, int y, int width) {
        List<SpellEditorViewModel.SpellEffect> current = viewModel.getEffects();
        boolean isSelected = current.stream().anyMatch(e -> e.id.equals(effect.id));
        boolean canAdd = current.size() < 4;

        String prefix = isSelected ? "§b●" : (canAdd ? "§7" : "§8");

        Button btn = Button.builder(
            Component.literal(prefix + effect.label),
            b -> {
                if (isSelected) {
                    viewModel.removeEffect(effect.id);
                } else if (canAdd) {
                    viewModel.addEffect(effect.id);
                }
                switchToTab(2); // 刷新
            }
        ).bounds(x, y, width, BUTTON_HEIGHT).build();

        btn.active = isSelected || canAdd;
        addRenderableWidget(btn);
        effectTabWidgets.add(btn);
    }

    /**
     * 渲染右侧已选效果列表
     */
    private void renderSelectedEffectsList(int x, int y, int width) {
        // 在render()方法中绘制文本
    }

    // ==================== Tab 3: 命名与预览 ====================

    /**
     * 初始化"命名与预览"Tab
     * 布局：上方输入术法ID和名称，中间是完整预览，底部是导出按钮
     */
    private void initFinalTab() {
        int contentY = 50;
        int leftX = 20;
        int fieldWidth = (this.width - 60) / 2 - 10;

        // ID输入
        idField = new EditBox(this.font, leftX, contentY, fieldWidth - 60, BUTTON_HEIGHT, Component.literal("术法ID"));
        // 显示当前ID或提示文本
        String currentId = viewModel.getSpellIdRaw();
        idField.setValue(currentId != null ? currentId : "§7[自动生成]");
        idField.setResponder(value -> {
            // 移除提示文本
            if (value.equals("§7[自动生成]") || value.isBlank()) {
                return;
            }
            try {
                viewModel.setSpellId(value);
                updatePreview();
            } catch (IllegalArgumentException ex) {
                // ID验证失败，显示错误提示（在保存时会再次验证）
                Tiandao.LOGGER.warn("ID验证失败: {}", ex.getMessage());
            }
        });
        addRenderableWidget(idField);
        finalTabWidgets.add(idField);

        // 生成新ID按钮
        Button generateIdBtn = Button.builder(
            Component.literal("§a生成"),
            b -> {
                String newId = SpellEditorViewModel.generateUniqueId();
                idField.setValue(newId);
                try {
                    viewModel.setSpellId(newId);
                    updatePreview();
                } catch (IllegalArgumentException ex) {
                    Tiandao.LOGGER.error("生成的ID无效（不应发生）: {}", ex.getMessage());
                }
            }
        ).bounds(leftX + fieldWidth - 55, contentY, 50, BUTTON_HEIGHT).build();
        addRenderableWidget(generateIdBtn);
        finalTabWidgets.add(generateIdBtn);

        // 名称输入
        nameField = new EditBox(this.font, leftX + fieldWidth + 20, contentY, fieldWidth, BUTTON_HEIGHT, Component.literal("术法名称"));
        nameField.setValue(viewModel.getDisplayName());
        nameField.setResponder(value -> {
            viewModel.setDisplayName(value);
            updatePreview();
        });
        addRenderableWidget(nameField);
        finalTabWidgets.add(nameField);

        // 随机名称按钮
        Button randomNameBtn = Button.builder(
            Component.literal("§d随机名称"),
            b -> generateRandomName()
        ).bounds(leftX + fieldWidth * 2 + 30, contentY, 100, BUTTON_HEIGHT).build();
        addRenderableWidget(randomNameBtn);
        finalTabWidgets.add(randomNameBtn);

        // 描述输入（多行）
        int descY = contentY + 30;
        EditBox descField = new EditBox(this.font, leftX, descY, this.width - 40, BUTTON_HEIGHT, Component.literal("描述"));
        descField.setValue(viewModel.getDescription());
        descField.setResponder(value -> {
            viewModel.setDescription(value);
            updatePreview();
        });
        addRenderableWidget(descField);
        finalTabWidgets.add(descField);

        // 基础数值输入
        int statsY = descY + 40;
        int statWidth = (this.width - 80) / 5;

        EditBox damageField = createNumberField(leftX, statsY, statWidth - 5, viewModel::setBaseDamage, "伤害");
        damageField.setValue(String.format("%.1f", viewModel.getBaseDamage()));
        addRenderableWidget(damageField);
        finalTabWidgets.add(damageField);

        EditBox speedField = createNumberField(leftX + statWidth, statsY, statWidth - 5, viewModel::setSpeed, "速度");
        speedField.setValue(String.format("%.1f", viewModel.getSpeed()));
        addRenderableWidget(speedField);
        finalTabWidgets.add(speedField);

        EditBox rangeField = createNumberField(leftX + statWidth * 2, statsY, statWidth - 5, viewModel::setRange, "范围");
        rangeField.setValue(String.format("%.1f", viewModel.getRange()));
        addRenderableWidget(rangeField);
        finalTabWidgets.add(rangeField);

        EditBox cooldownField = createNumberField(leftX + statWidth * 3, statsY, statWidth - 5, viewModel::setCooldown, "冷却");
        cooldownField.setValue(String.format("%.1f", viewModel.getCooldown()));
        addRenderableWidget(cooldownField);
        finalTabWidgets.add(cooldownField);

        spiritCostLabel = new StringWidget(leftX + statWidth * 4, statsY + 2, statWidth - 5, BUTTON_HEIGHT,
            Component.literal("灵力消耗：--"), this.font);
        addRenderableWidget(spiritCostLabel);
        finalTabWidgets.add(spiritCostLabel);
        updateSpiritCostLabel();

        // 完整预览区域（在render()中绘制）

        // 导出JSON按钮
        Button exportBtn = Button.builder(
            Component.literal("§a导出 JSON"),
            b -> exportDefinition()
        ).bounds(leftX, this.height - 60, 150, BUTTON_HEIGHT).build();
        addRenderableWidget(exportBtn);
        finalTabWidgets.add(exportBtn);

        Button saveBtn = Button.builder(
            Component.literal("§b保存术法"),
            b -> saveSpellToPlayer()
        ).bounds(leftX + 160, this.height - 60, 150, BUTTON_HEIGHT).build();
        addRenderableWidget(saveBtn);
        finalTabWidgets.add(saveBtn);

        // 导航按钮
        addNavigationButtons(true, false);
    }

    /**
     * 生成随机术法名称
     */
    private void generateRandomName() {
        String[] prefixes = {"破", "灭", "焚", "冰", "雷", "风", "土", "金", "木", "水"};
        String[] suffixes = {"术", "法", "诀", "咒", "印", "阵", "符", "劫", "炎", "寒"};

        String prefix = prefixes[(int) (Math.random() * prefixes.length)];
        String suffix = suffixes[(int) (Math.random() * suffixes.length)];
        String randomName = prefix + suffix;

        viewModel.setDisplayName(randomName);
        if (nameField != null) {
            nameField.setValue(randomName);
        }
        updatePreview();
    }

    // ==================== 通用辅助方法 ====================

    /**
     * 添加上一页/下一步按钮
     */
    private void addNavigationButtons(boolean showPrev, boolean showNext) {
        int bottomY = this.height - 35;

        if (showPrev) {
            Button prevBtn = Button.builder(
                Component.literal("§7« 上一页"),
                b -> switchToTab(currentTab - 1)
            ).bounds(20, bottomY, 100, BUTTON_HEIGHT).build();
            addRenderableWidget(prevBtn);
            getCurrentTabWidgets().add(prevBtn);
        }

        if (showNext) {
            Button nextBtn = Button.builder(
                Component.literal("§e下一页 »"),
                b -> switchToTab(currentTab + 1)
            ).bounds(this.width - 120, bottomY, 100, BUTTON_HEIGHT).build();
            addRenderableWidget(nextBtn);
            getCurrentTabWidgets().add(nextBtn);
        }

        // 关闭按钮（所有Tab都有）
        Button closeBtn = Button.builder(
            Component.literal("§c关闭"),
            b -> onClose()
        ).bounds(this.width / 2 - 50, bottomY, 100, BUTTON_HEIGHT).build();
        addRenderableWidget(closeBtn);
        getCurrentTabWidgets().add(closeBtn);
    }

    /**
     * 获取当前Tab的组件列表
     */
    private List<net.minecraft.client.gui.components.Renderable> getCurrentTabWidgets() {
        return switch (currentTab) {
            case 0 -> coreTabWidgets;
            case 1 -> attrTabWidgets;
            case 2 -> effectTabWidgets;
            case 3 -> finalTabWidgets;
            default -> new ArrayList<>();
        };
    }

    /**
     * 清除所有内容区域的组件（保留Tab按钮）
     */
    private void clearContentWidgets() {
        spiritCostLabel = null;
        // 移除所有Tab的组件（不仅仅是当前Tab）
        List<List<net.minecraft.client.gui.components.Renderable>> allTabWidgets = List.of(
            coreTabWidgets, attrTabWidgets, effectTabWidgets, finalTabWidgets
        );

        for (List<net.minecraft.client.gui.components.Renderable> tabWidgets : allTabWidgets) {
            tabWidgets.forEach(widget -> {
                if (widget instanceof net.minecraft.client.gui.components.events.GuiEventListener listener) {
                    this.removeWidget(listener);
                }
            });
            tabWidgets.clear();
        }
    }

    /**
     * 清除所有Tab的组件缓存
     */
    private void clearAllTabWidgets() {
        coreTabWidgets.clear();
        attrTabWidgets.clear();
        effectTabWidgets.clear();
        finalTabWidgets.clear();
    }

    /**
     * 创建数值输入框
     */
    private EditBox createNumberField(int x, int y, int width, java.util.function.DoubleConsumer consumer, String placeholder) {
        EditBox box = new EditBox(this.font, x, y, width, BUTTON_HEIGHT, Component.literal(placeholder));
        box.setResponder(value -> {
            try {
                double val = Double.parseDouble(value);
                consumer.accept(val);
                updatePreview();
            } catch (NumberFormatException ignored) {
            }
        });
        return box;
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
        updateSpiritCostLabel();
    }

    private void updateSpiritCostLabel() {
        if (this.spiritCostLabel != null) {
            double cost = viewModel.getComputedSpiritCost();
            this.spiritCostLabel.setMessage(Component.literal(
                String.format(Locale.ROOT, "灵力消耗：%.1f", cost)
            ));
        }
    }

    // ==================== 渲染方法 ====================

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // 绘制标题
        guiGraphics.drawCenteredString(font, this.title, this.width / 2, 50, 0xFFFFFF);

        // 根据当前Tab渲染不同内容
        switch (currentTab) {
            case 0 -> renderCoreTabContent(guiGraphics);
            case 1 -> renderAttributeTabContent(guiGraphics);
            case 2 -> renderEffectTabContent(guiGraphics);
            case 3 -> renderFinalTabContent(guiGraphics);
        }
    }

    /**
     * 渲染骨架Tab的文本内容
     */
    private void renderCoreTabContent(net.minecraft.client.gui.GuiGraphics guiGraphics) {
        int contentY = 50;
        int columnWidth = (this.width - 80) / 3;
        int leftX = 20;
        int midX = leftX + columnWidth + 20;
        int rightX = midX + columnWidth + 20;

        // 列标题
        guiGraphics.drawString(font, "§6§l起手式", leftX, contentY, 0xFFDD44);
        guiGraphics.drawString(font, "§6§l载体", midX, contentY, 0xFFDD44);
        guiGraphics.drawString(font, "§6§l术式", rightX, contentY, 0xFFDD44);

        // 底部预览
        int previewY = this.height - 120;
        guiGraphics.drawString(font, "§7§l当前预览:", 20, previewY, 0xCCCCCC);
        previewY += 12;

        String sourceLabel = viewModel.getSource() != null ? viewModel.getSource().label : "未选择";
        String carrierLabel = viewModel.getCarrier() != null ? viewModel.getCarrier().label : "未选择";
        String formLabel = viewModel.getForm() != null ? viewModel.getForm().label : "未选择";

        guiGraphics.drawString(font, "§e起手式: §7" + sourceLabel, 20, previewY, 0xFFFFFF);
        guiGraphics.drawString(font, "§e载体: §7" + carrierLabel, 20, previewY + 11, 0xFFFFFF);
        guiGraphics.drawString(font, "§e术式: §7" + formLabel, 20, previewY + 22, 0xFFFFFF);
    }

    /**
     * 渲染属性Tab的文本内容
     */
    private void renderAttributeTabContent(net.minecraft.client.gui.GuiGraphics guiGraphics) {
        int contentY = 50;
        int leftWidth = (this.width - 60) * 2 / 3;
        int rightX = 20 + leftWidth + 20;

        // 左侧标题
        guiGraphics.drawString(font, "§6§l五行属性", 20, contentY, 0xFFDD44);

        // 右侧已选属性列表
        int listY = contentY;
        guiGraphics.drawString(font, "§7§l已选属性(最多3):", rightX, listY, 0xCCCCCC);
        listY += 15;

        List<SpellEditorViewModel.SpellAttribute> attrs = viewModel.getAttributes();
        if (attrs.isEmpty()) {
            guiGraphics.drawString(font, "§8暂无", rightX, listY, 0x888888);
        } else {
            for (SpellEditorViewModel.SpellAttribute attr : attrs) {
                guiGraphics.drawString(font, "§e● " + attr.label + " §7(" + attr.type + ")", rightX, listY, 0xFFDD44);
                listY += 11;
            }
        }

        // 底部预览
        int previewY = this.height - 100;
        guiGraphics.drawString(font, "§7§l预览:", 20, previewY, 0xCCCCCC);
        String[] lines = previewText.split("\n");
        for (int i = 0; i < Math.min(lines.length, 3); i++) {
            if (!lines[i].trim().isEmpty()) {
                guiGraphics.drawString(font, "§7" + lines[i].trim(), 20, previewY + 12 + i * 11, 0xAAAAAA);
            }
        }
    }

    /**
     * 渲染效果Tab的文本内容
     */
    private void renderEffectTabContent(net.minecraft.client.gui.GuiGraphics guiGraphics) {
        int contentY = 50;
        int leftWidth = (this.width - 60) * 2 / 3;
        int rightX = 20 + leftWidth + 20;

        // 左侧标题
        guiGraphics.drawString(font, "§6§l效果选择", 20, contentY, 0xFFDD44);

        // 右侧已选效果列表
        int listY = contentY;
        guiGraphics.drawString(font, "§7§l已选效果(最多4):", rightX, listY, 0xCCCCCC);
        listY += 15;

        List<SpellEditorViewModel.SpellEffect> effects = viewModel.getEffects();
        if (effects.isEmpty()) {
            guiGraphics.drawString(font, "§8暂无", rightX, listY, 0x888888);
        } else {
            for (SpellEditorViewModel.SpellEffect effect : effects) {
                guiGraphics.drawString(font, "§b● " + effect.label, rightX, listY, 0x55FFFF);
                listY += 11;
            }
        }

        // 底部预览
        int previewY = this.height - 100;
        guiGraphics.drawString(font, "§7§l预览:", 20, previewY, 0xCCCCCC);
        String[] lines = previewText.split("\n");
        for (int i = 0; i < Math.min(lines.length, 3); i++) {
            if (!lines[i].trim().isEmpty()) {
                guiGraphics.drawString(font, "§7" + lines[i].trim(), 20, previewY + 12 + i * 11, 0xAAAAAA);
            }
        }
    }

    /**
     * 渲染命名与预览Tab的文本内容
     */
    private void renderFinalTabContent(net.minecraft.client.gui.GuiGraphics guiGraphics) {
        int contentY = 50;

        // 标签
        guiGraphics.drawString(font, "§7术法ID:", 20, contentY - 10, 0xCCCCCC);
        guiGraphics.drawString(font, "§7术法名称:", 20 + (this.width - 60) / 2 + 10, contentY - 10, 0xCCCCCC);

        // 描述标签
        guiGraphics.drawString(font, "§7描述:", 20, contentY + 20, 0xCCCCCC);

        // 数值标签
        int statsY = contentY + 60;
        guiGraphics.drawString(font, "§7伤害:", 20, statsY - 10, 0xCCCCCC);
        int statWidth = (this.width - 80) / 5;
        guiGraphics.drawString(font, "§7速度:", 20 + statWidth, statsY - 10, 0xCCCCCC);
        guiGraphics.drawString(font, "§7范围:", 20 + statWidth * 2, statsY - 10, 0xCCCCCC);
        guiGraphics.drawString(font, "§7冷却:", 20 + statWidth * 3, statsY - 10, 0xCCCCCC);

        // 完整预览
        int previewY = statsY + 40;
        guiGraphics.drawString(font, "§6§l=== 完整预览 ===", 20, previewY, 0xFFDD44);
        previewY += 15;

        // 骨架
        String sourceLabel = viewModel.getSource() != null ? viewModel.getSource().label : "未选择";
        String carrierLabel = viewModel.getCarrier() != null ? viewModel.getCarrier().label : "未选择";
        String formLabel = viewModel.getForm() != null ? viewModel.getForm().label : "未选择";
        guiGraphics.drawString(font, "§e骨架: §7" + sourceLabel + " → " + carrierLabel + " → " + formLabel, 20, previewY, 0xFFFFFF);
        previewY += 11;

        // 属性
        List<SpellEditorViewModel.SpellAttribute> attrs = viewModel.getAttributes();
        String attrStr = attrs.isEmpty() ? "无" : String.join(", ", attrs.stream().map(a -> a.label).toList());
        guiGraphics.drawString(font, "§e属性: §7" + attrStr, 20, previewY, 0xFFFFFF);
        previewY += 11;

        // 效果
        List<SpellEditorViewModel.SpellEffect> effects = viewModel.getEffects();
        String effectStr = effects.isEmpty() ? "无" : String.join(", ", effects.stream().map(e -> e.label).toList());
        guiGraphics.drawString(font, "§b效果: §7" + effectStr, 20, previewY, 0xFFFFFF);
        previewY += 11;

        // 剑修强化提示
        if (viewModel.isSwordQiEnhanced()) {
            previewY += 5;
            String enhanceText = String.format("§e§l【剑修强化激活！】 §7持剑时：伤害×%.1f 速度×%.1f 范围×%.1f",
                viewModel.getCalculatedSwordDamageMultiplier(),
                viewModel.getCalculatedSwordSpeedMultiplier(),
                viewModel.getCalculatedSwordRangeMultiplier());
            guiGraphics.drawString(font, enhanceText, 20, previewY, 0xFFDD00);
        }

        // 描述预览
        previewY += 15;
        String[] lines = previewText.split("\n");
        for (int i = 0; i < Math.min(lines.length, 2); i++) {
            if (!lines[i].trim().isEmpty()) {
                guiGraphics.drawString(font, "§7" + lines[i].trim(), 20, previewY + i * 11, 0xAAAAAA);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

