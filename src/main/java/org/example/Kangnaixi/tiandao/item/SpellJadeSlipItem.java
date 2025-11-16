
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
import org.example.Kangnaixi.tiandao.spell.rune.Rune;
import org.example.Kangnaixi.tiandao.spell.rune.RuneChainSerializer;
import org.example.Kangnaixi.tiandao.spell.rune.RuneChainExecutor;
import org.example.Kangnaixi.tiandao.spell.node.NodeSpell;
import org.example.Kangnaixi.tiandao.spell.node.execution.NodeSpellExecutor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;

public class SpellJadeSlipItem extends Item {
    public static final String BLUEPRINT_TAG = "SpellBlueprint";
    public static final String RUNE_CHAIN_TAG = "RuneChain";  // 符文链标签
    public static final String NODE_SPELL_TAG = "NodeSpell";  // 节点术法标签

    public SpellJadeSlipItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 优先检查是否有节点术法（最新系统）
        if (hasNodeSpell(stack)) {
            if (!level.isClientSide) {
                boolean success = castNodeSpell(stack, player);
                if (success) {
                    player.displayClientMessage(
                        Component.literal("§a节点术法释放成功！"),
                        true);
                } else {
                    player.displayClientMessage(
                        Component.literal("§c节点术法释放失败！"),
                        true);
                }
            }
            return InteractionResultHolder.success(stack);
        }

        // 其次检查是否有符文链
        if (hasRuneChain(stack)) {
            if (!level.isClientSide) {
                boolean success = castRuneChain(stack, player);
                if (success) {
                    player.displayClientMessage(
                        Component.literal("§a符文链释放成功！"),
                        true);
                } else {
                    player.displayClientMessage(
                        Component.literal("§c符文链释放失败！"),
                        true);
                }
            }
            return InteractionResultHolder.success(stack);
        }

        // 如果没有符文链，尝试旧的蓝图系统
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

    // ===== 新的符文链支持 =====

    /**
     * 获取符文链（优先使用符文链，如果没有则返回null）
     */
    @Nullable
    public static List<Rune> getRuneChain(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().contains(RUNE_CHAIN_TAG)) {
            return null;
        }
        return RuneChainSerializer.deserialize(stack.getTag().getCompound(RUNE_CHAIN_TAG));
    }

    /**
     * 创建带符文链的玉简
     */
    public static ItemStack createRuneSlip(String name, List<Rune> runeChain) {
        ItemStack stack = new ItemStack(Tiandao.SPELL_JADE_SLIP.get());
        stack.getOrCreateTag().put(RUNE_CHAIN_TAG, RuneChainSerializer.serialize(runeChain));
        stack.setHoverName(Component.literal(name));
        return stack;
    }

    /**
     * 设置玉简的符文链
     */
    public static void setRuneChain(ItemStack stack, List<Rune> runeChain) {
        stack.getOrCreateTag().put(RUNE_CHAIN_TAG, RuneChainSerializer.serialize(runeChain));
    }

    /**
     * 检查玉简是否有符文链
     */
    public static boolean hasRuneChain(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(RUNE_CHAIN_TAG);
    }

    /**
     * 使用符文链施法
     */
    public static boolean castRuneChain(ItemStack stack, Player player) {
        List<Rune> runeChain = getRuneChain(stack);
        if (runeChain == null || runeChain.isEmpty()) {
            return false;
        }

        // TODO: 检查灵力消耗和冷却时间

        // 执行符文链
        RuneChainExecutor.ExecutionResult result = RuneChainExecutor.execute(runeChain, player);

        return result.isSuccess();
    }

    // ===== 节点术法支持 =====

    /**
     * 获取节点术法
     */
    @Nullable
    public static NodeSpell getNodeSpell(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().contains(NODE_SPELL_TAG)) {
            return null;
        }
        return NodeSpell.fromNBT(stack.getTag().getCompound(NODE_SPELL_TAG));
    }

    /**
     * 创建带节点术法的玉简
     */
    public static ItemStack createNodeSpellSlip(NodeSpell spell) {
        ItemStack stack = new ItemStack(Tiandao.SPELL_JADE_SLIP.get());
        stack.getOrCreateTag().put(NODE_SPELL_TAG, spell.toNBT());
        stack.setHoverName(Component.literal(spell.getName()));
        return stack;
    }

    /**
     * 设置玉简的节点术法
     */
    public static void setNodeSpell(ItemStack stack, NodeSpell spell) {
        stack.getOrCreateTag().put(NODE_SPELL_TAG, spell.toNBT());
    }

    /**
     * 检查玉简是否有节点术法
     */
    public static boolean hasNodeSpell(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(NODE_SPELL_TAG);
    }

    /**
     * 使用节点术法施法
     */
    public static boolean castNodeSpell(ItemStack stack, Player player) {
        NodeSpell spell = getNodeSpell(stack);
        if (spell == null) {
            return false;
        }

        // TODO: 检查灵力消耗和冷却时间

        // 执行节点术法
        NodeSpellExecutor.ExecutionResult result = NodeSpellExecutor.getInstance().execute(spell, player);

        return result.isSuccess();
    }
}
