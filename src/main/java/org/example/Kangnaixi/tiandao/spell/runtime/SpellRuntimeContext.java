package org.example.Kangnaixi.tiandao.spell.runtime;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import org.example.Kangnaixi.tiandao.capability.ICultivation;
import org.example.Kangnaixi.tiandao.spell.definition.SpellDefinition;
import org.example.Kangnaixi.tiandao.spell.tag.SpellTags;

/**
 * 施法时的运行时上下文。
 */
public final class SpellRuntimeContext {

    private final ServerPlayer player;
    private final ICultivation cultivation;
    private final SpellDefinition definition;
    private final boolean holdingSword;

    private SpellRuntimeContext(ServerPlayer player,
                                ICultivation cultivation,
                                SpellDefinition definition,
                                boolean holdingSword) {
        this.player = player;
        this.cultivation = cultivation;
        this.definition = definition;
        this.holdingSword = holdingSword;
    }

    public static SpellRuntimeContext of(ServerPlayer player,
                                         ICultivation cultivation,
                                         SpellDefinition definition) {
        return new SpellRuntimeContext(player, cultivation, definition,
            isSword(player.getMainHandItem()));
    }

    private static boolean isSword(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.is(ItemTags.SWORDS)) {
            return true;
        }
        if (stack.getItem() == Items.TRIDENT) {
            return false;
        }
        if (stack.isEnchantable() && stack.getEnchantmentLevel(Enchantments.SHARPNESS) > 0) {
            return true;
        }
        for (ResourceLocation tag : SpellTags.additionalSwordTags()) {
            if (stack.is(SpellTags.item(tag))) {
                return true;
            }
        }
        return stack.is(SpellTags.SWORD_WEAPONS);
    }

    public ServerPlayer player() {
        return player;
    }

    public ICultivation cultivation() {
        return cultivation;
    }

    public SpellDefinition definition() {
        return definition;
    }

    public boolean isHoldingSword() {
        return holdingSword;
    }

    public boolean hasSwordIntent() {
        // 目前以术法属性标签为准，通过配置可扩展。
        return definition.hasAttributeTag("sword_intent");
    }

    public boolean shouldApplySwordQi() {
        return definition.isSwordQiCarrier()
            && definition.getSwordQiOverride().isPresent()
            && (holdingSword || hasSwordIntent());
    }
}
