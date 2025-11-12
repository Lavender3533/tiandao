package org.example.Kangnaixi.tiandao.network.packet;


import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.capability.ICultivation;
import org.example.Kangnaixi.tiandao.cultivation.CultivationRealm;
import org.example.Kangnaixi.tiandao.item.SpellJadeSlipItem;
import org.example.Kangnaixi.tiandao.network.NetworkHandler;
import org.example.Kangnaixi.tiandao.spell.SpellLocalization;
import org.example.Kangnaixi.tiandao.spell.builder.EffectDefinition;
import org.example.Kangnaixi.tiandao.spell.builder.FormDefinition;
import org.example.Kangnaixi.tiandao.spell.builder.SpellComponentAssembler;
import org.example.Kangnaixi.tiandao.spell.builder.SpellComponentLibrary;
import org.example.Kangnaixi.tiandao.spell.blueprint.SpellBlueprint;
import org.example.Kangnaixi.tiandao.spell.blueprint.SpellBlueprintLibrary;
import org.example.Kangnaixi.tiandao.spell.blueprint.SpellBlueprintMetrics;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class SpellBlueprintCreatePacket {
    private final String templateId;
    private final String name;
    private final String description;
    private final double basePower;
    private final double spiritCost;
    private final double cooldown;
    private final double range;
    private final double areaRadius;
    private final SpellBlueprint.ElementType element;
    @Nullable
    private final String advancedJson;
    private final String formId;
    private final String effectId;
    private final List<AugmentPayload> augmentPayloads;
    private final double formRadius;
    private final double formDistance;
    private final double formAngle;
    private final String targetingMode;

    public SpellBlueprintCreatePacket(String templateId, String name, String description,
                                      double basePower, double spiritCost,
                                      double cooldown, double range, double areaRadius,
                                      SpellBlueprint.ElementType element,
                                      @Nullable String advancedJson,
                                      String formId,
                                      String effectId,
                                      List<AugmentPayload> augmentPayloads,
                                      double formRadius,
                                      double formDistance,
                                      double formAngle,
                                      String targetingMode) {
        this.templateId = templateId;
        this.name = name;
        this.description = description;
        this.basePower = basePower;
        this.spiritCost = spiritCost;
        this.cooldown = cooldown;
        this.range = range;
        this.areaRadius = areaRadius;
        this.element = element;
        this.advancedJson = advancedJson;
        this.formId = formId;
        this.effectId = effectId;
        this.augmentPayloads = augmentPayloads;
        this.formRadius = formRadius;
        this.formDistance = formDistance;
        this.formAngle = formAngle;
        this.targetingMode = targetingMode;
    }

    public SpellBlueprintCreatePacket(FriendlyByteBuf buf) {
        this.templateId = buf.readUtf();
        this.name = buf.readUtf();
        this.description = buf.readUtf();
        this.basePower = buf.readDouble();
        this.spiritCost = buf.readDouble();
        this.cooldown = buf.readDouble();
        this.range = buf.readDouble();
        this.areaRadius = buf.readDouble();
        this.element = buf.readEnum(SpellBlueprint.ElementType.class);
        this.advancedJson = buf.readBoolean() ? buf.readUtf() : null;
        this.formId = buf.readUtf();
        this.effectId = buf.readUtf();
        int augmentSize = buf.readVarInt();
        List<AugmentPayload> list = new ArrayList<>();
        for (int i = 0; i < augmentSize; i++) {
            String id = buf.readUtf();
            int stacks = buf.readVarInt();
            list.add(new AugmentPayload(id, stacks));
        }
        this.augmentPayloads = list;
        this.formRadius = buf.readDouble();
        this.formDistance = buf.readDouble();
        this.formAngle = buf.readDouble();
        this.targetingMode = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(templateId);
        buf.writeUtf(name);
        buf.writeUtf(description);
        buf.writeDouble(basePower);
        buf.writeDouble(spiritCost);
        buf.writeDouble(cooldown);
        buf.writeDouble(range);
        buf.writeDouble(areaRadius);
        buf.writeEnum(element);
        if (advancedJson != null) {
            buf.writeBoolean(true);
            buf.writeUtf(advancedJson);
        } else {
            buf.writeBoolean(false);
        }
        buf.writeUtf(formId);
        buf.writeUtf(effectId);
        buf.writeVarInt(augmentPayloads.size());
        for (AugmentPayload payload : augmentPayloads) {
            buf.writeUtf(payload.id());
            buf.writeVarInt(payload.stacks());
        }
        buf.writeDouble(formRadius);
        buf.writeDouble(formDistance);
        buf.writeDouble(formAngle);
        buf.writeUtf(targetingMode);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            SpellComponentLibrary.init();
            SpellBlueprint template = SpellBlueprintLibrary.get(templateId);
            SpellBlueprint.AdvancedData advancedData = advancedJson != null
                ? SpellBlueprint.deserializeAdvancedData(advancedJson)
                : new SpellBlueprint.AdvancedData();
            SpellComponentAssembler.Result assembledResult = null;
            FormDefinition formDefinition = SpellComponentLibrary.findForm(formId).orElse(null);
            EffectDefinition effectDefinition = SpellComponentLibrary.findEffect(effectId).orElse(null);
            List<SpellComponentAssembler.AugmentStack> augmentStacks = new ArrayList<>();
            if (formDefinition != null && effectDefinition != null) {
                for (AugmentPayload payload : augmentPayloads) {
                    SpellComponentLibrary.findAugment(payload.id()).ifPresent(def ->
                        augmentStacks.add(new SpellComponentAssembler.AugmentStack(def, payload.stacks()))
                    );
                }
                double radius = clamp(formRadius, formDefinition.getMinRadius(), formDefinition.getMaxRadius());
                double distance = clamp(formDistance, formDefinition.getMinDistance(), formDefinition.getMaxDistance());
                double angle = clamp(formAngle, formDefinition.getMinAngle(), formDefinition.getMaxAngle());
                String targeting = targetingMode != null ? targetingMode : formDefinition.getTargeting();
                SpellComponentAssembler.FormParameters params = new SpellComponentAssembler.FormParameters(
                    formDefinition.getShape(),
                    targeting,
                    radius,
                    distance,
                    formDefinition.getBaseDurationSeconds(),
                    angle,
                    formDefinition.isMovementLock()
                );
                assembledResult = SpellComponentAssembler.assemble(formDefinition, params, effectDefinition, augmentStacks);
                advancedData = assembledResult.advancedData();
            } else if (template == null) {
                Tiandao.LOGGER.warn("无法解析术法组件 form={} effect={}", formId, effectId);
                return;
            }
            String newId = "tiandao:custom/" + player.getUUID() + "/" + UUID.randomUUID();
            SpellBlueprint.AdvancedData finalAdvancedData = advancedData.copy();
            EffectDefinition finalEffectDefinition = effectDefinition;
            FormDefinition finalFormDefinition = formDefinition;
            SpellComponentAssembler.Result finalAssembledResult = assembledResult;
            player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
                String sanitizedName = name;
                if (sanitizedName.isBlank()) {
                    if (template != null) {
                        sanitizedName = template.getName();
                    } else if (finalEffectDefinition != null) {
                        sanitizedName = finalEffectDefinition.getDisplayName();
                    }
                }
                String sanitizedDesc = description;
                if (sanitizedDesc.isBlank()) {
                    if (template != null) {
                        sanitizedDesc = template.getDescription();
                    } else if (finalEffectDefinition != null) {
                        sanitizedDesc = finalEffectDefinition.getDescription();
                    }
                }

                SpellBlueprint.EffectType effectType = template != null
                    ? template.getEffectType()
                    : finalAssembledResult.effectType();
                SpellBlueprint.TargetingType targetingType = template != null
                    ? template.getTargeting()
                    : finalAssembledResult.targetingType();
                CultivationRealm requiredRealm = template != null
                    ? template.getRequiredRealm()
                    : finalAssembledResult.requiredRealm();
                int requiredSubRealm = template != null
                    ? template.getRequiredSubRealmLevel()
                    : finalAssembledResult.requiredSubRealmLevel();

                double resolvedPower = finalAssembledResult != null ? finalAssembledResult.basePower() : basePower;
                double resolvedCost = finalAssembledResult != null ? finalAssembledResult.spiritCost() : spiritCost;
                double resolvedCooldown = finalAssembledResult != null ? finalAssembledResult.cooldown() : cooldown;
                double resolvedRange = finalAssembledResult != null ? finalAssembledResult.range() : range;
                double resolvedRadius = finalAssembledResult != null ? finalAssembledResult.areaRadius() : areaRadius;

                if (!validateBlueprint(player, finalAdvancedData, cultivation, requiredRealm, resolvedCost)) {
                    return;
                }

                SpellBlueprint custom = new SpellBlueprint(
                    newId,
                    sanitizedName,
                    sanitizedDesc,
                    element,
                    effectType,
                    targetingType,
                    resolvedPower,
                    resolvedCost,
                    resolvedCooldown,
                    resolvedRange,
                    resolvedRadius,
                    requiredRealm,
                    requiredSubRealm,
                    finalAdvancedData
                );

                cultivation.learnBlueprint(custom);
                ItemStack stack = SpellJadeSlipItem.createSlip(custom);
                if (!player.addItem(stack)) {
                    player.drop(stack, false);
                }
                player.sendSystemMessage(SpellLocalization.message("create.success", custom.getName())
                    .withStyle(ChatFormatting.GREEN));
                player.sendSystemMessage(SpellLocalization.message("create.hint.toggle")
                    .withStyle(ChatFormatting.DARK_AQUA));
                NetworkHandler.sendToPlayer(
                    new org.example.Kangnaixi.tiandao.network.CultivationDataSyncPacket(cultivation),
                    player
                );
            });
        });
        context.setPacketHandled(true);
    }

    private boolean validateBlueprint(ServerPlayer player,
                                      SpellBlueprint.AdvancedData data,
                                      ICultivation cultivation,
                                      CultivationRealm requirementRealm,
                                      double manaCost) {
        double complexity = SpellBlueprintMetrics.computeComplexity(data);
        if (complexity > 25 &&
            cultivation.getRealm().ordinal() < requirementRealm.ordinal()) {
            player.sendSystemMessage(SpellLocalization.message("create.error.realm")
                .withStyle(ChatFormatting.RED));
            return false;
        }
        double mana = SpellBlueprintMetrics.estimateManaCost(data, manaCost);
        if (mana > cultivation.getMaxSpiritPower() * 1.5f) {
            player.sendSystemMessage(SpellLocalization.message("create.error.mana")
                .withStyle(ChatFormatting.RED));
            return false;
        }
        return true;
    }

    public record AugmentPayload(String id, int stacks) {
    }

    private static double clamp(double value, double min, double max) {
        if (max <= min) {
            return max > 0 ? max : min;
        }
        return Math.max(min, Math.min(max, value));
    }
}
