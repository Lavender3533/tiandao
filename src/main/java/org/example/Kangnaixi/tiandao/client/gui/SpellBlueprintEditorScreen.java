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
import java.util.function.Consumer;

/**
 * 简化版术法构筑器：Form → Effect → Augments 三列展示。
 */
public class SpellBlueprintEditorScreen extends Screen {

    private static final int COLUMN_MARGIN = 24;
    private static final int CARD_HEIGHT = 68;
    private static final int CARD_GAP = 10;
    private static final int MAX_AUGMENTS = 3;

    private final List<FormDefinition> forms = new ArrayList<>();
    private final List<EffectDefinition> effects = new ArrayList<>();
    private final List<AugmentDefinition> augments = new ArrayList<>();
    private final List<SpellTemplateDefinition> templates = new ArrayList<>();

    private FormDefinition selectedForm;
    private EffectDefinition selectedEffect;
    private final List<AugmentSelection> selectedAugments = new ArrayList<>();
    private int templateIndex = -1;
    private SpellTemplateDefinition currentTemplate;

    private CultivationRealm playerRealm = CultivationRealm.QI_CONDENSATION;
    private int playerSubRealmLevel = 0;

    private EditBox nameBox;
    private EditBox descriptionBox;
    private Button prevTemplateButton;
    private Button nextTemplateButton;
    private Button clearButton;
    private Button createButton;
    private Button closeButton;

    private final List<CardArea<FormDefinition>> formAreas = new ArrayList<>();
    private final List<CardArea<EffectDefinition>> effectAreas = new ArrayList<>();
    private final List<CardArea<AugmentDefinition>> augmentAreas = new ArrayList<>();
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
    @Nullable
    private DragPayload<?> activeDrag;
    @Nullable
    private Runnable pendingClickAction;
    private boolean dragInProgress;
    private double dragMouseX;
    private double dragMouseY;
    private final NodeBounds formNodeBounds = new NodeBounds();
    private final NodeBounds effectNodeBounds = new NodeBounds();
    private final NodeBounds augmentNodeBounds = new NodeBounds();
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
            selectedForm = forms.isEmpty() ? null : forms.get(0);
            if (selectedForm != null) {
                resetFormParameters(selectedForm);
            } else {
                updateFormControlVisibility();
            }
            selectedEffect = effects.isEmpty() ? null : effects.get(0);
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
        if (handleSelectedAugmentAreaClick(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 0) {
            if (startDragFromAreas(formAreas, DragType.FORM, mouseX, mouseY, this::selectForm)) {
                return true;
            }
            if (startDragFromAreas(effectAreas, DragType.EFFECT, mouseX, mouseY, this::selectEffect)) {
                return true;
            }
            if (startDragFromAreas(augmentAreas, DragType.AUGMENT, mouseX, mouseY, this::toggleAugment)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (activeDrag != null && button == 0) {
            dragMouseX = mouseX;
            dragMouseY = mouseY;
            dragInProgress = true;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (activeDrag != null && button == 0) {
            boolean handled = false;
            if (dragInProgress) {
                handled = handleDrop(activeDrag, mouseX, mouseY);
            } else if (pendingClickAction != null) {
                pendingClickAction.run();
                handled = true;
            }
            clearDragState();
            if (handled) {
                return true;
            }
        }
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

    private <T> boolean startDragFromAreas(List<CardArea<T>> areas,
                                           DragType type,
                                           double mouseX,
                                           double mouseY,
                                           Consumer<T> clickAction) {
        for (CardArea<T> area : areas) {
            if (area.contains(mouseX, mouseY)) {
                if (!area.enabled()) {
                    Component message = type == DragType.FORM
                        ? SpellLocalization.gui("editor.error.locked_component")
                        : SpellLocalization.gui("editor.error_incompatible");
                    showCompatibilityError(type, message);
                    return true;
                }
                beginDrag(type, area.payload(), mouseX, mouseY, () -> clickAction.accept(area.payload()));
                return true;
            }
        }
        return false;
    }

    private <T> void beginDrag(DragType type, T payload, double mouseX, double mouseY, Runnable clickAction) {
        activeDrag = new DragPayload<>(type, payload);
        dragMouseX = mouseX;
        dragMouseY = mouseY;
        dragInProgress = false;
        pendingClickAction = clickAction;
    }

    private boolean handleDrop(DragPayload<?> payload, double mouseX, double mouseY) {
        NodeBounds target = switch (payload.type) {
            case FORM -> formNodeBounds;
            case EFFECT -> effectNodeBounds;
            case AUGMENT -> augmentNodeBounds;
        };
        if (target.isReady() && target.contains(mouseX, mouseY)) {
            switch (payload.type) {
                case FORM -> selectForm((FormDefinition) payload.data);
                case EFFECT -> selectEffect((EffectDefinition) payload.data);
                case AUGMENT -> toggleAugment((AugmentDefinition) payload.data);
            }
            return true;
        }
        if (!target.isReady()) {
            return false;
        }
        showCompatibilityError(payload.type, SpellLocalization.gui("editor.error.invalid_drop"));
        return true;
    }

    private void clearDragState() {
        activeDrag = null;
        pendingClickAction = null;
        dragInProgress = false;
    }

    private void selectForm(FormDefinition form) {
        if (!isUnlocked(form.getUnlockRealm(), form.getMinSubRealmLevel())) {
            showCompatibilityError(DragType.FORM, SpellLocalization.gui("editor.error.locked_component"));
            return;
        }
        selectedForm = form;
        resetFormParameters(form);
        ensureCompatibility();
        clearCompatibilityState();
        recalcPreview();
    }

    private void selectEffect(EffectDefinition effect) {
        if (!isEffectAllowed(effect)) {
            showCompatibilityError(DragType.EFFECT, SpellLocalization.gui("editor.error_incompatible"));
            return;
        }
        selectedEffect = effect;
        ensureCompatibility();
        clearCompatibilityState();
        recalcPreview();
    }

    private void toggleAugment(AugmentDefinition augment) {
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

    private void ensureCompatibility() {
        if (selectedEffect != null && !isEffectAllowed(selectedEffect)) {
            selectedEffect = null;
        }
        selectedAugments.removeIf(sel -> !canUseAugment(sel.definition));
        if (selectedForm == null) {
            updateFormControlVisibility();
        }
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

        int columnWidth = (this.width - COLUMN_MARGIN * 2 - 40) / 3;
        hoveredDetail = null;
        renderNodeRow(graphics, mouseX, mouseY);
        renderParameterLabels(graphics);
        int top = 140;
        renderFormColumn(graphics, COLUMN_MARGIN, columnWidth, top, mouseX, mouseY);
        renderEffectColumn(graphics, COLUMN_MARGIN + columnWidth + 20, columnWidth, top, mouseX, mouseY);
        renderAugmentColumn(graphics, COLUMN_MARGIN + (columnWidth + 20) * 2, columnWidth, top, mouseX, mouseY);
        renderStats(graphics);
        renderDetailPanel(graphics);
        renderDragGhost(graphics);
        if (!compatibilityMessage.getString().isEmpty()) {
            graphics.drawString(this.font, compatibilityMessage, 30, this.height - 48, 0xFF5555, false);
        }
    }

    private void renderNodeRow(GuiGraphics graphics, int mouseX, int mouseY) {
        int widthUnit = (this.width - COLUMN_MARGIN * 2 - 120) / 3;
        int nodeWidth = Math.min(180, widthUnit);
        int nodeHeight = 56;
        int y = 80;
        int formX = COLUMN_MARGIN;
        int effectX = COLUMN_MARGIN + nodeWidth + 60;
        int augmentX = COLUMN_MARGIN + (nodeWidth + 60) * 2;

        boolean formError = shouldHighlightNode(DragType.FORM);
        boolean effectError = shouldHighlightNode(DragType.EFFECT);
        boolean augmentError = shouldHighlightNode(DragType.AUGMENT);

        drawConnector(graphics, formX + nodeWidth, y + nodeHeight / 2, effectX, y + nodeHeight / 2,
            selectedForm != null && selectedEffect != null, formError || effectError);
        drawConnector(graphics, effectX + nodeWidth, y + nodeHeight / 2, augmentX, y + nodeHeight / 2,
            selectedEffect != null && !selectedAugments.isEmpty(), effectError || augmentError);

        drawNode(graphics, formX, y, nodeWidth, nodeHeight,
            SpellLocalization.gui("editor.column.form"),
            selectedForm != null ? Component.literal(selectedForm.getDisplayName()) :
                SpellLocalization.gui("editor.missing_selection"),
            selectedForm != null,
            formError,
            mouseX,
            mouseY,
            selectedForm == null ? null : createFormDetail(selectedForm, currentTargeting));
        formNodeBounds.set(formX, y, nodeWidth, nodeHeight);

        drawNode(graphics, effectX, y, nodeWidth, nodeHeight,
            SpellLocalization.gui("editor.column.effect"),
            selectedEffect != null ? Component.literal(selectedEffect.getDisplayName()) :
                SpellLocalization.gui("editor.missing_selection"),
            selectedEffect != null,
            effectError,
            mouseX,
            mouseY,
            selectedEffect == null ? null : createEffectDetail(selectedEffect));
        effectNodeBounds.set(effectX, y, nodeWidth, nodeHeight);

        String augmentTitle = selectedAugments.isEmpty()
            ? SpellLocalization.gui("editor.missing_selection").getString()
            : selectedAugments.size() + "× " + selectedAugments.get(0).definition.getDisplayName();
        DetailInfo augmentDetail = selectedAugments.isEmpty()
            ? null
            : createAugmentDetail(selectedAugments.get(0).definition);
        drawNode(graphics, augmentX, y, nodeWidth, nodeHeight,
            SpellLocalization.gui("editor.column.augment"),
            Component.literal(augmentTitle),
            !selectedAugments.isEmpty(),
            augmentError,
            mouseX,
            mouseY,
            augmentDetail);
        augmentNodeBounds.set(augmentX, y, nodeWidth, nodeHeight);
    }

    private void renderParameterLabels(GuiGraphics graphics) {
        if (selectedForm == null) {
            return;
        }
        int y = radiusMinusButton.getY() - 12;
        if (radiusMinusButton != null && radiusMinusButton.visible) {
            graphics.drawString(this.font,
                SpellLocalization.gui("editor.field.radius")
                    .append(": ")
                    .append(String.format(Locale.ROOT, "%.1f", formRadius)),
                radiusMinusButton.getX(), y, 0xFFFFFF, false);
        }
        if (distanceMinusButton != null && distanceMinusButton.visible) {
            graphics.drawString(this.font,
                SpellLocalization.gui("editor.field.distance")
                    .append(": ")
                    .append(String.format(Locale.ROOT, "%.1f", formDistance)),
                distanceMinusButton.getX(), y, 0xFFFFFF, false);
        }
        if (angleMinusButton != null && angleMinusButton.visible) {
            graphics.drawString(this.font,
                SpellLocalization.gui("editor.field.angle")
                    .append(": ")
                    .append(String.format(Locale.ROOT, "%.0f°", formAngle)),
                angleMinusButton.getX(), y, 0xFFFFFF, false);
        }
        if (targetingCycle != null && targetingCycle.visible) {
            graphics.drawString(this.font,
                SpellLocalization.gui("editor.targeting.label")
                    .append(": ")
                    .append(targetingLabel(currentTargeting)),
                targetingCycle.getX(), targetingCycle.getY() - 12, 0xFFFFFF, false);
        }
    }

    private void drawConnector(GuiGraphics graphics, int x1, int y1, int x2, int y2, boolean active, boolean error) {
        int color = error ? 0xFFB94A48 : (active ? 0xFF51A6FF : 0xFF404040);
        graphics.fill(Math.min(x1, x2), y1 - 2, Math.max(x1, x2), y1 + 2, color);
    }

    private void drawNode(GuiGraphics graphics,
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
        int color = highlightError ? 0xFF531818 : (active ? 0xFF1E324F : 0xAA111111);
        graphics.fill(x, y, x + width, y + height, color);
        graphics.drawString(this.font, title, x + 8, y + 6, 0xFFBFF3FF, false);
        graphics.drawString(this.font, value, x + 8, y + 26, 0xFFFFFFFF, false);
        if (hover && detailInfo != null) {
            hoveredDetail = detailInfo;
        }
        if (hover) {
            int border = highlightError ? 0xFFFF5555 : 0xFFFFFFFF;
            graphics.fill(x, y, x + width, y + 1, border);
            graphics.fill(x, y, x + 1, y + height, border);
            graphics.fill(x + width - 1, y, x + width, y + height, border);
            graphics.fill(x, y + height - 1, x + width, y + height, border);
        }
    }

    private void renderFormColumn(GuiGraphics graphics, int x, int width, int top, int mouseX, int mouseY) {
        formAreas.clear();
        graphics.drawString(this.font, SpellLocalization.gui("editor.column.form"), x, top - 14, 0xFFE3A1, false);
        int y = top;
        boolean anyUnlocked = false;
        for (FormDefinition form : forms) {
            if (!isUnlocked(form.getUnlockRealm(), form.getMinSubRealmLevel())) {
                continue;
            }
            boolean selected = selectedForm == form;
            drawCard(graphics, x, y, width, CARD_HEIGHT,
                form.getDisplayName(),
                SpellLocalization.gui("editor.form.desc",
                    Component.literal("R:" + form.getBaseRadius()),
                    Component.literal("D:" + form.getBaseDurationSeconds())),
                selected,
                true,
                mouseX,
                mouseY,
                createFormDetail(form, selected ? currentTargeting : null));
            formAreas.add(new CardArea<>(x, y, width, CARD_HEIGHT, form, true));
            y += CARD_HEIGHT + CARD_GAP;
            anyUnlocked = true;
        }
        if (!anyUnlocked) {
            graphics.drawString(this.font, SpellLocalization.gui("editor.column.locked_hint"), x, top + 4, 0xFFAAAAAA, false);
        }
    }

    private void renderEffectColumn(GuiGraphics graphics, int x, int width, int top, int mouseX, int mouseY) {
        effectAreas.clear();
        graphics.drawString(this.font, SpellLocalization.gui("editor.column.effect"), x, top - 14, 0xAEE3FF, false);
        int y = top;
        for (EffectDefinition effect : effects) {
            boolean allowed = selectedForm == null || selectedForm.getAllowedEffects().isEmpty()
                || selectedForm.getAllowedEffects().contains(effect.getId());
            boolean selected = selectedEffect == effect;
            drawCard(graphics, x, y, width, CARD_HEIGHT,
                effect.getDisplayName(),
                Component.literal(effect.getDescription()),
                selected,
                allowed,
                mouseX,
                mouseY,
                createEffectDetail(effect));
            effectAreas.add(new CardArea<>(x, y, width, CARD_HEIGHT, effect, allowed));
            y += CARD_HEIGHT + CARD_GAP;
        }
    }

    private void renderAugmentColumn(GuiGraphics graphics, int x, int width, int top, int mouseX, int mouseY) {
        augmentAreas.clear();
        selectedAugmentAreas.clear();
        graphics.drawString(this.font, SpellLocalization.gui("editor.column.augment"), x, top - 14, 0xC6FFA3, false);
        int y = top;
        for (AugmentDefinition augment : augments) {
            boolean enabled = selectedForm != null && selectedEffect != null && canUseAugment(augment);
            drawCard(graphics, x, y, width, CARD_HEIGHT,
                augment.getDisplayName(),
                Component.literal(augment.getDescription()),
                selectedAugments.stream().anyMatch(sel -> sel.definition == augment),
                enabled,
                mouseX,
                mouseY,
                createAugmentDetail(augment));
            augmentAreas.add(new CardArea<>(x, y, width, CARD_HEIGHT, augment, enabled));
            y += CARD_HEIGHT + CARD_GAP;
        }

        int chipTop = y + 6;
        graphics.drawString(this.font, SpellLocalization.gui("editor.augment.selected"), x, chipTop, 0xFFFFFF, false);
        chipTop += 8;
        graphics.drawString(this.font, SpellLocalization.gui("editor.augment.selected_hint"), x, chipTop, 0x8EE0E0E0, false);
        chipTop += 10;
        for (AugmentSelection selection : selectedAugments) {
            int chipHeight = 28;
            drawChip(graphics, x, chipTop, width, chipHeight,
                selection.definition.getDisplayName() + " x" + selection.stacks);
            selectedAugmentAreas.add(new CardArea<>(x, chipTop, width, chipHeight, selection, true));
            chipTop += chipHeight + 4;
        }
    }

    private void renderStats(GuiGraphics graphics) {
        int statsX = COLUMN_MARGIN;
        int statsY = this.height - 110;
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

    private void renderDragGhost(GuiGraphics graphics) {
        if (activeDrag == null || !dragInProgress) {
            return;
        }
        int width = 160;
        int height = 46;
        int x = (int) dragMouseX - width / 2;
        int y = (int) dragMouseY - height / 2;
        graphics.fill(x, y, x + width, y + height, 0x99F4F4F4);
        graphics.drawString(this.font, dragSubtitle(activeDrag.type), x + 8, y + 6, 0xFF4C7EFF, false);
        graphics.drawString(this.font, dragLabel(activeDrag), x + 8, y + 24, 0xFF000000, false);
    }

    private Component dragLabel(DragPayload<?> payload) {
        if (payload.data instanceof FormDefinition form) {
            return Component.literal(form.getDisplayName());
        }
        if (payload.data instanceof EffectDefinition effect) {
            return Component.literal(effect.getDisplayName());
        }
        if (payload.data instanceof AugmentDefinition augment) {
            return Component.literal(augment.getDisplayName());
        }
        return Component.empty();
    }

    private Component dragSubtitle(DragType type) {
        return switch (type) {
            case FORM -> SpellLocalization.gui("editor.column.form");
            case EFFECT -> SpellLocalization.gui("editor.column.effect");
            case AUGMENT -> SpellLocalization.gui("editor.column.augment");
        };
    }

    private void drawCard(GuiGraphics graphics,
                          int x,
                          int y,
                          int width,
                          int height,
                          String title,
                          Component desc,
                          boolean selected,
                          boolean enabled,
                          int mouseX,
                          int mouseY,
                          DetailInfo detailInfo) {
        int color = selected ? 0xFF4C7EFF : enabled ? 0xCC1E1E1E : 0x661E1E1E;
        graphics.fill(x, y, x + width, y + height, color);
        if (!enabled) {
            graphics.fill(x, y, x + width, y + height, 0x44000000);
        }
        graphics.drawString(this.font, title, x + 6, y + 6, enabled ? 0xFFFFFF : 0xAAAAAA, false);
        graphics.drawString(this.font, this.font.plainSubstrByWidth(desc.getString(), width - 12), x + 6, y + 20, 0xCFCFCF, false);
        boolean hover = isMouseWithin(mouseX, mouseY, x, y, width, height);
        if (hover && detailInfo != null) {
            hoveredDetail = detailInfo;
        }
        if (hover) {
            graphics.fill(x, y, x + width, y + 1, 0xFFFFFFFF);
            graphics.fill(x, y, x + 1, y + height, 0xFFFFFFFF);
            graphics.fill(x + width - 1, y, x + width, y + height, 0xFFFFFFFF);
            graphics.fill(x, y + height - 1, x + width, y + height, 0xFFFFFFFF);
        }
    }

    private void drawChip(GuiGraphics graphics, int x, int y, int width, int height, String text) {
        graphics.fill(x, y, x + width, y + height, 0xAA2E774B);
        graphics.drawString(this.font, text, x + 6, y + 8, 0xFFFFFF, false);
    }

    private boolean isMouseWithin(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private enum DragType {
        FORM,
        EFFECT,
        AUGMENT
    }

    private static class DragPayload<T> {
        private final DragType type;
        private final T data;

        private DragPayload(DragType type, T data) {
            this.type = type;
            this.data = data;
        }
    }

    private static class NodeBounds {
        private int x;
        private int y;
        private int width;
        private int height;

        void set(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        boolean isReady() {
            return width > 0 && height > 0;
        }

        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
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
}
