package org.example.Kangnaixi.tiandao.spell.tag;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.example.Kangnaixi.tiandao.Config;
import org.example.Kangnaixi.tiandao.Tiandao;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Spell 系统用到的标签。
 */
public final class SpellTags {

    public static final TagKey<Item> SWORD_WEAPONS = TagKey.create(Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath(Tiandao.MODID, "weapon/swords"));

    private static final Set<ResourceLocation> CACHED_EXTRA_SWORD_TAGS = new LinkedHashSet<>();

    private SpellTags() {}

    public static TagKey<Item> item(ResourceLocation id) {
        return ItemTags.create(id);
    }

    public static Set<ResourceLocation> additionalSwordTags() {
        if (CACHED_EXTRA_SWORD_TAGS.isEmpty() && !Config.extraSwordTags.isEmpty()) {
            CACHED_EXTRA_SWORD_TAGS.addAll(Config.extraSwordTags);
        }
        return Collections.unmodifiableSet(CACHED_EXTRA_SWORD_TAGS);
    }

    public static void refreshCachedTags() {
        CACHED_EXTRA_SWORD_TAGS.clear();
        if (!Config.extraSwordTags.isEmpty()) {
            CACHED_EXTRA_SWORD_TAGS.addAll(Config.extraSwordTags);
        }
    }
}
