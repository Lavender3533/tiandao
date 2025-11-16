package org.example.Kangnaixi.tiandao.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.capability.ICultivation;
import org.example.Kangnaixi.tiandao.cultivation.CultivationRealm;
import org.example.Kangnaixi.tiandao.cultivation.SubRealm;
import org.example.Kangnaixi.tiandao.network.NetworkHandler;
import org.example.Kangnaixi.tiandao.network.packet.SpellBlueprintCreatePacket;
import org.example.Kangnaixi.tiandao.spell.SpellLocalization;
import org.example.Kangnaixi.tiandao.spell.blueprint.SpellBlueprint;
import org.example.Kangnaixi.tiandao.spell.builder.*;
import org.joml.Matrix4f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 修仙风格术法构筑器
 * 特点:
 * - 中心术法阵图可视化(八卦/太极风格)
 * - 拖拽式组件交互
 * - 灵气流动效果(性能优化版)
 * - 五行元素着色
 */
public class ImprovedSpellBlueprintEditorScreen extends Screen {

    // 布局常量
    private static final int SIDEBAR_WIDTH = 160;
    private static final int CENTER_SIZE = 280;
    private static final int SLOT_SIZE = 56;
    private static final int SMALL_SLOT = 44;
    private static final int PADDING = 8;

    // 性能优化: 减少粒子生成频率
    private static final int PARTICLE_INTERVAL = 10; // 从2改为10tick
    private static final int MAX_PARTICLES = 30; // 限制最大粒子数

    // 五行颜色 (Wood木/Fire火/Earth土/Metal金/Water水)
    private static final int COLOR_WOOD = 0xFF4CAF50;   // 绿
    private static final int COLOR_FIRE = 0xFFF44336;   // 红
    private static final int COLOR_EARTH = 0xFFFF9800;  // 橙
    private static final int COLOR_METAL = 0xFFCCCCCC;  // 银
    private static final int COLOR_WATER = 0xFF2196F3;  // 蓝
    private static final int COLOR_SPIRIT = 0xFFE1BEE7; // 灵气紫

    // 组件库
    private final List<FormDefinition> forms = new ArrayList<>();
    private final List<EffectDefinition> effects = new ArrayList<>();
    private final List<AugmentDefinition> augments = new ArrayList<>();
    private final List<SpellTemplateDefinition> templates = new ArrayList<>();

    // 当前选择
    private FormDefinition selectedForm;
    private EffectDefinition selectedEffect;
    private final List<AugmentSelection> selectedAugments = new ArrayList<>();

    // 参数
    private double formRadius;
    private double formDistance;
    private double formAngle;
    private String currentTargeting = "SELF";

    // 玩家数据
    private CultivationRealm playerRealm = CultivationRealm.QI_CONDENSATION;
    private int playerSubRealmLevel = 0;

    // UI组件
    private EditBox nameBox;
    private EditBox descriptionBox;
    private Button createButton;
    private Button closeButton;
    private Button clearButton;

    // 参数调节按钮
    private Button radiusMinusButton;
    private Button radiusPlusButton;
    private Button distanceMinusButton;
    private Button distancePlusButton;
    private Button angleMinusButton;
    private Button anglePlusButton;
    private Button targetingButton;

    // 拖拽状态
    @Nullable
    private DraggableComponent draggingComponent;
    private double dragCurrentX;
    private double dragCurrentY;

    // 槽位
    private final List<ComponentSlot> formSlots = new ArrayList<>();
    private final List<ComponentSlot> effectSlots = new ArrayList<>();
    private final List<ComponentSlot> augmentSlots = new ArrayList<>();
    private ComponentSlot centerFormSlot;
    private ComponentSlot centerEffectSlot;
    private final List<ComponentSlot> centerAugmentSlots = new ArrayList<>();

    // 动画(简化版)
    private int tickCount = 0;
    private float rotationAngle = 0;
    private final List<SpiritParticle> particles = new ArrayList<>();

    // 预览
    @Nullable
    private SpellComponentAssembler.Result previewResult;

    // 工具提示
    private final List<Component> tooltipLines = new ArrayList<>();
    private int tooltipX, tooltipY;

    // 滚动
    private int formScrollOffset = 0;
    private int effectScrollOffset = 0;
    private int augmentScrollOffset = 0;

    public ImprovedSpellBlueprintEditorScreen() {
        super(Component.literal("§6术法构筑台"));

        SpellComponentLibrary.init();
        forms.addAll(SpellComponentLibrary.getForms());
        effects.addAll(SpellComponentLibrary.getEffects());
        augments.addAll(SpellComponentLibrary.getAugments());
        templates.addAll(SpellComponentLibrary.getTemplates());

        forms.sort(Comparator.comparing(FormDefinition::getDisplayName, String.CASE_INSENSITIVE_ORDER));
        effects.sort(Comparator.comparing(EffectDefinition::getDisplayName, String.CASE_INSENSITIVE_ORDER));
        augments.sort(Comparator.comparing(AugmentDefinition::getDisplayName, String.CASE_INSENSITIVE_ORDER));
    }

    @Override
    protected void init() {
        super.init();
        readPlayerRealm();

        // 顶部输入框
        nameBox = new EditBox(this.font, 20, 16, 180, 18,
            Component.literal("术法名称"));
        nameBox.setHint(Component.literal("输入术法名称...").withStyle(ChatFormatting.DARK_GRAY));

        descriptionBox = new EditBox(this.font, 210, 16, 280, 18,
            Component.literal("术法描述"));
        descriptionBox.setHint(Component.literal("输入术法描述...").withStyle(ChatFormatting.DARK_GRAY));

        this.addRenderableWidget(nameBox);
        this.addRenderableWidget(descriptionBox);

        // 底部按钮
        clearButton = Button.builder(Component.literal("清空"),
            btn -> clearSelection())
            .bounds(this.width - 280, this.height - 32, 60, 22).build();

        createButton = Button.builder(Component.literal("§6炼制玉简"),
            btn -> createJadeSlip())
            .bounds(this.width - 210, this.height - 32, 100, 22).build();

        closeButton = Button.builder(Component.literal("关闭"),
            btn -> onClose())
            .bounds(this.width - 100, this.height - 32, 70, 22).build();

        this.addRenderableWidget(clearButton);
        this.addRenderableWidget(createButton);
        this.addRenderableWidget(closeButton);

        // 参数调节按钮（在中心区域下方）
        int paramY = this.height / 2 + 150;
        int centerX = this.width / 2;

        // 半径调节
        radiusMinusButton = Button.builder(Component.literal("-"),
            btn -> adjustRadius(-1))
            .bounds(centerX - 180, paramY, 20, 18).build();
        radiusPlusButton = Button.builder(Component.literal("+"),
            btn -> adjustRadius(1))
            .bounds(centerX - 100, paramY, 20, 18).build();

        // 距离调节
        distanceMinusButton = Button.builder(Component.literal("-"),
            btn -> adjustDistance(-1))
            .bounds(centerX - 50, paramY, 20, 18).build();
        distancePlusButton = Button.builder(Component.literal("+"),
            btn -> adjustDistance(1))
            .bounds(centerX + 30, paramY, 20, 18).build();

        // 角度调节
        angleMinusButton = Button.builder(Component.literal("-"),
            btn -> adjustAngle(-5))
            .bounds(centerX + 80, paramY, 20, 18).build();
        anglePlusButton = Button.builder(Component.literal("+"),
            btn -> adjustAngle(5))
            .bounds(centerX + 160, paramY, 20, 18).build();

        // 目标模式切换
        targetingButton = Button.builder(Component.literal("目标模式"),
            btn -> cycleTargeting())
            .bounds(centerX - 60, paramY - 30, 120, 18).build();

        this.addRenderableWidget(radiusMinusButton);
        this.addRenderableWidget(radiusPlusButton);
        this.addRenderableWidget(distanceMinusButton);
        this.addRenderableWidget(distancePlusButton);
        this.addRenderableWidget(angleMinusButton);
        this.addRenderableWidget(anglePlusButton);
        this.addRenderableWidget(targetingButton);

        updateParameterButtonsVisibility();

        initializeSlots();
    }

    private void clearSelection() {
        selectedForm = null;
        selectedEffect = null;
        selectedAugments.clear();
        centerFormSlot.component = null;
        centerEffectSlot.component = null;
        for (ComponentSlot slot : centerAugmentSlots) {
            slot.component = null;
        }
        updateParameterButtonsVisibility();
        recalcPreview();
    }

    private void adjustRadius(int direction) {
        if (selectedForm == null) return;
        double step = selectedForm.getRadiusStep();
        formRadius = clamp(formRadius + direction * step,
            selectedForm.getMinRadius(), selectedForm.getMaxRadius());
        recalcPreview();
    }

    private void adjustDistance(int direction) {
        if (selectedForm == null) return;
        double step = selectedForm.getDistanceStep();
        formDistance = clamp(formDistance + direction * step,
            selectedForm.getMinDistance(), selectedForm.getMaxDistance());
        recalcPreview();
    }

    private void adjustAngle(int degrees) {
        if (selectedForm == null) return;
        double step = selectedForm.getAngleStep();
        formAngle = clamp(formAngle + degrees,
            selectedForm.getMinAngle(), selectedForm.getMaxAngle());
        recalcPreview();
    }

    private void cycleTargeting() {
        if (selectedForm == null) return;
        List<String> options = selectedForm.getTargetingOptions();
        if (options.isEmpty()) return;

        int currentIndex = options.indexOf(currentTargeting);
        currentIndex = (currentIndex + 1) % options.size();
        currentTargeting = options.get(currentIndex);

        // 更新按钮文字
        String label = getTargetingLabel(currentTargeting);
        targetingButton.setMessage(Component.literal(label));
        recalcPreview();
    }

    private String getTargetingLabel(String targeting) {
        return switch (targeting) {
            case "SELF" -> "§b自身";
            case "TARGET_ENTITY" -> "§a目标实体";
            case "TARGET_BLOCK" -> "§e目标方块";
            case "DIRECTIONAL_RELEASE" -> "§6指向施放";
            case "AREA_RELEASE" -> "§d区域施放";
            default -> targeting;
        };
    }

    private void updateParameterButtonsVisibility() {
        boolean hasForm = selectedForm != null;

        if (!hasForm) {
            radiusMinusButton.visible = radiusPlusButton.visible = false;
            distanceMinusButton.visible = distancePlusButton.visible = false;
            angleMinusButton.visible = anglePlusButton.visible = false;
            targetingButton.visible = false;
            return;
        }

        // 根据Form的参数范围决定是否显示按钮
        double minRadius = selectedForm.getMinRadius();
        double maxRadius = selectedForm.getMaxRadius();
        boolean canAdjustRadius = maxRadius > minRadius && maxRadius > 0;
        radiusMinusButton.visible = radiusPlusButton.visible = canAdjustRadius;

        double minDistance = selectedForm.getMinDistance();
        double maxDistance = selectedForm.getMaxDistance();
        boolean canAdjustDistance = maxDistance > minDistance && maxDistance > 0;
        distanceMinusButton.visible = distancePlusButton.visible = canAdjustDistance;

        double minAngle = selectedForm.getMinAngle();
        double maxAngle = selectedForm.getMaxAngle();
        boolean canAdjustAngle = maxAngle > minAngle && maxAngle > 0;
        angleMinusButton.visible = anglePlusButton.visible = canAdjustAngle;

        // 目标模式按钮
        List<String> options = selectedForm.getTargetingOptions();
        targetingButton.visible = !options.isEmpty() && options.size() > 1;
        if (targetingButton.visible) {
            targetingButton.setMessage(Component.literal(getTargetingLabel(currentTargeting)));
        }
    }

    private void initializeSlots() {
        int centerX = this.width / 2;
        int centerY = this.height / 2 + 10;

        formSlots.clear();
        effectSlots.clear();
        augmentSlots.clear();
        centerAugmentSlots.clear();

        // 左侧Form库
        int leftX = PADDING;
        int leftY = 50;
        for (int i = 0; i < forms.size(); i++) {
            FormDefinition form = forms.get(i);
            int slotY = leftY + i * (SMALL_SLOT + PADDING);
            if (slotY > this.height - 60) break;

            formSlots.add(new ComponentSlot(
                leftX, slotY, SIDEBAR_WIDTH - PADDING * 2, SMALL_SLOT,
                ComponentType.FORM, form
            ));
        }

        // 右侧Effect库
        int rightX = this.width - SIDEBAR_WIDTH + PADDING;
        int rightY = 50;
        for (int i = 0; i < effects.size(); i++) {
            EffectDefinition effect = effects.get(i);
            int slotY = rightY + i * (SMALL_SLOT + PADDING);
            if (slotY > this.height / 2 - 20) break;

            effectSlots.add(new ComponentSlot(
                rightX, slotY, SIDEBAR_WIDTH - PADDING * 2, SMALL_SLOT,
                ComponentType.EFFECT, effect
            ));
        }

        // 右侧Augment库
        int augY = this.height / 2 + 10;
        for (int i = 0; i < Math.min(augments.size(), 8); i++) {
            AugmentDefinition aug = augments.get(i);
            int slotY = augY + i * (SMALL_SLOT + PADDING);
            if (slotY > this.height - 60) break;

            augmentSlots.add(new ComponentSlot(
                rightX, slotY, SIDEBAR_WIDTH - PADDING * 2, SMALL_SLOT,
                ComponentType.AUGMENT, aug
            ));
        }

        // 中心槽位 - 上下排列
        centerFormSlot = new ComponentSlot(
            centerX - SLOT_SIZE / 2,
            centerY - SLOT_SIZE - 16,
            SLOT_SIZE, SLOT_SIZE,
            ComponentType.FORM, null
        );

        centerEffectSlot = new ComponentSlot(
            centerX - SLOT_SIZE / 2,
            centerY + 16,
            SLOT_SIZE, SLOT_SIZE,
            ComponentType.EFFECT, null
        );

        // Augment环绕在周围(八卦位)
        int augRadius = 110;
        for (int i = 0; i < 4; i++) {
            double angle = Math.PI * 2 * i / 4 - Math.PI / 2;
            int slotX = (int)(centerX + Math.cos(angle) * augRadius - SMALL_SLOT / 2);
            int slotY = (int)(centerY + Math.sin(angle) * augRadius - SMALL_SLOT / 2);

            centerAugmentSlots.add(new ComponentSlot(
                slotX, slotY, SMALL_SLOT, SMALL_SLOT,
                ComponentType.AUGMENT, null
            ));
        }
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
        tickCount++;
        rotationAngle += 0.3f; // 减慢旋转速度

        // 性能优化: 限制粒子数量
        particles.removeIf(p -> p.age >= p.maxAge);
        for (SpiritParticle p : particles) {
            p.tick();
        }

        // 减少粒子生成频率
        if (tickCount % PARTICLE_INTERVAL == 0 && particles.size() < MAX_PARTICLES) {
            if (selectedForm != null || selectedEffect != null) {
                spawnSpiritParticles();
            }
        }
    }

    private void spawnSpiritParticles() {
        int centerX = this.width / 2;
        int centerY = this.height / 2 + 10;

        // 只生成1-2个粒子
        for (int i = 0; i < 2; i++) {
            double angle = Math.random() * Math.PI * 2;
            double radius = 80 + Math.random() * 30;

            double px = centerX + Math.cos(angle) * radius;
            double py = centerY + Math.sin(angle) * radius;

            // 向中心移动
            double vx = -Math.cos(angle) * 0.3;
            double vy = -Math.sin(angle) * 0.3;

            particles.add(new SpiritParticle(px, py, vx, vy, getSpiritColor(), 60));
        }
    }

    private int getSpiritColor() {
        // 根据选中的Effect或Form返回对应元素颜色
        if (selectedEffect != null) {
            // 可以根据effect的elementType返回对应颜色
            return COLOR_SPIRIT;
        }
        return COLOR_SPIRIT;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (button == 0) { // 左键拖拽
            for (ComponentSlot slot : formSlots) {
                if (slot.isMouseOver(mouseX, mouseY) && slot.component != null) {
                    startDragging(slot.component, ComponentType.FORM, mouseX, mouseY);
                    return true;
                }
            }
            for (ComponentSlot slot : effectSlots) {
                if (slot.isMouseOver(mouseX, mouseY) && slot.component != null) {
                    startDragging(slot.component, ComponentType.EFFECT, mouseX, mouseY);
                    return true;
                }
            }
            for (ComponentSlot slot : augmentSlots) {
                if (slot.isMouseOver(mouseX, mouseY) && slot.component != null) {
                    startDragging(slot.component, ComponentType.AUGMENT, mouseX, mouseY);
                    return true;
                }
            }
        } else if (button == 1) { // 右键移除
            if (centerFormSlot.isMouseOver(mouseX, mouseY) && selectedForm != null) {
                selectedForm = null;
                centerFormSlot.component = null;
                playSound(SoundEvents.UI_BUTTON_CLICK, 0.5f);
                recalcPreview();
                return true;
            }
            if (centerEffectSlot.isMouseOver(mouseX, mouseY) && selectedEffect != null) {
                selectedEffect = null;
                centerEffectSlot.component = null;
                playSound(SoundEvents.UI_BUTTON_CLICK, 0.5f);
                recalcPreview();
                return true;
            }
            for (int i = 0; i < centerAugmentSlots.size(); i++) {
                ComponentSlot slot = centerAugmentSlots.get(i);
                if (slot.isMouseOver(mouseX, mouseY) && i < selectedAugments.size()) {
                    selectedAugments.remove(i);
                    slot.component = null;
                    playSound(SoundEvents.UI_BUTTON_CLICK, 0.5f);
                    recalcPreview();
                    return true;
                }
            }
        }
        return false;
    }

    private void startDragging(Object component, ComponentType type, double mouseX, double mouseY) {
        draggingComponent = new DraggableComponent(component, type);
        dragCurrentX = mouseX;
        dragCurrentY = mouseY;
        playSound(SoundEvents.ITEM_PICKUP, 0.3f);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingComponent != null) {
            dragCurrentX = mouseX;
            dragCurrentY = mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingComponent != null && button == 0) {
            handleDrop(mouseX, mouseY);
            draggingComponent = null;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void handleDrop(double mouseX, double mouseY) {
        if (draggingComponent == null) return;

        switch (draggingComponent.type) {
            case FORM:
                if (centerFormSlot.isMouseOver(mouseX, mouseY)) {
                    FormDefinition form = (FormDefinition) draggingComponent.component;
                    if (isUnlocked(form.getUnlockRealm(), form.getMinSubRealmLevel())) {
                        selectedForm = form;
                        centerFormSlot.component = form;
                        resetFormParameters(form);
                        updateParameterButtonsVisibility();
                        playSound(SoundEvents.ENCHANTMENT_TABLE_USE, 0.7f);
                        recalcPreview();
                    } else {
                        playSound(SoundEvents.VILLAGER_NO, 0.5f);
                    }
                }
                break;

            case EFFECT:
                if (centerEffectSlot.isMouseOver(mouseX, mouseY)) {
                    EffectDefinition effect = (EffectDefinition) draggingComponent.component;
                    if (isEffectAllowed(effect)) {
                        selectedEffect = effect;
                        centerEffectSlot.component = effect;
                        playSound(SoundEvents.ENCHANTMENT_TABLE_USE, 0.7f);
                        recalcPreview();
                    } else {
                        playSound(SoundEvents.VILLAGER_NO, 0.5f);
                    }
                }
                break;

            case AUGMENT:
                AugmentDefinition aug = (AugmentDefinition) draggingComponent.component;
                for (int i = 0; i < centerAugmentSlots.size(); i++) {
                    ComponentSlot slot = centerAugmentSlots.get(i);
                    if (slot.isMouseOver(mouseX, mouseY)) {
                        if (canUseAugment(aug)) {
                            if (i < selectedAugments.size()) {
                                selectedAugments.set(i, new AugmentSelection(aug));
                            } else {
                                selectedAugments.add(new AugmentSelection(aug));
                            }
                            slot.component = aug;
                            playSound(SoundEvents.ENCHANTMENT_TABLE_USE, 0.7f);
                            recalcPreview();
                        } else {
                            playSound(SoundEvents.VILLAGER_NO, 0.5f);
                        }
                        break;
                    }
                }
                break;
        }
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
            playSound(SoundEvents.VILLAGER_NO, 0.5f);
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
        playSound(SoundEvents.PLAYER_LEVELUP, 0.8f);
        this.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        tooltipLines.clear();

        int centerX = this.width / 2;
        int centerY = this.height / 2 + 10;

        // 标题
        graphics.drawCenteredString(this.font, this.title, centerX, 6, 0xFFFFD700);

        // 中心术法阵
        renderFormationCircle(graphics, centerX, centerY, partialTick);

        // 侧边栏
        renderSidebars(graphics, mouseX, mouseY);

        // 槽位
        renderSlots(graphics, mouseX, mouseY);

        // 灵气粒子
        renderSpiritParticles(graphics, partialTick);

        // 统计
        renderStats(graphics);

        // 参数标签
        renderParameterLabels(graphics);

        // UI组件
        super.render(graphics, mouseX, mouseY, partialTick);

        // 拖拽中的组件
        if (draggingComponent != null) {
            renderDragging(graphics, dragCurrentX, dragCurrentY);
        }

        // 工具提示
        if (!tooltipLines.isEmpty()) {
            graphics.renderComponentTooltip(this.font, tooltipLines, tooltipX, tooltipY);
        }
    }

    private void renderFormationCircle(GuiGraphics graphics, int cx, int cy, float partialTick) {
        // 简化的术法阵 - 减少渲染复杂度

        // 外圈金色装饰
        drawCircleOutline(graphics, cx, cy, 120, 1, 0x44FFD700);

        // 中圈旋转八卦
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(cx, cy, 0);
        pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rotationAngle));

        // 八卦符号(简化为8个点)
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2 * i / 8;
            int px = (int)(Math.cos(angle) * 100);
            int py = (int)(Math.sin(angle) * 100);
            graphics.fill(px - 2, py - 2, px + 2, py + 2, 0x88FFD700);
        }

        pose.popPose();

        // 根据Form渲染形状预览
        if (selectedForm != null) {
            renderShapePreview(graphics, cx, cy, selectedForm.getShape());
        }

        // 内圈光晕
        if (selectedForm != null && selectedEffect != null) {
            int glowColor = getSpiritColor() & 0x55FFFFFF;
            for (int r = 30; r <= 50; r += 5) {
                drawCircleOutline(graphics, cx, cy, r, 1, glowColor);
            }
        }
    }

    private void renderShapePreview(GuiGraphics graphics, int cx, int cy, String shapeType) {
        int color = 0xAA4FC3F7; // 浅蓝半透明
        int scale = 60; // 基础缩放

        switch (shapeType.toUpperCase()) {
            case "SELF_AURA" -> {
                // 自身气场 - 圆形光环
                int radius = (int)(scale * Math.max(0.5, Math.min(2.0, formRadius / 4.0)));
                for (int r = radius - 8; r <= radius; r += 2) {
                    drawCircleOutline(graphics, cx, cy, r, 2, color);
                }
            }

            case "SPHERE" -> {
                // 球形 - 实心圆
                int radius = (int)(scale * Math.max(0.5, Math.min(2.0, formRadius / 4.0)));
                fillCircle(graphics, cx, cy, radius, color & 0x66FFFFFF);
                drawCircleOutline(graphics, cx, cy, radius, 2, color);
            }

            case "LINE" -> {
                // 线性 - 直线箭头
                int length = (int)(scale * Math.max(0.5, Math.min(2.0, formDistance / 10.0)));
                graphics.fill(cx - 2, cy - length, cx + 2, cy + length, color);
                // 箭头
                graphics.fill(cx - 6, cy - length, cx, cy - length - 8, color);
                graphics.fill(cx + 6, cy - length, cx, cy - length - 8, color);
            }

            case "CONE" -> {
                // 锥形/扇形
                int distance = (int)(scale * Math.max(0.5, Math.min(2.0, formDistance / 10.0)));
                double angle = Math.toRadians(Math.min(120, formAngle));

                // 绘制扇形
                PoseStack pose = graphics.pose();
                pose.pushPose();
                pose.translate(cx, cy, 0);

                // 中心点
                graphics.fill(-2, -2, 2, 2, color | 0xFF000000);

                // 扇形边缘
                int segments = (int)(formAngle / 5); // 根据角度调整段数
                for (int i = 0; i <= segments; i++) {
                    double a = -angle/2 + (angle * i / segments) - Math.PI/2;
                    int ex = (int)(Math.cos(a) * distance);
                    int ey = (int)(Math.sin(a) * distance);

                    // 从中心到边缘的线
                    if (i % 2 == 0) {
                        drawLine(graphics, 0, 0, ex, ey, color);
                    }

                    // 边缘点
                    graphics.fill(ex - 2, ey - 2, ex + 2, ey + 2, color);
                }

                // 扇形外弧
                for (int i = 0; i <= segments; i++) {
                    double a = -angle/2 + (angle * i / segments) - Math.PI/2;
                    int ex = (int)(Math.cos(a) * distance);
                    int ey = (int)(Math.sin(a) * distance);
                    graphics.fill(ex - 1, ey - 1, ex + 1, ey + 1, color);
                }

                pose.popPose();
            }

            case "PROJECTILE" -> {
                // 弹道 - 小圆球加轨迹
                int distance = (int)(scale * Math.max(0.5, Math.min(1.5, formDistance / 10.0)));

                // 轨迹虚线
                for (int i = 0; i < distance; i += 8) {
                    graphics.fill(cx - 1, cy - i - 4, cx + 1, cy - i, color & 0x88FFFFFF);
                }

                // 弹道末端圆球
                int ballRadius = 6;
                fillCircle(graphics, cx, cy - distance, ballRadius, color);
                drawCircleOutline(graphics, cx, cy - distance, ballRadius, 1, color | 0xFF000000);
            }

            case "TARGET_AREA" -> {
                // 目标区域 - 方形框
                int size = (int)(scale * Math.max(0.5, Math.min(2.0, formRadius / 4.0)));
                graphics.fill(cx - size, cy - size, cx - size + 3, cy + size, color);
                graphics.fill(cx + size - 3, cy - size, cx + size, cy + size, color);
                graphics.fill(cx - size, cy - size, cx + size, cy - size + 3, color);
                graphics.fill(cx - size, cy + size - 3, cx + size, cy + size, color);

                // 中心十字
                graphics.fill(cx - 8, cy - 1, cx + 8, cy + 1, color);
                graphics.fill(cx - 1, cy - 8, cx + 1, cy + 8, color);
            }

            default -> {
                // 默认 - 问号
                graphics.drawCenteredString(this.font, Component.literal("?"),
                    cx, cy - 4, color | 0xFF000000);
            }
        }
    }

    private void drawLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        // 简单的直线绘制（Bresenham算法简化版）
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;

        int steps = 0;
        int maxSteps = 200; // 防止无限循环

        while (steps++ < maxSteps) {
            graphics.fill(x1, y1, x1 + 1, y1 + 1, color);

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

    private void drawCircleOutline(GuiGraphics graphics, int cx, int cy, int radius, int thickness, int color) {
        // 简化版圆形绘制 - 减少采样点
        for (int angle = 0; angle < 360; angle += 10) { // 从5改为10度
            double rad = Math.toRadians(angle);
            int x = (int)(cx + Math.cos(rad) * radius);
            int y = (int)(cy + Math.sin(rad) * radius);
            graphics.fill(x, y, x + thickness, y + thickness, color);
        }
    }

    private void renderSidebars(GuiGraphics graphics, int mouseX, int mouseY) {
        // 左侧
        graphics.fill(0, 45, SIDEBAR_WIDTH, this.height - 40, 0x77000000);
        graphics.drawString(this.font, "§b术式形态", PADDING, 44, 0xFFFFFFFF, false);

        // 右侧上
        int rightX = this.width - SIDEBAR_WIDTH;
        graphics.fill(rightX, 45, this.width, this.height / 2 - 5, 0x77000000);
        graphics.drawString(this.font, "§a术式效果", rightX + PADDING, 44, 0xFFFFFFFF, false);

        // 右侧下
        graphics.fill(rightX, this.height / 2 + 5, this.width, this.height - 40, 0x77000000);
        graphics.drawString(this.font, "§d术式增幅", rightX + PADDING, this.height / 2 + 4, 0xFFFFFFFF, false);
    }

    private void renderSlots(GuiGraphics graphics, int mouseX, int mouseY) {
        for (ComponentSlot slot : formSlots) {
            renderLibrarySlot(graphics, slot, mouseX, mouseY);
        }
        for (ComponentSlot slot : effectSlots) {
            renderLibrarySlot(graphics, slot, mouseX, mouseY);
        }
        for (ComponentSlot slot : augmentSlots) {
            renderLibrarySlot(graphics, slot, mouseX, mouseY);
        }

        renderCenterSlot(graphics, centerFormSlot, "形态", mouseX, mouseY);
        renderCenterSlot(graphics, centerEffectSlot, "效果", mouseX, mouseY);

        for (int i = 0; i < centerAugmentSlots.size(); i++) {
            renderAugmentSlot(graphics, centerAugmentSlots.get(i), i, mouseX, mouseY);
        }
    }

    private void renderLibrarySlot(GuiGraphics graphics, ComponentSlot slot, int mx, int my) {
        if (slot.component == null) return;

        boolean hovered = slot.isMouseOver(mx, my);
        int bgColor = hovered ? 0xAA222222 : 0x88111111;
        int borderColor = getTypeColor(slot.type) | (hovered ? 0xFF000000 : 0x88000000);

        graphics.fill(slot.x, slot.y, slot.x + slot.width, slot.y + slot.height, bgColor);
        graphics.fill(slot.x, slot.y, slot.x + slot.width, slot.y + 2, borderColor);

        String name = getComponentName(slot.component);
        graphics.drawString(this.font, Component.literal(name).withStyle(ChatFormatting.WHITE),
            slot.x + 4, slot.y + slot.height / 2 - 4, 0xFFFFFFFF, false);

        if (hovered) {
            tooltipLines.add(Component.literal(name).withStyle(ChatFormatting.GOLD));
            tooltipLines.add(Component.literal(getComponentDesc(slot.component)).withStyle(ChatFormatting.GRAY));
            tooltipX = mx;
            tooltipY = my;
        }
    }

    private void renderCenterSlot(GuiGraphics graphics, ComponentSlot slot, String label, int mx, int my) {
        boolean hovered = slot.isMouseOver(mx, my);
        boolean filled = slot.component != null;

        int bgColor = filled ? (getTypeColor(slot.type) & 0x33FFFFFF) : 0x66000000;
        int borderColor = filled ? (getTypeColor(slot.type) | 0xFF000000) : 0xFF444444;

        graphics.fill(slot.x, slot.y, slot.x + slot.width, slot.y + slot.height, bgColor);

        int borderW = hovered ? 3 : 2;
        graphics.fill(slot.x, slot.y, slot.x + slot.width, slot.y + borderW, borderColor);
        graphics.fill(slot.x, slot.y, slot.x + borderW, slot.y + slot.height, borderColor);
        graphics.fill(slot.x + slot.width - borderW, slot.y, slot.x + slot.width, slot.y + slot.height, borderColor);
        graphics.fill(slot.x, slot.y + slot.height - borderW, slot.x + slot.width, slot.y + slot.height, borderColor);

        graphics.drawCenteredString(this.font, Component.literal(label).withStyle(ChatFormatting.GRAY),
            slot.x + slot.width / 2, slot.y + 6, 0xFFAAAAAA);

        if (filled) {
            String name = getComponentName(slot.component);
            graphics.drawCenteredString(this.font, Component.literal(name).withStyle(ChatFormatting.WHITE),
                slot.x + slot.width / 2, slot.y + slot.height / 2 - 2, 0xFFFFFFFF);
        }
    }

    private void renderAugmentSlot(GuiGraphics graphics, ComponentSlot slot, int index, int mx, int my) {
        boolean filled = index < selectedAugments.size();
        boolean hovered = slot.isMouseOver(mx, my);

        int color = filled ? 0xFFE1BEE7 : 0xFF555555;
        int bgColor = filled ? 0x44E1BEE7 : 0x33000000;

        // 圆形背景
        fillCircle(graphics, slot.x + slot.width / 2, slot.y + slot.height / 2, slot.width / 2 - 2, bgColor);
        drawCircleOutline(graphics, slot.x + slot.width / 2, slot.y + slot.height / 2,
            slot.width / 2 - 1, hovered ? 2 : 1, color);

        if (filled) {
            AugmentSelection sel = selectedAugments.get(index);
            String name = sel.definition.getDisplayName();
            String abbr = name.length() > 2 ? name.substring(0, 2) : name;

            graphics.drawCenteredString(this.font, Component.literal(abbr).withStyle(ChatFormatting.WHITE),
                slot.x + slot.width / 2, slot.y + slot.height / 2 - 4, 0xFFFFFFFF);

            if (sel.stacks > 1) {
                graphics.drawString(this.font, "×" + sel.stacks,
                    slot.x + slot.width - 14, slot.y + slot.height - 12, 0xFFFFFFFF, false);
            }
        }
    }

    private void fillCircle(GuiGraphics graphics, int cx, int cy, int radius, int color) {
        // 性能优化: 使用更简单的圆形填充算法
        for (int y = -radius; y <= radius; y++) {
            int width = (int) Math.sqrt(radius * radius - y * y);
            graphics.fill(cx - width, cy + y, cx + width, cy + y + 1, color);
        }
    }

    private void renderSpiritParticles(GuiGraphics graphics, float partialTick) {
        for (SpiritParticle p : particles) {
            float alpha = 1.0f - (float)p.age / p.maxAge;
            int a = (int)(alpha * 200);
            int color = (p.color & 0x00FFFFFF) | (a << 24);

            int px = (int)p.x;
            int py = (int)p.y;
            graphics.fill(px - 1, py - 1, px + 2, py + 2, color);
        }
    }

    private void renderDragging(GuiGraphics graphics, double x, double y) {
        String name = getComponentName(draggingComponent.component);
        int color = (getTypeColor(draggingComponent.type) & 0x00FFFFFF) | 0xCC000000;

        int w = 50, h = 50;
        int sx = (int)x - w / 2;
        int sy = (int)y - h / 2;

        graphics.fill(sx, sy, sx + w, sy + h, color);
        graphics.drawCenteredString(this.font, Component.literal(name).withStyle(ChatFormatting.WHITE),
            (int)x, (int)y - 4, 0xFFFFFFFF);
    }

    private void renderStats(GuiGraphics graphics) {
        if (previewResult == null) return;

        int x = this.width / 2 - 200;
        int y = this.height - 60;

        graphics.fill(x, y, x + 400, y + 26, 0x88000000);

        String line1 = String.format(Locale.ROOT,
            "§6威力§r %.1f  §b灵力§r %.1f  §e冷却§r %.1fs  §a范围§r %.1f",
            previewResult.basePower(), previewResult.spiritCost(),
            previewResult.cooldown(), previewResult.range());

        graphics.drawCenteredString(this.font, Component.literal(line1),
            x + 200, y + 4, 0xFFFFFFFF);

        String line2 = String.format(Locale.ROOT, "§d复杂度§r %.2f", previewResult.complexity());
        graphics.drawCenteredString(this.font, Component.literal(line2),
            x + 200, y + 16, 0xFFCCCCFF);
    }

    private void renderParameterLabels(GuiGraphics graphics) {
        if (selectedForm == null) return;

        int paramY = this.height / 2 + 150;
        int centerX = this.width / 2;

        // 半径标签
        if (radiusMinusButton.visible) {
            String radiusText = String.format(Locale.ROOT, "§b半径: %.1f格", formRadius);
            graphics.drawCenteredString(this.font, Component.literal(radiusText),
                centerX - 140, paramY + 4, 0xFFFFFFFF);
        }

        // 距离标签
        if (distanceMinusButton.visible) {
            String distanceText = String.format(Locale.ROOT, "§a距离: %.1f格", formDistance);
            graphics.drawCenteredString(this.font, Component.literal(distanceText),
                centerX - 10, paramY + 4, 0xFFFFFFFF);
        }

        // 角度标签
        if (angleMinusButton.visible) {
            String angleText = String.format(Locale.ROOT, "§e角度: %.0f°", formAngle);
            graphics.drawCenteredString(this.font, Component.literal(angleText),
                centerX + 120, paramY + 4, 0xFFFFFFFF);
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

    private int getTypeColor(ComponentType type) {
        return switch (type) {
            case FORM -> 0xFF4FC3F7;    // 浅蓝
            case EFFECT -> 0xFF66BB6A;  // 绿
            case AUGMENT -> 0xFFE1BEE7; // 紫
        };
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
            this.x = x; this.y = y; this.width = w; this.height = h;
            this.type = type; this.component = comp;
        }

        boolean isMouseOver(double mx, double my) {
            return mx >= x && mx <= x + width && my >= y && my <= y + height;
        }
    }

    private static class DraggableComponent {
        Object component;
        ComponentType type;

        DraggableComponent(Object comp, ComponentType type) {
            this.component = comp; this.type = type;
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

    private static class SpiritParticle {
        double x, y, vx, vy;
        int color, age, maxAge;

        SpiritParticle(double x, double y, double vx, double vy, int color, int maxAge) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
            this.color = color; this.age = 0; this.maxAge = maxAge;
        }

        void tick() {
            x += vx; y += vy; age++;
        }
    }
}
