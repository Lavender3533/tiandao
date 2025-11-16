package org.example.Kangnaixi.tiandao.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.capability.ICultivation;
import org.example.Kangnaixi.tiandao.cultivation.CultivationRealm;
import org.example.Kangnaixi.tiandao.cultivation.SubRealm;
import org.example.Kangnaixi.tiandao.network.NetworkHandler;
import org.example.Kangnaixi.tiandao.network.packet.SpellBlueprintCreatePacket;
import org.example.Kangnaixi.tiandao.spell.SpellLocalization;
import org.example.Kangnaixi.tiandao.spell.builder.AugmentDefinition;
import org.example.Kangnaixi.tiandao.spell.builder.EffectDefinition;
import org.example.Kangnaixi.tiandao.spell.builder.FormDefinition;
import org.example.Kangnaixi.tiandao.spell.builder.SpellComponentAssembler;
import org.example.Kangnaixi.tiandao.spell.builder.SpellComponentLibrary;
import org.example.Kangnaixi.tiandao.spell.builder.SpellTemplateDefinition;
import org.example.Kangnaixi.tiandao.spell.blueprint.SpellBlueprint;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 简化版术法构筑器：Form → Effect → Augments 三列展示。
 */
public class SpellBlueprintEditorScreen extends Screen {

    private static final int COLUMN_MARGIN = 32;
    private static final int CARD_HEIGHT = 220;
    private static final int MAX_AUGMENTS = 3;
    private static final int COLUMN_SPACING = 36;
    private static final int CARD_PADDING = 12;

    private final List<FormDefinition> forms = new ArrayList<>();
    private final List<EffectDefinition> effects = new ArrayList<>();
    private final List<AugmentDefinition> augments = new ArrayList<>();
    private final List<SpellTemplateDefinition> templates = new ArrayList<>();

    private FormDefinition selectedForm;
    private EffectDefinition selectedEffect;
    private final List<AugmentSelection> selectedAugments = new ArrayList<>();
    private int templateIndex = -1;
    private SpellTemplateDefinition currentTemplate;
    private int formIndex = -1;
    private int effectIndex = -1;
    private int augmentCandidateIndex = 0;

    private CultivationRealm playerRealm = CultivationRealm.QI_CONDENSATION;
    private int playerSubRealmLevel = 0;

    private EditBox nameBox;
    private EditBox descriptionBox;
    private Button prevTemplateButton;
    private Button nextTemplateButton;
    private Button clearButton;
    private Button createButton;
    private Button closeButton;
    private Button formPrevButton;
    private Button formNextButton;
    private Button effectPrevButton;
    private Button effectNextButton;
    private Button augmentPrevButton;
    private Button augmentNextButton;
    private Button augmentAddButton;

    private final List<CardArea<AugmentSelection>> selectedAugmentAreas = new ArrayList<>();

    private CycleButton<String> targetingCycle;
    private Button radiusMinusButton;
    private Button radiusPlusButton;
    private Button distanceMinusButton;
    private Button distancePlusButton;
    private Button angleMinusButton;
    private Button anglePlusButton;
    private double formRadius;
    private double formDistance;
    private double formAngle;
    private String currentTargeting = "SELF";

    @Nullable
    private SpellComponentAssembler.Result previewResult;
    private Component compatibilityMessage = Component.empty();
    @Nullable
    private DetailInfo hoveredDetail;
    private int compatibilityFlashTicks;
    @Nullable
    private DragType compatibilityFlashTarget;

    public SpellBlueprintEditorScreen() {
        super(SpellLocalization.gui("editor.title"));
        SpellComponentLibrary.init();
        forms.addAll(SpellComponentLibrary.getForms());
        effects.addAll(SpellComponentLibrary.getEffects());
        augments.addAll(SpellComponentLibrary.getAugments());
        templates.addAll(SpellComponentLibrary.getTemplates());
        forms.sort(Comparator.comparing(FormDefinition::getDisplayName, String.CASE_INSENSITIVE_ORDER));
        effects.sort(Comparator.comparing(EffectDefinition::getDisplayName, String.CASE_INSENSITIVE_ORDER));
        augments.sort(Comparator.comparing(AugmentDefinition::getDisplayName, String.CASE_INSENSITIVE_ORDER));
        if (!templates.isEmpty()) {
            templateIndex = 0;
            applyTemplate(templates.get(0));
        } else {
            selectedForm = findFirstUnlockedForm();
            if (selectedForm != null) {
                resetFormParameters(selectedForm);
                formIndex = forms.indexOf(selectedForm);
            } else {
                updateFormControlVisibility();
            }
            selectedEffect = findFirstAllowedEffect(selectedForm);
            effectIndex = selectedEffect == null ? -1 : effects.indexOf(selectedEffect);
            ensureAugmentCandidateValid();
            recalcPreview();
        }
    }

    @Override
    protected void init() {
        super.init();
        readPlayerRealm();
        int top = 20;
        nameBox = new EditBox(this.font, 30, top, 220, 20, SpellLocalization.gui("editor.field.name"));
        descriptionBox = new EditBox(this.font, 260, top, 260, 20, SpellLocalization.gui("editor.field.description"));
        nameBox.setBordered(false);
        descriptionBox.setBordered(false);
        this.addRenderableWidget(nameBox);
        this.addRenderableWidget(descriptionBox);

        prevTemplateButton = Button.builder(SpellLocalization.gui("editor.button.prev_template"), btn -> switchTemplate(-1))
            .bounds(this.width - 320, top, 60, 20).build();
        nextTemplateButton = Button.builder(SpellLocalization.gui("editor.button.next_template"), btn -> switchTemplate(1))
            .bounds(this.width - 250, top, 60, 20).build();
        clearButton = Button.builder(SpellLocalization.gui("editor.button.clear"), btn -> {
            selectedAugments.clear();
            recalcPreview();
            clearCompatibilityState();
        }).bounds(this.width - 180, top, 60, 20).build();

        this.addRenderableWidget(prevTemplateButton);
        this.addRenderableWidget(nextTemplateButton);
        this.addRenderableWidget(clearButton);

        createButton = Button.builder(SpellLocalization.gui("editor.button.create_slip"), btn -> createJadeSlip())
            .bounds(this.width - 200, this.height - 36, 90, 20).build();
        closeButton = Button.builder(SpellLocalization.gui("editor.button.close"), btn -> onClose())
            .bounds(this.width - 100, this.height - 36, 70, 20).build();
        this.addRenderableWidget(createButton);
        this.addRenderableWidget(closeButton);

        initFormControls();

        formPrevButton = Button.builder(Component.literal("<"), btn -> cycleForm(-1)).bounds(0, 0, 18, 20).build();
        formNextButton = Button.builder(Component.literal(">"), btn -> cycleForm(1)).bounds(0, 0, 18, 20).build();
        effectPrevButton = Button.builder(Component.literal("<"), btn -> cycleEffect(-1)).bounds(0, 0, 18, 20).build();
        effectNextButton = Button.builder(Component.literal(">"), btn -> cycleEffect(1)).bounds(0, 0, 18, 20).build();
        augmentPrevButton = Button.builder(Component.literal("<"), btn -> cycleAugmentCandidate(-1)).bounds(0, 0, 18, 20).build();
        augmentNextButton = Button.builder(Component.literal(">"), btn -> cycleAugmentCandidate(1)).bounds(0, 0, 18, 20).build();
        augmentAddButton = Button.builder(Component.literal("+"), btn -> addCurrentAugment()).bounds(0, 0, 22, 20).build();

        this.addRenderableWidget(formPrevButton);
        this.addRenderableWidget(formNextButton);
        this.addRenderableWidget(effectPrevButton);
        this.addRenderableWidget(effectNextButton);
        this.addRenderableWidget(augmentPrevButton);
        this.addRenderableWidget(augmentNextButton);
        this.addRenderableWidget(augmentAddButton);
    }

    private void readPlayerRealm() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        mc.player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cap -> {
            playerRealm = cap.getRealm();
            playerSubRealmLevel = subRealmToLevel(cap.getSubRealm());
        });
        enforceUnlockState();
    }

    private void enforceUnlockState() {
        if (selectedForm != null && !isUnlocked(selectedForm.getUnlockRealm(), selectedForm.getMinSubRealmLevel())) {
            selectedForm = null;
            selectedEffect = null;
            selectedAugments.clear();
            previewResult = null;
            updateFormControlVisibility();
        }
    }

    private void initFormControls() {
        int y = 120;
        rebuildTargetingCycle(List.of("SELF"), y - 30);

        radiusMinusButton = Button.builder(Component.literal("-"), btn -> adjustRadius(-1))
            .bounds(COLUMN_MARGIN, y, 20, 20).build();
        radiusPlusButton = Button.builder(Component.literal("+"), btn -> adjustRadius(1))
            .bounds(COLUMN_MARGIN + 80, y, 20, 20).build();
        distanceMinusButton = Button.builder(Component.literal("-"), btn -> adjustDistance(-1))
            .bounds(COLUMN_MARGIN + 240, y, 20, 20).build();
        distancePlusButton = Button.builder(Component.literal("+"), btn -> adjustDistance(1))
            .bounds(COLUMN_MARGIN + 320, y, 20, 20).build();
        angleMinusButton = Button.builder(Component.literal("-"), btn -> adjustAngle(-5))
            .bounds(COLUMN_MARGIN + 480, y, 20, 20).build();
        anglePlusButton = Button.builder(Component.literal("+"), btn -> adjustAngle(5))
            .bounds(COLUMN_MARGIN + 560, y, 20, 20).build();

        this.addRenderableWidget(radiusMinusButton);
        this.addRenderableWidget(radiusPlusButton);
        this.addRenderableWidget(distanceMinusButton);
        this.addRenderableWidget(distancePlusButton);
        this.addRenderableWidget(angleMinusButton);
        this.addRenderableWidget(anglePlusButton);
        updateFormControlVisibility();
    }

    private void rebuildTargetingCycle(List<String> options, int y) {
        if (targetingCycle != null) {
            this.removeWidget(targetingCycle);
        }
        if (options == null || options.size() <= 1) {
            targetingCycle = null;
            return;
        }
        targetingCycle = CycleButton.<String>builder(this::targetingLabel)
            .withValues(options)
            .displayOnlyValue()
            .withInitialValue(options.get(0))
            .create(this.width / 2 - 60, y, 120, 20, SpellLocalization.gui("editor.targeting.cycle"),
                (btn, value) -> {
                    currentTargeting = value;
                    recalcPreview();
                });
        this.addRenderableWidget(targetingCycle);
    }

    private void updateFormControlVisibility() {
        if (radiusMinusButton == null || radiusPlusButton == null
            || distanceMinusButton == null || distancePlusButton == null
            || angleMinusButton == null || anglePlusButton == null) {
            // UI控件尚未初始化（构造期间调用），直接跳过
            return;
        }
        boolean hasForm = selectedForm != null;
        if (!hasForm) {
            if (targetingCycle != null) {
                targetingCycle.visible = false;
            }
            radiusMinusButton.visible = radiusPlusButton.visible = false;
            distanceMinusButton.visible = distancePlusButton.visible = false;
            angleMinusButton.visible = anglePlusButton.visible = false;
            return;
        }
        List<String> options = selectedForm.getTargetingOptions();
        if (currentTargeting == null || !options.contains(currentTargeting)) {
            currentTargeting = options.get(0);
        }
        rebuildTargetingCycle(options, 90);
        if (targetingCycle != null) {
            targetingCycle.visible = options.size() > 1;
            targetingCycle.setValue(currentTargeting);
        }
        double minRadius = selectedForm.getMinRadius();
        double maxRadius = selectedForm.getMaxRadius();
        double minDistance = selectedForm.getMinDistance();
        double maxDistance = selectedForm.getMaxDistance();
        double minAngle = selectedForm.getMinAngle();
        double maxAngle = selectedForm.getMaxAngle();
        formRadius = clamp(formRadius > 0 ? formRadius : selectedForm.getBaseRadius(), minRadius, maxRadius);
        formDistance = clamp(formDistance > 0 ? formDistance : selectedForm.getBaseDistance(), minDistance, maxDistance);
        formAngle = clamp(formAngle > 0 ? formAngle : selectedForm.getBaseAngle(), minAngle, maxAngle);
        boolean radiusAdjust = maxRadius > minRadius;
        boolean distanceAdjust = maxDistance > minDistance;
        boolean angleAdjust = maxAngle > minAngle;
        radiusMinusButton.visible = radiusPlusButton.visible = radiusAdjust;
        distanceMinusButton.visible = distancePlusButton.visible = distanceAdjust;
        angleMinusButton.visible = anglePlusButton.visible = angleAdjust;
    }

    private void resetFormParameters(FormDefinition form) {
        formRadius = clamp(form.getBaseRadius(), form.getMinRadius(), form.getMaxRadius());
        formDistance = clamp(form.getBaseDistance(), form.getMinDistance(), form.getMaxDistance());
        formAngle = clamp(form.getBaseAngle(), form.getMinAngle(), form.getMaxAngle());
        List<String> options = form.getTargetingOptions();
        currentTargeting = options.isEmpty() ? form.getTargeting() : options.get(0);
        updateFormControlVisibility();
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
        if (compatibilityFlashTicks > 0) {
            compatibilityFlashTicks--;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return handleSelectedAugmentAreaClick(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX, mouseY, button);
    }


    private boolean handleSelectedAugmentAreaClick(double mouseX, double mouseY, int button) {
        for (CardArea<AugmentSelection> area : selectedAugmentAreas) {
            if (area.contains(mouseX, mouseY)) {
                AugmentSelection selection = area.payload();
                if (button == 0) {
                    selection.increment();
                } else if (button == 1) {
                    selectedAugments.remove(selection);
                }
                clearCompatibilityState();
                recalcPreview();
                return true;
            }
        }
        return false;
    }


    private void selectForm(FormDefinition form) {
        if (!isUnlocked(form.getUnlockRealm(), form.getMinSubRealmLevel())) {
            showCompatibilityError(DragType.FORM, SpellLocalization.gui("editor.error.locked_component"));
            return;
        }
        selectedForm = form;
        formIndex = forms.indexOf(form);
        resetFormParameters(form);
        ensureCompatibility();
        ensureAugmentCandidateValid();
        clearCompatibilityState();
        recalcPreview();
    }

    private void selectEffect(EffectDefinition effect) {
        if (!isEffectAllowed(effect)) {
            showCompatibilityError(DragType.EFFECT, SpellLocalization.gui("editor.error_incompatible"));
            return;
        }
        selectedEffect = effect;
        effectIndex = effects.indexOf(effect);
        ensureCompatibility();
        ensureAugmentCandidateValid();
        clearCompatibilityState();
        recalcPreview();
    }

    private void addAugment(AugmentDefinition augment) {
        if (!canUseAugment(augment)) {
            showCompatibilityError(DragType.AUGMENT, SpellLocalization.gui("editor.error_incompatible"));
            return;
        }
        Optional<AugmentSelection> existing = selectedAugments.stream()
            .filter(sel -> sel.definition == augment)
            .findFirst();
        if (existing.isPresent()) {
            existing.get().increment();
        } else {
            if (selectedAugments.size() >= MAX_AUGMENTS) {
                selectedAugments.remove(0);
            }
            selectedAugments.add(new AugmentSelection(augment));
        }
        clearCompatibilityState();
        recalcPreview();
    }

    private boolean canUseAugment(AugmentDefinition augment) {
        if (selectedForm == null || selectedEffect == null) {
            return false;
        }
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
        if (selectedForm == null) {
            return true;
        }
        return selectedForm.getAllowedEffects().isEmpty()
            || selectedForm.getAllowedEffects().contains(effect.getId());
    }

    private boolean isUnlocked(CultivationRealm requirement, int level) {
        if (playerRealm.ordinal() > requirement.ordinal()) {
            return true;
        }
        return playerRealm == requirement && playerSubRealmLevel >= level;
    }

    private boolean isFormUnlocked(FormDefinition form) {
        return isUnlocked(form.getUnlockRealm(), form.getMinSubRealmLevel());
    }

    private void ensureCompatibility() {
        if (selectedEffect != null && !isEffectAllowed(selectedEffect)) {
            selectedEffect = null;
            effectIndex = -1;
        }
        selectedAugments.removeIf(sel -> !canUseAugment(sel.definition));
        if (selectedForm == null) {
            updateFormControlVisibility();
        }
        ensureAugmentCandidateValid();
    }

    private void showCompatibilityError(DragType highlight, Component message) {
        compatibilityMessage = message.copy().withStyle(ChatFormatting.RED);
        compatibilityFlashTarget = highlight;
        compatibilityFlashTicks = 20;
        playDeny();
    }

    private void clearCompatibilityState() {
        compatibilityMessage = Component.empty();
        compatibilityFlashTarget = null;
        compatibilityFlashTicks = 0;
    }

    private boolean shouldHighlightNode(DragType type) {
        return compatibilityFlashTarget == type && compatibilityFlashTicks > 0;
    }

    private void adjustRadius(int direction) {
        if (selectedForm == null) {
            return;
        }
        double step = selectedForm.getRadiusStep();
        formRadius = clamp(formRadius + direction * step, selectedForm.getMinRadius(), selectedForm.getMaxRadius());
        recalcPreview();
    }

    private void adjustDistance(int direction) {
        if (selectedForm == null) {
            return;
        }
        double step = selectedForm.getDistanceStep();
        formDistance = clamp(formDistance + direction * step, selectedForm.getMinDistance(), selectedForm.getMaxDistance());
        recalcPreview();
    }

    private void adjustAngle(int direction) {
        if (selectedForm == null) {
            return;
        }
        double step = selectedForm.getAngleStep();
        formAngle = clamp(formAngle + direction * step, selectedForm.getMinAngle(), selectedForm.getMaxAngle());
        recalcPreview();
    }

    private void switchTemplate(int delta) {
        if (templates.isEmpty()) {
            return;
        }
        templateIndex = (templateIndex + delta + templates.size()) % templates.size();
        applyTemplate(templates.get(templateIndex));
    }

    private void applyTemplate(SpellTemplateDefinition template) {
        currentTemplate = template;
        boolean blocked = false;
        DragType blockedType = DragType.FORM;
        FormDefinition templateForm = findForm(template.getFormId()).orElse(null);
        if (templateForm != null) {
            if (isUnlocked(templateForm.getUnlockRealm(), templateForm.getMinSubRealmLevel())) {
                selectedForm = templateForm;
                formIndex = forms.indexOf(templateForm);
                resetFormParameters(templateForm);
            } else {
                blocked = true;
                blockedType = DragType.FORM;
            }
        } else {
            selectedForm = null;
            updateFormControlVisibility();
        }

        if (!blocked) {
            EffectDefinition templateEffect = findEffect(template.getEffectId()).orElse(null);
            if (templateEffect != null) {
                if (selectedForm == null || isEffectAllowed(templateEffect)) {
                    selectedEffect = templateEffect;
                    effectIndex = effects.indexOf(templateEffect);
                } else {
                    blocked = true;
                    blockedType = DragType.EFFECT;
                }
            } else {
                selectedEffect = null;
            }
        } else {
            selectedEffect = null;
        }

        selectedAugments.clear();
        if (!blocked && selectedForm != null && selectedEffect != null) {
            for (String augmentId : template.getAugmentIds()) {
                SpellComponentLibrary.findAugment(augmentId).ifPresent(def -> {
                    if (canUseAugment(def)) {
                        selectedAugments.add(new AugmentSelection(def));
                    }
                });
            }
        }

        if (blocked) {
            showCompatibilityError(blockedType, SpellLocalization.gui("editor.error.template_locked"));
        } else {
            ensureAugmentCandidateValid();
            clearCompatibilityState();
        }
        recalcPreview();
    }

    private Optional<FormDefinition> findForm(String id) {
        return forms.stream().filter(f -> f.getId().equals(id)).findFirst();
    }

    private Optional<EffectDefinition> findEffect(String id) {
        return effects.stream().filter(e -> e.getId().equals(id)).findFirst();
    }

    private void recalcPreview() {
        previewResult = null;
        if (selectedForm == null || selectedEffect == null) {
            return;
        }
        if (!isEffectAllowed(selectedEffect)) {
            showCompatibilityError(DragType.EFFECT, SpellLocalization.gui("editor.error_incompatible"));
            return;
        }
        List<SpellComponentAssembler.AugmentStack> stacks = selectedAugments.stream()
            .map(sel -> new SpellComponentAssembler.AugmentStack(sel.definition, sel.stacks))
            .toList();
        double radius = clamp(formRadius, selectedForm.getMinRadius(), selectedForm.getMaxRadius());
        double distance = clamp(formDistance, selectedForm.getMinDistance(), selectedForm.getMaxDistance());
        double angle = clamp(formAngle, selectedForm.getMinAngle(), selectedForm.getMaxAngle());
        String targetingMode = currentTargeting != null ? currentTargeting : selectedForm.getTargeting();
        SpellComponentAssembler.FormParameters params = new SpellComponentAssembler.FormParameters(
            selectedForm.getShape(),
            targetingMode,
            radius,
            distance,
            selectedForm.getBaseDurationSeconds(),
            angle,
            selectedForm.isMovementLock()
        );
        previewResult = SpellComponentAssembler.assemble(selectedForm, params, selectedEffect, stacks);
    }

    private void playDeny() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.playSound(net.minecraft.sounds.SoundEvents.VILLAGER_NO, 0.5f, 1.0f);
        }
    }

    private void createJadeSlip() {
        if (selectedForm == null || selectedEffect == null || previewResult == null) {
            DragType missing = selectedForm == null ? DragType.FORM : DragType.EFFECT;
            showCompatibilityError(missing, SpellLocalization.gui("editor.missing_selection"));
            return;
        }
        String templateId = currentTemplate != null ? currentTemplate.getId()
            : "components/" + selectedForm.getId() + "/" + selectedEffect.getId();
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
        this.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawString(this.font, this.title, 30, 8, 0xFFFFFF, false);
        graphics.drawString(this.font, SpellLocalization.gui("editor.field.template")
            .append(": ")
            .append(currentTemplate != null
                ? Component.literal(currentTemplate.getDisplayName())
                : Component.literal("-")), this.width - 330, 8, 0xA0A0A0, false);

        super.render(graphics, mouseX, mouseY, partialTick);

        hoveredDetail = null;
        int cardWidth = (this.width - COLUMN_MARGIN * 2 - COLUMN_SPACING * 2) / 3;
        cardWidth = Math.max(200, cardWidth);
        int cardTop = 70;

        int formX = COLUMN_MARGIN;
        int effectX = formX + cardWidth + COLUMN_SPACING;
        int augmentX = effectX + cardWidth + COLUMN_SPACING;

        renderFormCard(graphics, formX, cardTop, cardWidth, CARD_HEIGHT, mouseX, mouseY);
        renderEffectCard(graphics, effectX, cardTop, cardWidth, CARD_HEIGHT, mouseX, mouseY);
        renderAugmentCard(graphics, augmentX, cardTop, cardWidth, CARD_HEIGHT, mouseX, mouseY);

        layoutNavigationButtons(formX, effectX, augmentX, cardTop, cardWidth);
        layoutParameterControls(cardTop + CARD_HEIGHT + 24, formX, effectX, augmentX, cardWidth);
        renderParameterLabels(graphics);

        int statsTop = cardTop + CARD_HEIGHT + 140;
        renderStats(graphics, statsTop);
        renderDetailPanel(graphics);

        if (!compatibilityMessage.getString().isEmpty()) {
            graphics.drawString(this.font, compatibilityMessage, COLUMN_MARGIN, this.height - 48, 0xFFDD5555, false);
        }
    }

    private void renderFormCard(GuiGraphics graphics,
                                int x,
                                int y,
                                int width,
                                int height,
                                int mouseX,
                                int mouseY) {
        boolean highlight = shouldHighlightNode(DragType.FORM);
        List<Component> lines = new ArrayList<>();
        DetailInfo detail = null;
        if (selectedForm == null) {
            lines.add(Component.literal("等待配置器选择").withStyle(ChatFormatting.GRAY));
            lines.add(Component.literal("使用左右按钮切换形状").withStyle(ChatFormatting.AQUA));
        } else {
            detail = createFormDetail(selectedForm, currentTargeting);
            lines.add(Component.literal(selectedForm.getDescription()).withStyle(ChatFormatting.AQUA));
            lines.add(Component.literal(String.format(Locale.ROOT, "半径 %.1f 格 | 持续 %.1f 秒",
                formRadius, selectedForm.getBaseDurationSeconds())).withStyle(ChatFormatting.WHITE));
            lines.add(SpellLocalization.gui("editor.detail.targeting", targetingLabel(currentTargeting))
                .withStyle(ChatFormatting.WHITE));
            lines.add(Component.literal(String.format(Locale.ROOT, "解锁需求: %s · %d层",
                selectedForm.getUnlockRealm().name(), selectedForm.getMinSubRealmLevel())).withStyle(ChatFormatting.GRAY));
        }
        drawInfoCard(graphics, CardPalette.FORM, x, y, width, height,
            SpellLocalization.gui("editor.column.form"), lines, highlight, mouseX, mouseY, detail);
    }

    private void renderEffectCard(GuiGraphics graphics,
                                  int x,
                                  int y,
                                  int width,
                                  int height,
                                  int mouseX,
                                  int mouseY) {
        boolean highlight = shouldHighlightNode(DragType.EFFECT);
        List<Component> lines = new ArrayList<>();
        DetailInfo detail = null;
        if (selectedEffect == null) {
            lines.add(Component.literal("等待效果灵纹选择").withStyle(ChatFormatting.GRAY));
            lines.add(Component.literal("形状不同将限制可选效果").withStyle(ChatFormatting.AQUA));
        } else {
            detail = createEffectDetail(selectedEffect);
            lines.add(Component.literal(selectedEffect.getDescription()).withStyle(ChatFormatting.AQUA));
            lines.add(Component.literal(String.format(Locale.ROOT, "消耗 %.1f 灵力 | 冷却 %.1f 秒",
                selectedEffect.getBaseSpiritCost(), selectedEffect.getBaseCooldown())).withStyle(ChatFormatting.WHITE));
            if (!selectedEffect.getAllowedForms().isEmpty()) {
                lines.add(Component.literal("允许形状: " + String.join(", ", selectedEffect.getAllowedForms()))
                    .withStyle(ChatFormatting.GRAY));
            }
        }
        drawInfoCard(graphics, CardPalette.EFFECT, x, y, width, height,
            SpellLocalization.gui("editor.column.effect"), lines, highlight, mouseX, mouseY, detail);
    }

    private void renderAugmentCard(GuiGraphics graphics,
                                   int x,
                                   int y,
                                   int width,
                                   int height,
                                   int mouseX,
                                   int mouseY) {
        selectedAugmentAreas.clear();
        boolean highlight = shouldHighlightNode(DragType.AUGMENT);
        List<Component> lines = new ArrayList<>();
        DetailInfo detail = null;
        AugmentDefinition candidate = getCurrentAugmentCandidate();
        if (selectedForm == null || selectedEffect == null) {
            lines.add(Component.literal("请选择形状与效果后配置增幅").withStyle(ChatFormatting.GRAY));
        } else if (candidate == null || !canUseAugment(candidate)) {
            lines.add(Component.literal("暂无可用增幅").withStyle(ChatFormatting.GRAY));
        } else {
            detail = createAugmentDetail(candidate);
            lines.add(Component.literal(candidate.getDescription()).withStyle(ChatFormatting.AQUA));
            if (candidate.getTargetParam() != null && !candidate.getTargetParam().isEmpty()) {
                lines.add(Component.literal("目标: " + candidate.getTargetParam()).withStyle(ChatFormatting.WHITE));
            }
            lines.add(Component.literal(String.format(Locale.ROOT, "复杂度 +%.2f | 灵力系数 +%.0f%%",
                candidate.getComplexityWeight(), candidate.getSpiritCostMultiplier() * 100.0)).withStyle(ChatFormatting.WHITE));
            lines.add(Component.literal("使用 + 按钮加入下方槽位").withStyle(ChatFormatting.GRAY));
        }
        drawInfoCard(graphics, CardPalette.AUGMENT, x, y, width, height,
            SpellLocalization.gui("editor.column.augment"), lines, highlight, mouseX, mouseY, detail);

        int chipX = x + CARD_PADDING;
        int chipY = y + height - 70;
        int chipWidth = width - CARD_PADDING * 2;
        graphics.drawString(this.font, SpellLocalization.gui("editor.augment.selected"), chipX, chipY - 14, 0xFFF8E8FF, false);
        if (selectedAugments.isEmpty()) {
            graphics.drawString(this.font, SpellLocalization.gui("editor.augment.selected_hint"), chipX, chipY + 4, 0x88FFFFFF, false);
            return;
        }
        for (AugmentSelection selection : selectedAugments) {
            int chipHeight = 24;
            drawChip(graphics, chipX, chipY, chipWidth, chipHeight,
                selection.definition.getDisplayName() + " x" + selection.stacks);
            selectedAugmentAreas.add(new CardArea<>(chipX, chipY, chipWidth, chipHeight, selection, true));
            chipY += chipHeight + 4;
        }
    }

    private void layoutNavigationButtons(int formX, int effectX, int augmentX, int cardTop, int cardWidth) {
        int arrowY = cardTop + CARD_HEIGHT / 2 - 10;
        positionButton(formPrevButton, formX - 28, arrowY, unlockedFormCount() > 1);
        positionButton(formNextButton, formX + cardWidth + 10, arrowY, unlockedFormCount() > 1);

        positionButton(effectPrevButton, effectX - 28, arrowY, availableEffectCount() > 1);
        positionButton(effectNextButton, effectX + cardWidth + 10, arrowY, availableEffectCount() > 1);

        positionButton(augmentPrevButton, augmentX - 28, arrowY, availableAugmentCount() > 1);
        positionButton(augmentNextButton, augmentX + cardWidth + 10, arrowY, availableAugmentCount() > 1);

        boolean canAdd = canAddCurrentAugment();
        positionButton(augmentAddButton, augmentX + cardWidth - 26, cardTop + 16, canAdd);
    }

    private void layoutParameterControls(int baseY, int formX, int effectX, int augmentX, int cardWidth) {
        int controlSpacing = 86;
        if (radiusMinusButton != null) {
            positionButton(radiusMinusButton, formX, baseY, radiusMinusButton.visible);
            positionButton(radiusPlusButton, formX + controlSpacing, baseY, radiusPlusButton.visible);
        }
        if (distanceMinusButton != null) {
            positionButton(distanceMinusButton, effectX, baseY, distanceMinusButton.visible);
            positionButton(distancePlusButton, effectX + controlSpacing, baseY, distancePlusButton.visible);
        }
        if (angleMinusButton != null) {
            positionButton(angleMinusButton, augmentX, baseY, angleMinusButton.visible);
            positionButton(anglePlusButton, augmentX + controlSpacing, baseY, anglePlusButton.visible);
        }
        if (targetingCycle != null) {
            targetingCycle.setX(effectX + cardWidth / 2 - targetingCycle.getWidth() / 2);
            targetingCycle.setY(baseY + 26);
        }
    }

    private void positionButton(Button button, int x, int y, boolean visible) {
        if (button == null) {
            return;
        }
        button.visible = visible;
        button.setX(x);
        button.setY(y);
    }

    private int unlockedFormCount() {
        int count = 0;
        for (FormDefinition form : forms) {
            if (isFormUnlocked(form)) {
                count++;
            }
        }
        return count;
    }

    private int availableEffectCount() {
        if (effects.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (EffectDefinition effect : effects) {
            if (isEffectAllowed(effect)) {
                count++;
            }
        }
        return count;
    }

    private int availableAugmentCount() {
        if (selectedForm == null || selectedEffect == null) {
            return 0;
        }
        int count = 0;
        for (AugmentDefinition augment : augments) {
            if (canUseAugment(augment)) {
                count++;
            }
        }
        return count;
    }

    private boolean canAddCurrentAugment() {
        AugmentDefinition candidate = getCurrentAugmentCandidate();
        return candidate != null && selectedForm != null && selectedEffect != null && canUseAugment(candidate);
    }

    @Nullable
    private AugmentDefinition getCurrentAugmentCandidate() {
        if (augments.isEmpty()) {
            return null;
        }
        augmentCandidateIndex = floorMod(augmentCandidateIndex, augments.size());
        return augments.get(augmentCandidateIndex);
    }

    @Nullable
    private FormDefinition findFirstUnlockedForm() {
        for (FormDefinition form : forms) {
            if (isUnlocked(form.getUnlockRealm(), form.getMinSubRealmLevel())) {
                return form;
            }
        }
        return forms.isEmpty() ? null : forms.get(0);
    }

    @Nullable
    private EffectDefinition findFirstAllowedEffect(@Nullable FormDefinition form) {
        for (EffectDefinition effect : effects) {
            if (form == null || form.getAllowedEffects().isEmpty()
                || form.getAllowedEffects().contains(effect.getId())) {
                return effect;
            }
        }
        return effects.isEmpty() ? null : effects.get(0);
    }

    private void cycleForm(int delta) {
        if (forms.isEmpty()) {
            playDeny();
            return;
        }
        int idx = formIndex < 0 ? 0 : formIndex;
        for (int i = 0; i < forms.size(); i++) {
            idx = floorMod(idx + delta, forms.size());
            FormDefinition candidate = forms.get(idx);
            if (isFormUnlocked(candidate)) {
                selectForm(candidate);
                formIndex = idx;
                return;
            }
        }
        playDeny();
    }

    private void cycleEffect(int delta) {
        if (effects.isEmpty()) {
            playDeny();
            return;
        }
        int idx = effectIndex < 0 ? 0 : effectIndex;
        for (int i = 0; i < effects.size(); i++) {
            idx = floorMod(idx + delta, effects.size());
            EffectDefinition candidate = effects.get(idx);
            if (isEffectAllowed(candidate)) {
                selectEffect(candidate);
                effectIndex = idx;
                return;
            }
        }
        playDeny();
    }

    private void cycleAugmentCandidate(int delta) {
        if (augments.isEmpty()) {
            playDeny();
            return;
        }
        int idx = augmentCandidateIndex;
        for (int i = 0; i < augments.size(); i++) {
            idx = floorMod(idx + delta, augments.size());
            AugmentDefinition candidate = augments.get(idx);
            if (selectedForm != null && selectedEffect != null && canUseAugment(candidate)) {
                augmentCandidateIndex = idx;
                return;
            }
        }
        playDeny();
    }

    private void addCurrentAugment() {
        if (selectedForm == null || selectedEffect == null) {
            showCompatibilityError(DragType.AUGMENT, SpellLocalization.gui("editor.missing_selection"));
            return;
        }
        AugmentDefinition candidate = getCurrentAugmentCandidate();
        if (candidate == null || !canUseAugment(candidate)) {
            showCompatibilityError(DragType.AUGMENT, SpellLocalization.gui("editor.error_incompatible"));
            return;
        }
        addAugment(candidate);
    }

    private void ensureAugmentCandidateValid() {
        if (augments.isEmpty()) {
            augmentCandidateIndex = 0;
            return;
        }
        for (int i = 0; i < augments.size(); i++) {
            int idx = (augmentCandidateIndex + i) % augments.size();
            AugmentDefinition candidate = augments.get(idx);
            if (selectedForm == null || selectedEffect == null || canUseAugment(candidate)) {
                augmentCandidateIndex = idx;
                return;
            }
        }
        augmentCandidateIndex = 0;
    }

    private void renderParameterLabels(GuiGraphics graphics) {
        if (selectedForm == null) {
            return;
        }
        int labelY = radiusMinusButton != null ? radiusMinusButton.getY() - 12 : 0;
        if (radiusMinusButton != null && radiusMinusButton.visible) {
            graphics.drawString(this.font,
                SpellLocalization.gui("editor.field.radius")
                    .append(": ")
                    .append(String.format(Locale.ROOT, "%.1f", formRadius)),
                radiusMinusButton.getX(), labelY, 0xFFFFFF, false);
        }
        if (distanceMinusButton != null && distanceMinusButton.visible) {
            graphics.drawString(this.font,
                SpellLocalization.gui("editor.field.distance")
                    .append(": ")
                    .append(String.format(Locale.ROOT, "%.1f", formDistance)),
                distanceMinusButton.getX(), labelY, 0xFFFFFF, false);
        }
        if (angleMinusButton != null && angleMinusButton.visible) {
            graphics.drawString(this.font,
                SpellLocalization.gui("editor.field.angle")
                    .append(": ")
                    .append(String.format(Locale.ROOT, "%.0f°", formAngle)),
                angleMinusButton.getX(), labelY, 0xFFFFFF, false);
        }
        if (targetingCycle != null && targetingCycle.visible) {
            graphics.drawString(this.font,
                SpellLocalization.gui("editor.targeting.label")
                    .append(": ")
                    .append(targetingLabel(currentTargeting)),
                targetingCycle.getX(), targetingCycle.getY() - 12, 0xFFFFFF, false);
        }
    }

    private void drawNode(GuiGraphics graphics,
                          CardPalette palette,
                          int x,
                          int y,
                          int width,
                          int height,
                          Component title,
                          Component value,
                          boolean active,
                          boolean highlightError,
                          int mouseX,
                          int mouseY,
                          @Nullable DetailInfo detailInfo) {
        boolean hover = isMouseWithin(mouseX, mouseY, x, y, width, height);
        int baseColor = active ? palette.selectedColor : palette.cardColor;
        graphics.fill(x, y, x + width, y + height, baseColor);
        graphics.fill(x, y, x + width, y + 3, palette.accentColor);
        if (highlightError) {
            graphics.fill(x, y, x + width, y + height, 0x55220000);
        }
        graphics.drawString(this.font, title, x + 12, y + 8, palette.textColor, false);
        graphics.drawString(this.font, value, x + 12, y + 30, 0xFFEAF1FF, false);
        if (hover && detailInfo != null) {
            hoveredDetail = detailInfo;
        }
        if (hover) {
            int border = highlightError ? 0xFFFF7474 : palette.accentColor;
            graphics.fill(x, y, x + width, y + 1, border);
            graphics.fill(x, y, x + 1, y + height, border);
            graphics.fill(x + width - 1, y, x + width, y + height, border);
            graphics.fill(x, y + height - 1, x + width, y + height, border);
        }
    }


    private void renderStats(GuiGraphics graphics, int statsY) {
        int statsX = COLUMN_MARGIN;
        graphics.fill(statsX, statsY, statsX + this.width - COLUMN_MARGIN * 2, statsY + 70, 0x66000000);
        if (previewResult == null) {
            graphics.drawString(this.font, SpellLocalization.gui("editor.stats.empty"), statsX + 8, statsY + 8, 0xFFFFFF, false);
            return;
        }
        graphics.drawString(this.font,
            SpellLocalization.gui("editor.stats.complexity")
                .append(": ")
                .append(String.format(Locale.ROOT, "%.2f", previewResult.complexity())),
            statsX + 8, statsY + 8, 0xFFFFFF, false);
        graphics.drawString(this.font,
            SpellLocalization.gui("editor.stats.spirit")
                .append(": ")
                .append(String.format(Locale.ROOT, "%.1f", previewResult.spiritCost())),
            statsX + 8, statsY + 20, 0xFFFFFF, false);
        graphics.drawString(this.font,
            SpellLocalization.gui("editor.stats.cooldown")
                .append(": ")
                .append(String.format(Locale.ROOT, "%.1fs", previewResult.cooldown())),
            statsX + 8, statsY + 32, 0xFFFFFF, false);
        graphics.drawString(this.font,
            SpellLocalization.gui("editor.stats.range")
                .append(": ")
                .append(String.format(Locale.ROOT, "%.1f", previewResult.range())),
            statsX + 220, statsY + 8, 0xFFFFFF, false);
        graphics.drawString(this.font,
            SpellLocalization.gui("editor.stats.radius")
                .append(": ")
                .append(String.format(Locale.ROOT, "%.1f", previewResult.areaRadius())),
            statsX + 220, statsY + 20, 0xFFFFFF, false);
        graphics.drawString(this.font,
            SpellLocalization.gui("editor.stats.duration")
                .append(": ")
                .append(String.format(Locale.ROOT, "%.1fs", previewResult.durationSeconds())),
            statsX + 220, statsY + 32, 0xFFFFFF, false);
    }

    private void renderDetailPanel(GuiGraphics graphics) {
        int width = 220;
        int x = this.width - width - 24;
        int y = 150;
        DetailInfo info = hoveredDetail;
        if (info == null) {
            if (selectedEffect != null) {
                info = createEffectDetail(selectedEffect);
            } else if (selectedForm != null) {
                info = createFormDetail(selectedForm, currentTargeting);
            }
        }
        graphics.fill(x, y, x + width, y + 180, 0xAA050505);
        if (info == null) {
            graphics.drawString(this.font, SpellLocalization.gui("editor.stats.empty"), x + 8, y + 8, 0xFFFFFF, false);
            return;
        }
        graphics.drawString(this.font, info.title, x + 8, y + 8, 0xFFFFFF, false);
        int offsetY = y + 24;
        for (Component line : info.lines) {
            graphics.drawString(this.font, line, x + 8, offsetY, 0xD0D0D0, false);
            offsetY += 12;
        }
    }

    private void drawInfoCard(GuiGraphics graphics,
                              CardPalette palette,
                              int x,
                              int y,
                              int width,
                              int height,
                              Component header,
                              List<Component> lines,
                              boolean highlightError,
                              int mouseX,
                              int mouseY,
                              @Nullable DetailInfo detailInfo) {
        int background = palette.cardColor;
        graphics.fill(x, y, x + width, y + height, background);
        graphics.fill(x, y, x + width, y + 4, palette.accentColor);
        graphics.fill(x + 4, y + 4, x + width - 4, y + height - 4, palette.panelColor);
        if (highlightError) {
            graphics.fill(x, y, x + width, y + height, 0x33FF0000);
        }
        graphics.drawString(this.font, header, x + CARD_PADDING, y + CARD_PADDING, palette.headerColor, false);
        int textY = y + CARD_PADDING + 18;
        for (Component line : lines) {
            graphics.drawString(this.font, line, x + CARD_PADDING, textY, palette.descColor, false);
            textY += 14;
            if (textY > y + height - CARD_PADDING) {
                break;
            }
        }
        boolean hover = isMouseWithin(mouseX, mouseY, x, y, width, height);
        if (hover && detailInfo != null) {
            hoveredDetail = detailInfo;
        }
        if (hover) {
            graphics.fill(x, y, x + width, y + 1, palette.accentColor);
            graphics.fill(x, y, x + 1, y + height, palette.accentColor);
            graphics.fill(x + width - 1, y, x + width, y + height, palette.accentColor);
            graphics.fill(x, y + height - 1, x + width, y + height, palette.accentColor);
        }
    }

    private void drawChip(GuiGraphics graphics, int x, int y, int width, int height, String text) {
        graphics.fill(x, y, x + width, y + height, 0x88382C4A);
        graphics.fill(x, y, x + width, y + 2, 0xFF9D65D3);
        graphics.drawString(this.font, text, x + 8, y + 8, 0xFFF5F1FF, false);
    }

    private boolean isMouseWithin(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private enum DragType {
        FORM,
        EFFECT,
        AUGMENT
    }

    private static class CardArea<T> {
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final T payload;
        private final boolean enabled;

        CardArea(int x, int y, int width, int height, T payload, boolean enabled) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.payload = payload;
            this.enabled = enabled;
        }

        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }

        T payload() {
            return payload;
        }

        boolean enabled() {
            return enabled;
        }
    }

    private static class AugmentSelection {
        private final AugmentDefinition definition;
        private int stacks = 1;

        private AugmentSelection(AugmentDefinition definition) {
            this.definition = definition;
        }

        void increment() {
            int max = Math.max(1, definition.getMaxStacks());
            stacks = stacks >= max ? 1 : stacks + 1;
        }
    }

    private enum CardPalette {
        FORM(0x55121C31, 0xCC16243C, 0xFF2C4F82, 0xFF6AB6FF, 0xFFEAF3FF, 0xFFAED2FF, 0xFFC7D5EA),
        EFFECT(0x55141F1A, 0xCC1A2F29, 0xFF236154, 0xFF64E9C7, 0xFFEAFDF5, 0xFFA4F7D9, 0xFFC6E5DA),
        AUGMENT(0x551F1526, 0xCC281B35, 0xFF5C2B65, 0xFFE7A1FF, 0xFFFFEDFF, 0xFFF9CDFE, 0xFFE6CCE9);

        private final int panelColor;
        private final int cardColor;
        private final int selectedColor;
        private final int accentColor;
        private final int textColor;
        private final int headerColor;
        private final int descColor;

        CardPalette(int panelColor,
                    int cardColor,
                    int selectedColor,
                    int accentColor,
                    int textColor,
                    int headerColor,
                    int descColor) {
            this.panelColor = panelColor;
            this.cardColor = cardColor;
            this.selectedColor = selectedColor;
            this.accentColor = accentColor;
            this.textColor = textColor;
            this.headerColor = headerColor;
            this.descColor = descColor;
        }
    }

    private DetailInfo createFormDetail(FormDefinition form, @Nullable String targetingOverride) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(form.getDescription()).withStyle(ChatFormatting.GRAY));
        lines.add(SpellLocalization.gui("editor.detail.requirement",
            Component.literal(form.getUnlockRealm().name()),
            Component.literal(String.valueOf(form.getMinSubRealmLevel()))));
        if (form.getMaxRadius() > 0) {
            lines.add(Component.literal(String.format(Locale.ROOT, "半径 %.1f - %.1f",
                form.getMinRadius(), form.getMaxRadius())).withStyle(ChatFormatting.WHITE));
        }
        if (form.getMaxDistance() > 0) {
            lines.add(Component.literal(String.format(Locale.ROOT, "距离 %.1f - %.1f",
                form.getMinDistance(), form.getMaxDistance())).withStyle(ChatFormatting.WHITE));
        }
        if (form.getMaxAngle() > 0) {
            lines.add(Component.literal(String.format(Locale.ROOT, "角度 %.0f° - %.0f°",
                form.getMinAngle(), form.getMaxAngle())).withStyle(ChatFormatting.WHITE));
        }
        String targetingMode = targetingOverride != null
            ? targetingOverride
            : (form.getTargetingOptions().isEmpty()
                ? form.getTargeting()
                : form.getTargetingOptions().get(0));
        lines.add(SpellLocalization.gui("editor.detail.targeting",
            targetingLabel(targetingMode)));
        return new DetailInfo(Component.literal(form.getDisplayName()), lines);
    }

    private DetailInfo createEffectDetail(EffectDefinition effect) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(effect.getDescription()).withStyle(ChatFormatting.GRAY));
        lines.add(SpellLocalization.gui("editor.detail.cost",
            Component.literal(String.format(Locale.ROOT, "%.1f", effect.getBaseSpiritCost())),
            Component.literal(String.format(Locale.ROOT, "%.1f", effect.getBaseCooldown()))));
        if (!effect.getAllowedForms().isEmpty()) {
            lines.add(SpellLocalization.gui("editor.detail.allowed_forms",
                Component.literal(String.join(", ", effect.getAllowedForms()))));
        }
        return new DetailInfo(Component.literal(effect.getDisplayName()), lines);
    }

    private DetailInfo createAugmentDetail(AugmentDefinition augment) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(augment.getDescription()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal(String.format(Locale.ROOT, "层数上限 %d | 复杂度 %.2f",
            Math.max(1, augment.getMaxStacks()), augment.getComplexityWeight())));
        if (!augment.getAllowedForms().isEmpty()) {
            lines.add(SpellLocalization.gui("editor.detail.allowed_forms",
                Component.literal(String.join(", ", augment.getAllowedForms()))));
        } else if (!augment.getAllowedEffects().isEmpty()) {
            lines.add(SpellLocalization.gui("editor.detail.allowed_effects",
                Component.literal(String.join(", ", augment.getAllowedEffects()))));
        }
        return new DetailInfo(Component.literal(augment.getDisplayName()), lines);
    }

    private static class DetailInfo {
        private final Component title;
        private final List<Component> lines;

        private DetailInfo(Component title, List<Component> lines) {
            this.title = title;
            this.lines = lines;
        }
    }

    private Component targetingLabel(String mode) {
        return SpellLocalization.gui("targeting." + mode.toLowerCase(Locale.ROOT));
    }

    private double clamp(double value, double min, double max) {
        if (max <= min) {
            return max > 0 ? max : min;
        }
        return Math.max(min, Math.min(max, value));
    }

    private int floorMod(int value, int mod) {
        if (mod <= 0) {
            return 0;
        }
        int result = value % mod;
        return result < 0 ? result + mod : result;
    }
}
