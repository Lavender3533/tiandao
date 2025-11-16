package org.example.Kangnaixi.tiandao.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.cultivation.CultivationRealm;
import org.example.Kangnaixi.tiandao.cultivation.SubRealm;
import org.example.Kangnaixi.tiandao.network.NetworkHandler;
import org.example.Kangnaixi.tiandao.network.packet.SpellBlueprintCreatePacket;
import org.example.Kangnaixi.tiandao.spell.SpellLocalization;
import org.example.Kangnaixi.tiandao.spell.blueprint.SpellBlueprint;
import org.example.Kangnaixi.tiandao.spell.builder.*;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Ars Nouveau 风格法术编辑器
 *
 * 特点:
 * - 书本式羊皮纸背景
 * - 水平序列槽位（Form → Effect → Augments）
 * - 底部组件库可拖拽
 * - 简洁优雅的界面设计
 */
public class ArsNouveauStyleSpellEditorScreen extends Screen {

    // 界面布局常量
    private static final int BOOK_WIDTH = 420;
    private static final int BOOK_HEIGHT = 240;
    private static final int SLOT_SIZE = 32;
    private static final int SLOT_SPACING = 8;
    private static final int LIBRARY_SLOT_SIZE = 28;
    private static final int MAX_AUGMENTS = 4;

    // 颜色主题 - 羊皮纸风格
    private static final int COLOR_PARCHMENT_BG = 0xFFECE5D8;
    private static final int COLOR_PARCHMENT_DARK = 0xFF8B7355;
    private static final int COLOR_FORM_SLOT = 0xFF6BA5E7;      // 蓝色 - Form
    private static final int COLOR_EFFECT_SLOT = 0xFF7BC96F;    // 绿色 - Effect
    private static final int COLOR_AUGMENT_SLOT = 0xFFBA68C8;   // 紫色 - Augment
    private static final int COLOR_SLOT_EMPTY = 0xFFAAAAAA;
    private static final int COLOR_HOVER = 0xFFFFD700;

    // 组件库
    private final List<FormDefinition> forms = new ArrayList<>();
    private final List<EffectDefinition> effects = new ArrayList<>();
    private final List<AugmentDefinition> augments = new ArrayList<>();

    // 当前选择
    @Nullable
    private FormDefinition selectedForm;
    @Nullable
    private EffectDefinition selectedEffect;
    private final List<AugmentSelection> selectedAugments = new ArrayList<>();

    // Form参数
    private double formRadius;
    private double formDistance;
    private double formAngle;
    private String currentTargeting = "SELF";

    // 玩家境界
    private CultivationRealm playerRealm = CultivationRealm.QI_CONDENSATION;
    private int playerSubRealmLevel = 0;

    // 槽位
    private final List<ComponentSlot> sequenceSlots = new ArrayList<>(); // 上方序列槽位
    private final List<ComponentSlot> librarySlots = new ArrayList<>();  // 下方组件库

    // 拖拽状态
    @Nullable
    private DragState dragState;

    // UI组件
    private EditBox nameBox;
    private EditBox descriptionBox;
    private Button createButton;
    private Button closeButton;
    private Button clearButton;

    // 预览结果
    @Nullable
    private SpellComponentAssembler.Result previewResult;

    // 工具提示
    private final List<Component> tooltip = new ArrayList<>();
    private int tooltipX, tooltipY;

    // 滚动偏移
    private int libraryScrollOffset = 0;

    public ArsNouveauStyleSpellEditorScreen() {
        super(Component.literal("§6术法构筑台"));

        SpellComponentLibrary.init();
        forms.addAll(SpellComponentLibrary.getForms());
        effects.addAll(SpellComponentLibrary.getEffects());
        augments.addAll(SpellComponentLibrary.getAugments());

        forms.sort(Comparator.comparing(FormDefinition::getDisplayName, String.CASE_INSENSITIVE_ORDER));
        effects.sort(Comparator.comparing(EffectDefinition::getDisplayName, String.CASE_INSENSITIVE_ORDER));
        augments.sort(Comparator.comparing(AugmentDefinition::getDisplayName, String.CASE_INSENSITIVE_ORDER));
    }

    @Override
    protected void init() {
        super.init();
        readPlayerRealm();

        int centerX = this.width / 2;
        int bookTop = 40;

        // 顶部输入框
        nameBox = new EditBox(this.font, centerX - 200, 14, 160, 18,
            Component.literal("术法名称"));
        nameBox.setHint(Component.literal("输入名称...").withStyle(ChatFormatting.DARK_GRAY));

        descriptionBox = new EditBox(this.font, centerX - 30, 14, 230, 18,
            Component.literal("术法描述"));
        descriptionBox.setHint(Component.literal("输入描述...").withStyle(ChatFormatting.DARK_GRAY));

        this.addRenderableWidget(nameBox);
        this.addRenderableWidget(descriptionBox);

        // 底部按钮
        int bottomY = this.height - 30;
        clearButton = Button.builder(Component.literal("清空"),
            btn -> clearSelection())
            .bounds(centerX - 120, bottomY, 50, 20).build();

        createButton = Button.builder(Component.literal("§6炼制"),
            btn -> createJadeSlip())
            .bounds(centerX - 60, bottomY, 60, 20).build();

        closeButton = Button.builder(Component.literal("关闭"),
            btn -> onClose())
            .bounds(centerX + 10, bottomY, 50, 20).build();

        this.addRenderableWidget(clearButton);
        this.addRenderableWidget(createButton);
        this.addRenderableWidget(closeButton);

        initializeSlots();
    }

    private void initializeSlots() {
        sequenceSlots.clear();
        librarySlots.clear();

        int centerX = this.width / 2;
        int sequenceY = 80;

        // 序列槽位: Form → Effect → Aug1 → Aug2 → Aug3 → Aug4
        int sequenceX = centerX - (SLOT_SIZE + SLOT_SPACING) * 3;

        // Form槽位
        sequenceSlots.add(new ComponentSlot(
            sequenceX, sequenceY, SLOT_SIZE, SLOT_SIZE,
            ComponentType.FORM, null
        ));
        sequenceX += SLOT_SIZE + SLOT_SPACING;

        // Effect槽位
        sequenceSlots.add(new ComponentSlot(
            sequenceX, sequenceY, SLOT_SIZE, SLOT_SIZE,
            ComponentType.EFFECT, null
        ));
        sequenceX += SLOT_SIZE + SLOT_SPACING;

        // Augment槽位 x4
        for (int i = 0; i < MAX_AUGMENTS; i++) {
            sequenceSlots.add(new ComponentSlot(
                sequenceX, sequenceY, SLOT_SIZE, SLOT_SIZE,
                ComponentType.AUGMENT, null
            ));
            sequenceX += SLOT_SIZE + SLOT_SPACING;
        }

        // 组件库（下方三行）
        int libraryY = 160;
        int libraryStartX = centerX - (LIBRARY_SLOT_SIZE + 4) * 6;

        // 第一行: Forms
        int libX = libraryStartX;
        for (int i = 0; i < Math.min(forms.size(), 12); i++) {
            if (i == 6) {
                libX = libraryStartX;
                libraryY += LIBRARY_SLOT_SIZE + 4;
            }
            librarySlots.add(new ComponentSlot(
                libX, libraryY, LIBRARY_SLOT_SIZE, LIBRARY_SLOT_SIZE,
                ComponentType.FORM, forms.get(i)
            ));
            libX += LIBRARY_SLOT_SIZE + 4;
        }

        libraryY += LIBRARY_SLOT_SIZE + 12;

        // 第二行: Effects
        libX = libraryStartX;
        for (int i = 0; i < Math.min(effects.size(), 12); i++) {
            if (i == 6) {
                libX = libraryStartX;
                libraryY += LIBRARY_SLOT_SIZE + 4;
            }
            librarySlots.add(new ComponentSlot(
                libX, libraryY, LIBRARY_SLOT_SIZE, LIBRARY_SLOT_SIZE,
                ComponentType.EFFECT, effects.get(i)
            ));
            libX += LIBRARY_SLOT_SIZE + 4;
        }

        libraryY += LIBRARY_SLOT_SIZE + 12;

        // 第三行: Augments
        libX = libraryStartX;
        for (int i = 0; i < Math.min(augments.size(), 12); i++) {
            if (i == 6) {
                libX = libraryStartX;
                libraryY += LIBRARY_SLOT_SIZE + 4;
            }
            librarySlots.add(new ComponentSlot(
                libX, libraryY, LIBRARY_SLOT_SIZE, LIBRARY_SLOT_SIZE,
                ComponentType.AUGMENT, augments.get(i)
            ));
            libX += LIBRARY_SLOT_SIZE + 4;
        }
    }

    private void clearSelection() {
        selectedForm = null;
        selectedEffect = null;
        selectedAugments.clear();
        for (ComponentSlot slot : sequenceSlots) {
            slot.component = null;
        }
        recalcPreview();
        playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 0.5f);
    }

    private void readPlayerRealm() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        mc.player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cap -> {
            playerRealm = cap.getRealm();
            playerSubRealmLevel = subRealmToLevel(cap.getSubRealm());
        });
    }

    private int subRealmToLevel(SubRealm sub) {
        return switch (sub) {
            case EARLY -> 1;
            case MIDDLE -> 4;
            case LATE -> 7;
        };
    }

    @Override
    public void tick() {
        nameBox.tick();
        descriptionBox.tick();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (button == 0) { // 左键开始拖拽
            // 从组件库拖拽
            for (ComponentSlot slot : librarySlots) {
                if (slot.isMouseOver(mouseX, mouseY) && slot.component != null) {
                    startDrag(slot, mouseX, mouseY);
                    return true;
                }
            }

            // 从序列槽位拖拽（移除）
            for (int i = 0; i < sequenceSlots.size(); i++) {
                ComponentSlot slot = sequenceSlots.get(i);
                if (slot.isMouseOver(mouseX, mouseY) && slot.component != null) {
                    // 右键移除
                    return false;
                }
            }
        } else if (button == 1) { // 右键移除
            for (int i = 0; i < sequenceSlots.size(); i++) {
                ComponentSlot slot = sequenceSlots.get(i);
                if (slot.isMouseOver(mouseX, mouseY) && slot.component != null) {
                    removeFromSlot(i);
                    return true;
                }
            }
        }

        return false;
    }

    private void startDrag(ComponentSlot sourceSlot, double mouseX, double mouseY) {
        dragState = new DragState(sourceSlot.component, sourceSlot.type, mouseX, mouseY);
        playSound(net.minecraft.sounds.SoundEvents.ITEM_PICKUP, 0.3f);
    }

    private void removeFromSlot(int slotIndex) {
        ComponentSlot slot = sequenceSlots.get(slotIndex);

        if (slotIndex == 0) { // Form
            selectedForm = null;
        } else if (slotIndex == 1) { // Effect
            selectedEffect = null;
        } else { // Augment
            int augIndex = slotIndex - 2;
            if (augIndex < selectedAugments.size()) {
                selectedAugments.remove(augIndex);
            }
        }

        slot.component = null;
        recalcPreview();
        playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 0.5f);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragState != null) {
            dragState.currentX = mouseX;
            dragState.currentY = mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragState != null && button == 0) {
            handleDrop(mouseX, mouseY);
            dragState = null;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void handleDrop(double mouseX, double mouseY) {
        if (dragState == null) return;

        // 查找目标槽位
        for (int i = 0; i < sequenceSlots.size(); i++) {
            ComponentSlot targetSlot = sequenceSlots.get(i);
            if (targetSlot.isMouseOver(mouseX, mouseY)) {
                // 检查类型匹配
                if (targetSlot.type == dragState.type) {
                    boolean success = placeInSlot(i, dragState.component);
                    if (success) {
                        playSound(net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE, 0.7f);
                    } else {
                        playSound(net.minecraft.sounds.SoundEvents.VILLAGER_NO, 0.5f);
                    }
                    return;
                }
            }
        }

        // 没有找到有效槽位
        playSound(net.minecraft.sounds.SoundEvents.VILLAGER_NO, 0.5f);
    }

    private boolean placeInSlot(int slotIndex, Object component) {
        ComponentSlot slot = sequenceSlots.get(slotIndex);

        if (slotIndex == 0) { // Form槽位
            FormDefinition form = (FormDefinition) component;
            if (!isUnlocked(form.getUnlockRealm(), form.getMinSubRealmLevel())) {
                return false;
            }
            selectedForm = form;
            slot.component = form;
            resetFormParameters(form);

        } else if (slotIndex == 1) { // Effect槽位
            EffectDefinition effect = (EffectDefinition) component;
            if (!isEffectAllowed(effect)) {
                return false;
            }
            selectedEffect = effect;
            slot.component = effect;

        } else { // Augment槽位
            AugmentDefinition aug = (AugmentDefinition) component;
            if (!canUseAugment(aug)) {
                return false;
            }

            int augIndex = slotIndex - 2;
            if (augIndex < selectedAugments.size()) {
                selectedAugments.set(augIndex, new AugmentSelection(aug));
            } else {
                selectedAugments.add(new AugmentSelection(aug));
            }
            slot.component = aug;
        }

        recalcPreview();
        return true;
    }

    private void resetFormParameters(FormDefinition form) {
        formRadius = clamp(form.getBaseRadius(), form.getMinRadius(), form.getMaxRadius());
        formDistance = clamp(form.getBaseDistance(), form.getMinDistance(), form.getMaxDistance());
        formAngle = clamp(form.getBaseAngle(), form.getMinAngle(), form.getMaxAngle());
        List<String> options = form.getTargetingOptions();
        currentTargeting = options.isEmpty() ? form.getTargeting() : options.get(0);
    }

    private boolean canUseAugment(AugmentDefinition augment) {
        if (selectedForm == null || selectedEffect == null) return false;
        if (!augment.getAllowedForms().isEmpty()
            && !augment.getAllowedForms().contains(selectedForm.getId())) {
            return false;
        }
        if (!augment.getAllowedEffects().isEmpty()
            && !augment.getAllowedEffects().contains(selectedEffect.getId())) {
            return false;
        }
        return true;
    }

    private boolean isEffectAllowed(EffectDefinition effect) {
        if (selectedForm == null) return true;
        return selectedForm.getAllowedEffects().isEmpty()
            || selectedForm.getAllowedEffects().contains(effect.getId());
    }

    private boolean isUnlocked(CultivationRealm requirement, int level) {
        if (playerRealm.ordinal() > requirement.ordinal()) return true;
        return playerRealm == requirement && playerSubRealmLevel >= level;
    }

    private void recalcPreview() {
        previewResult = null;
        if (selectedForm == null || selectedEffect == null) return;

        List<SpellComponentAssembler.AugmentStack> stacks = selectedAugments.stream()
            .map(sel -> new SpellComponentAssembler.AugmentStack(sel.definition, sel.stacks))
            .toList();

        SpellComponentAssembler.FormParameters params = new SpellComponentAssembler.FormParameters(
            selectedForm.getShape(),
            currentTargeting,
            formRadius,
            formDistance,
            selectedForm.getBaseDurationSeconds(),
            formAngle,
            selectedForm.isMovementLock()
        );

        previewResult = SpellComponentAssembler.assemble(selectedForm, params, selectedEffect, stacks);
    }

    private void createJadeSlip() {
        if (selectedForm == null || selectedEffect == null || previewResult == null) {
            playSound(net.minecraft.sounds.SoundEvents.VILLAGER_NO, 0.5f);
            return;
        }

        String templateId = "custom/" + selectedForm.getId() + "/" + selectedEffect.getId();
        String advancedJson = SpellBlueprint.serializeAdvancedData(previewResult.advancedData());

        List<SpellBlueprintCreatePacket.AugmentPayload> payloads = selectedAugments.stream()
            .map(sel -> new SpellBlueprintCreatePacket.AugmentPayload(sel.definition.getId(), sel.stacks))
            .toList();

        SpellComponentAssembler.FormParameters params = previewResult.parameters();
        SpellBlueprintCreatePacket packet = new SpellBlueprintCreatePacket(
            templateId,
            nameBox.getValue(),
            descriptionBox.getValue(),
            previewResult.basePower(),
            previewResult.spiritCost(),
            previewResult.cooldown(),
            previewResult.range(),
            previewResult.areaRadius(),
            previewResult.elementType(),
            advancedJson,
            selectedForm.getId(),
            selectedEffect.getId(),
            payloads,
            params.radius(),
            params.distance(),
            params.angle(),
            params.targetingType()
        );

        NetworkHandler.sendBlueprintCreateToServer(packet);
        playSound(net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, 0.8f);
        this.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        tooltip.clear();

        int centerX = this.width / 2;

        // 书本背景
        renderBookBackground(graphics, centerX);

        // 标题
        graphics.drawCenteredString(this.font,
            Component.literal("§6§l术法构筑").withStyle(ChatFormatting.BOLD),
            centerX, 50, COLOR_PARCHMENT_DARK);

        // 箭头指示器
        renderArrows(graphics, centerX);

        // 槽位
        renderSequenceSlots(graphics, mouseX, mouseY);
        renderLibrarySlots(graphics, mouseX, mouseY);

        // 统计信息
        renderStats(graphics, centerX);

        // UI组件
        super.render(graphics, mouseX, mouseY, partialTick);

        // 拖拽中
        if (dragState != null) {
            renderDragging(graphics);
        }

        // 工具提示
        if (!tooltip.isEmpty()) {
            graphics.renderComponentTooltip(this.font, tooltip, tooltipX, tooltipY);
        }
    }

    private void renderBookBackground(GuiGraphics graphics, int centerX) {
        int bgX = centerX - BOOK_WIDTH / 2;
        int bgY = 45;
        int bgWidth = BOOK_WIDTH;
        int bgHeight = BOOK_HEIGHT;

        // 羊皮纸背景
        graphics.fill(bgX, bgY, bgX + bgWidth, bgY + bgHeight, 0xEEECE5D8);

        // 边框装饰
        graphics.fill(bgX, bgY, bgX + bgWidth, bgY + 2, COLOR_PARCHMENT_DARK);
        graphics.fill(bgX, bgY + bgHeight - 2, bgX + bgWidth, bgY + bgHeight, COLOR_PARCHMENT_DARK);
        graphics.fill(bgX, bgY, bgX + 2, bgY + bgHeight, COLOR_PARCHMENT_DARK);
        graphics.fill(bgX + bgWidth - 2, bgY, bgX + bgWidth, bgY + bgHeight, COLOR_PARCHMENT_DARK);

        // 装饰线
        int decorY = 72;
        for (int i = 0; i < 3; i++) {
            graphics.fill(bgX + 10, decorY, bgX + bgWidth - 10, decorY + 1, 0x44000000);
            decorY += 1;
        }
    }

    private void renderArrows(GuiGraphics graphics, int centerX) {
        int arrowY = 96;
        int startX = centerX - (SLOT_SIZE + SLOT_SPACING) * 3 + SLOT_SIZE + 2;

        for (int i = 0; i < 5; i++) {
            int ax = startX + i * (SLOT_SIZE + SLOT_SPACING);
            drawArrow(graphics, ax, arrowY, COLOR_PARCHMENT_DARK);
        }
    }

    private void drawArrow(GuiGraphics graphics, int x, int y, int color) {
        // 简单箭头: →
        graphics.fill(x, y, x + 4, y + 1, color);
        graphics.fill(x + 3, y - 1, x + 4, y + 2, color);
    }

    private void renderSequenceSlots(GuiGraphics graphics, int mouseX, int mouseY) {
        for (int i = 0; i < sequenceSlots.size(); i++) {
            ComponentSlot slot = sequenceSlots.get(i);
            boolean hovered = slot.isMouseOver(mouseX, mouseY);

            int bgColor;
            int borderColor;

            if (i == 0) { // Form
                bgColor = slot.component != null ? 0xFF6BA5E7 : 0xFFCCCCCC;
                borderColor = COLOR_FORM_SLOT;
            } else if (i == 1) { // Effect
                bgColor = slot.component != null ? 0xFF7BC96F : 0xFFCCCCCC;
                borderColor = COLOR_EFFECT_SLOT;
            } else { // Augment
                bgColor = slot.component != null ? 0xFFBA68C8 : 0xFFCCCCCC;
                borderColor = COLOR_AUGMENT_SLOT;
            }

            // 槽位背景
            graphics.fill(slot.x, slot.y, slot.x + slot.width, slot.y + slot.height,
                slot.component != null ? (bgColor & 0x55FFFFFF) : 0x44000000);

            // 边框
            int borderW = hovered ? 2 : 1;
            int bColor = hovered ? COLOR_HOVER : borderColor;
            graphics.fill(slot.x, slot.y, slot.x + slot.width, slot.y + borderW, bColor);
            graphics.fill(slot.x, slot.y, slot.x + borderW, slot.y + slot.height, bColor);
            graphics.fill(slot.x + slot.width - borderW, slot.y, slot.x + slot.width, slot.y + slot.height, bColor);
            graphics.fill(slot.x, slot.y + slot.height - borderW, slot.x + slot.width, slot.y + slot.height, bColor);

            // 标签
            if (slot.component == null) {
                String label = i == 0 ? "形" : i == 1 ? "效" : "增";
                graphics.drawCenteredString(this.font, Component.literal(label).withStyle(ChatFormatting.GRAY),
                    slot.x + slot.width / 2, slot.y + slot.height / 2 - 4, 0xFF666666);
            } else {
                String name = getComponentName(slot.component);
                String abbr = name.length() > 2 ? name.substring(0, 2) : name;
                graphics.drawCenteredString(this.font, Component.literal(abbr).withStyle(ChatFormatting.WHITE),
                    slot.x + slot.width / 2, slot.y + slot.height / 2 - 4, 0xFFFFFFFF);
            }

            // 工具提示
            if (hovered && slot.component != null) {
                tooltip.add(Component.literal(getComponentName(slot.component)).withStyle(ChatFormatting.GOLD));
                tooltip.add(Component.literal(getComponentDesc(slot.component)).withStyle(ChatFormatting.GRAY));
                tooltipX = mouseX;
                tooltipY = mouseY;
            }
        }
    }

    private void renderLibrarySlots(GuiGraphics graphics, int mouseX, int mouseY) {
        for (ComponentSlot slot : librarySlots) {
            if (slot.component == null) continue;

            boolean hovered = slot.isMouseOver(mouseX, mouseY);

            int color = switch (slot.type) {
                case FORM -> COLOR_FORM_SLOT;
                case EFFECT -> COLOR_EFFECT_SLOT;
                case AUGMENT -> COLOR_AUGMENT_SLOT;
            };

            int bgColor = hovered ? (color & 0x88FFFFFF) : (color & 0x44FFFFFF);
            graphics.fill(slot.x, slot.y, slot.x + slot.width, slot.y + slot.height, bgColor);

            // 边框
            int borderW = hovered ? 2 : 1;
            graphics.fill(slot.x, slot.y, slot.x + slot.width, slot.y + borderW, color);
            graphics.fill(slot.x, slot.y, slot.x + borderW, slot.y + slot.height, color);
            graphics.fill(slot.x + slot.width - borderW, slot.y, slot.x + slot.width, slot.y + slot.height, color);
            graphics.fill(slot.x, slot.y + slot.height - borderW, slot.x + slot.width, slot.y + slot.height, color);

            // 缩写显示
            String name = getComponentName(slot.component);
            String abbr = name.length() > 1 ? name.substring(0, 1) : name;
            graphics.drawCenteredString(this.font, Component.literal(abbr).withStyle(ChatFormatting.WHITE),
                slot.x + slot.width / 2, slot.y + slot.height / 2 - 3, 0xFFFFFFFF);

            // 工具提示
            if (hovered) {
                tooltip.add(Component.literal(name).withStyle(ChatFormatting.GOLD));
                tooltip.add(Component.literal(getComponentDesc(slot.component)).withStyle(ChatFormatting.GRAY));
                tooltipX = mouseX;
                tooltipY = mouseY;
            }
        }
    }

    private void renderDragging(GuiGraphics graphics) {
        if (dragState == null) return;

        int size = 28;
        int x = (int) dragState.currentX - size / 2;
        int y = (int) dragState.currentY - size / 2;

        int color = switch (dragState.type) {
            case FORM -> COLOR_FORM_SLOT;
            case EFFECT -> COLOR_EFFECT_SLOT;
            case AUGMENT -> COLOR_AUGMENT_SLOT;
        };

        graphics.fill(x, y, x + size, y + size, color & 0xCC000000);

        String name = getComponentName(dragState.component);
        String abbr = name.length() > 2 ? name.substring(0, 2) : name;
        graphics.drawCenteredString(this.font, Component.literal(abbr).withStyle(ChatFormatting.WHITE),
            x + size / 2, y + size / 2 - 4, 0xFFFFFFFF);
    }

    private void renderStats(GuiGraphics graphics, int centerX) {
        if (previewResult == null) {
            graphics.drawCenteredString(this.font,
                Component.literal("§7请配置术式形态与效果"),
                centerX, 130, 0xFF666666);
            return;
        }

        int statsY = 130;
        String stats = String.format(Locale.ROOT,
            "§6威力§r %.1f  §b灵力§r %.1f  §e冷却§r %.1fs",
            previewResult.basePower(), previewResult.spiritCost(), previewResult.cooldown());

        graphics.drawCenteredString(this.font, Component.literal(stats),
            centerX, statsY, 0xFF000000);

        String stats2 = String.format(Locale.ROOT,
            "§a范围§r %.1f  §d复杂度§r %.2f",
            previewResult.range(), previewResult.complexity());

        graphics.drawCenteredString(this.font, Component.literal(stats2),
            centerX, statsY + 12, 0xFF000000);
    }

    private void playSound(net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> soundEvent, float volume) {
        playSound(soundEvent.value(), volume);
    }

    private void playSound(net.minecraft.sounds.SoundEvent soundEvent, float volume) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.playSound(soundEvent, volume, 1.0f);
        }
    }

    private String getComponentName(Object comp) {
        if (comp instanceof FormDefinition f) return f.getDisplayName();
        if (comp instanceof EffectDefinition e) return e.getDisplayName();
        if (comp instanceof AugmentDefinition a) return a.getDisplayName();
        return "未知";
    }

    private String getComponentDesc(Object comp) {
        if (comp instanceof FormDefinition f) return f.getDescription();
        if (comp instanceof EffectDefinition e) return e.getDescription();
        if (comp instanceof AugmentDefinition a) return a.getDescription();
        return "";
    }

    private double clamp(double v, double min, double max) {
        if (max <= min) return max > 0 ? max : min;
        return Math.max(min, Math.min(max, v));
    }

    // ===== 内部类 =====

    private static class ComponentSlot {
        int x, y, width, height;
        ComponentType type;
        @Nullable Object component;

        ComponentSlot(int x, int y, int w, int h, ComponentType type, @Nullable Object comp) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
            this.type = type;
            this.component = comp;
        }

        boolean isMouseOver(double mx, double my) {
            return mx >= x && mx <= x + width && my >= y && my <= y + height;
        }
    }

    private static class DragState {
        Object component;
        ComponentType type;
        double currentX, currentY;

        DragState(Object comp, ComponentType type, double x, double y) {
            this.component = comp;
            this.type = type;
            this.currentX = x;
            this.currentY = y;
        }
    }

    private enum ComponentType {
        FORM, EFFECT, AUGMENT
    }

    private static class AugmentSelection {
        final AugmentDefinition definition;
        int stacks = 1;

        AugmentSelection(AugmentDefinition def) {
            this.definition = def;
        }

        void increment() {
            int max = Math.max(1, definition.getMaxStacks());
            stacks = stacks >= max ? 1 : stacks + 1;
        }
    }
}


