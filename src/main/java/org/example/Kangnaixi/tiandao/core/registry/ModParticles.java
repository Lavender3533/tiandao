package org.example.Kangnaixi.tiandao.core.registry;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.client.particle.SpiritualEnergyParticle;
import org.example.Kangnaixi.tiandao.cultivation.SpiritualRootType;

/**
 * 粒子类型注册类
 */
public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = 
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, Tiandao.MODID);
    
    // 为每种灵根类型创建粒子类型
    public static final RegistryObject<SimpleParticleType> GOLD_ENERGY = 
            registerParticleType("gold_energy");
    
    public static final RegistryObject<SimpleParticleType> WOOD_ENERGY = 
            registerParticleType("wood_energy");
    
    public static final RegistryObject<SimpleParticleType> WATER_ENERGY = 
            registerParticleType("water_energy");
    
    public static final RegistryObject<SimpleParticleType> FIRE_ENERGY = 
            registerParticleType("fire_energy");
    
    public static final RegistryObject<SimpleParticleType> EARTH_ENERGY = 
            registerParticleType("earth_energy");
    
    private static RegistryObject<SimpleParticleType> registerParticleType(String name) {
        return PARTICLE_TYPES.register(name, () -> new SimpleParticleType(true));
    }
    
    /**
     * 注册粒子提供者
     */
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        
        // 为每种灵根类型注册粒子提供者
        event.registerSpriteSet(GOLD_ENERGY.get(), sprites -> 
            new SpiritualEnergyParticle.Provider(sprites, SpiritualRootType.GOLD));
        
        event.registerSpriteSet(WOOD_ENERGY.get(), sprites -> 
            new SpiritualEnergyParticle.Provider(sprites, SpiritualRootType.WOOD));
        
        event.registerSpriteSet(WATER_ENERGY.get(), sprites -> 
            new SpiritualEnergyParticle.Provider(sprites, SpiritualRootType.WATER));
        
        event.registerSpriteSet(FIRE_ENERGY.get(), sprites -> 
            new SpiritualEnergyParticle.Provider(sprites, SpiritualRootType.FIRE));
        
        event.registerSpriteSet(EARTH_ENERGY.get(), sprites -> 
            new SpiritualEnergyParticle.Provider(sprites, SpiritualRootType.EARTH));
    }
    
    /**
     * 根据灵根类型获取对应的粒子类型
     */
    public static SimpleParticleType getParticleTypeForRoot(SpiritualRootType rootType) {
        switch (rootType) {
            case GOLD:
                return GOLD_ENERGY.get();
            case WOOD:
                return WOOD_ENERGY.get();
            case WATER:
                return WATER_ENERGY.get();
            case FIRE:
                return FIRE_ENERGY.get();
            case EARTH:
                return EARTH_ENERGY.get();
            default:
                return null; // 无灵根时不显示粒子
        }
    }
    
    /**
     * 注册所有粒子类型到事件总线
     */
    public static void register(IEventBus eventBus) {
        PARTICLE_TYPES.register(eventBus);
    }
}