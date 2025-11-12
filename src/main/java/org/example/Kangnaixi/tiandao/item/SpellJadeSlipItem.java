
package org.example.Kangnaixi.tiandao.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.spell.SpellLocalization;
import org.example.Kangnaixi.tiandao.spell.blueprint.SpellBlueprint;
import org.example.Kangnaixi.tiandao.spell.blueprint.SpellBlueprintMetrics;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;

public class SpellJadeSlipItem extends Item {
    public static final String BLUEPRINT_TAG = "SpellBlueprint";

    public SpellJadeSlipItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel)) {
            return InteractionResultHolder.pass(stack);
        }

        SpellBlueprint blueprint = getBlueprint(stack);
        if (blueprint == null) {
            player.displayClientMessage(
                SpellLocalization.message("jade_slip.missing").withStyle(ChatFormatting.RED),
                true);
            return InteractionResultHolder.fail(stack);
        }

        serverPlayer.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            if (cultivation.knowsBlueprint(blueprint.getId())) {
                player.displayClientMessage(
                    SpellLocalization.message("jade_slip.duplicate").withStyle(ChatFormatting.YELLOW),
                    true);
            } else {
                cultivation.learnBlueprint(blueprint);
                player.displayClientMessage(
                        SpellLocalization.message("jade_slip.learned", blueprint.getName())
                            .withStyle(ChatFormatting.GREEN),
                        true);
                stack.shrink(1);
            }
        });

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        SpellBlueprint blueprint = getBlueprint(stack);
        if (blueprint == null) {
            tooltip.add(SpellLocalization.tooltip("jade_slip.uninitialized").withStyle(ChatFormatting.RED));
            return;
        }
        tooltip.add(SpellLocalization.tooltip("jade_slip.element",
                SpellLocalization.element(blueprint.getElement()))
            .withStyle(ChatFormatting.AQUA));
        tooltip.add(SpellLocalization.tooltip("jade_slip.effect",
                SpellLocalization.effect(blueprint.getEffectType()))
            .withStyle(ChatFormatting.GRAY));

        SpellBlueprint.AdvancedData data = blueprint.getAdvancedData().orElse(null);
        double complexity = SpellBlueprintMetrics.computeComplexity(data);
        double mana = SpellBlueprintMetrics.estimateManaCost(data, blueprint.getSpiritCost());
        double cooldown = SpellBlueprintMetrics.estimateCooldown(data, blueprint.getCooldownSeconds());
        double overload = SpellBlueprintMetrics.estimateOverloadThreshold(data, blueprint);
        tooltip.add(SpellLocalization.tooltip("jade_slip.summary",
            String.format(Locale.ROOT, "%.1f", complexity),
            String.format(Locale.ROOT, "%.1f", mana),
            String.format(Locale.ROOT, "%.1f", cooldown)).withStyle(ChatFormatting.YELLOW));
        tooltip.add(SpellLocalization.tooltip("jade_slip.overload",
            String.format(Locale.ROOT, "%.1f", overload)).withStyle(ChatFormatting.DARK_AQUA));

        if (data != null) {
            tooltip.add(SpellLocalization.tooltip("jade_slip.shape",
                    SpellLocalization.shape(data.getShape().getType()),
                    String.format(Locale.ROOT, "%.1f", data.getShape().getRadius()),
                    String.format(Locale.ROOT, "%.1f", data.getShape().getLength()))
                .withStyle(ChatFormatting.DARK_GRAY));
            if (!data.getSegments().isEmpty()) {
                tooltip.add(SpellLocalization.tooltip("jade_slip.segments", data.getSegments().size())
                    .withStyle(ChatFormatting.DARK_GREEN));
            }
            if (!data.getStatusEffects().isEmpty()) {
                tooltip.add(SpellLocalization.tooltip("jade_slip.statuses", data.getStatusEffects().size())
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
    }

    @Nullable
    public static SpellBlueprint getBlueprint(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().contains(BLUEPRINT_TAG)) {
            return null;
        }
        return SpellBlueprint.fromNBT(stack.getTag().getCompound(BLUEPRINT_TAG));
    }

    public static ItemStack createSlip(SpellBlueprint blueprint) {
        ItemStack stack = new ItemStack(Tiandao.SPELL_JADE_SLIP.get());
        stack.getOrCreateTag().put(BLUEPRINT_TAG, blueprint.toNBT());
        stack.setHoverName(Component.translatable("item.tiandao.spell_jade_slip.named", blueprint.getName()));
        return stack;
    }
}
