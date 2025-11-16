package org.example.Kangnaixi.tiandao.spell.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.spell.node.entity.NodeSpellProjectileEntity;

/**
 * 术法实体类型注册
 */
public class ModEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Tiandao.MODID);

    /**
     * 术法弹道实体（符文链）
     */
    public static final RegistryObject<EntityType<SpellProjectileEntity>> SPELL_PROJECTILE =
        ENTITY_TYPES.register("spell_projectile",
            () -> EntityType.Builder.<SpellProjectileEntity>of(SpellProjectileEntity::new, MobCategory.MISC)
                .sized(0.25F, 0.25F)
                .clientTrackingRange(4)
                .updateInterval(10)
                .build("spell_projectile"));

    /**
     * 节点术法弹道实体
     */
    public static final RegistryObject<EntityType<NodeSpellProjectileEntity>> NODE_SPELL_PROJECTILE =
        ENTITY_TYPES.register("node_spell_projectile",
            () -> EntityType.Builder.<NodeSpellProjectileEntity>of(NodeSpellProjectileEntity::new, MobCategory.MISC)
                .sized(0.25F, 0.25F)
                .clientTrackingRange(4)
                .updateInterval(10)
                .build("node_spell_projectile"));

    /**
     * 注册到事件总线
     */
    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
