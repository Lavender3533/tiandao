package org.example.Kangnaixi.tiandao.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.spell.entity.ModEntityTypes;
import org.example.Kangnaixi.tiandao.spell.node.entity.NodeSpellProjectileEntity;
import org.example.Kangnaixi.tiandao.spell.entity.SpellProjectileEntity;

@Mod.EventBusSubscriber(modid = Tiandao.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModEntityRenderers {

    private static final ResourceLocation NODE_PROJECTILE_TEXTURE =
        ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/enderdragon_fireball.png");
    private static final ResourceLocation RUNE_PROJECTILE_TEXTURE =
        ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/fireball.png");

    private ModEntityRenderers() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.NODE_SPELL_PROJECTILE.get(),
            ctx -> new BillboardProjectileRenderer<NodeSpellProjectileEntity>(ctx, NODE_PROJECTILE_TEXTURE, 0.6f));
        event.registerEntityRenderer(ModEntityTypes.SPELL_PROJECTILE.get(),
            ctx -> new BillboardProjectileRenderer<SpellProjectileEntity>(ctx, RUNE_PROJECTILE_TEXTURE, 0.45f));
    }
}
