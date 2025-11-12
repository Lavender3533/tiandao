package org.example.Kangnaixi.tiandao;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.example.Kangnaixi.tiandao.blocks.CultivationAltarBlock;
import org.example.Kangnaixi.tiandao.blocks.SpiritGatheringBlock;
import org.example.Kangnaixi.tiandao.capability.ICultivation;
import org.example.Kangnaixi.tiandao.events.CultivationEvents;
import org.example.Kangnaixi.tiandao.item.SpellJadeSlipItem;
import org.example.Kangnaixi.tiandao.spell.blueprint.SpellBlueprintLibrary;
import org.example.Kangnaixi.tiandao.spell.builder.SpellComponentLibrary;
import org.example.Kangnaixi.tiandao.menu.ModMenuTypes;
import org.example.Kangnaixi.tiandao.blockentity.CultivationAltarBlockEntity;
import org.example.Kangnaixi.tiandao.client.event.ParticleEventHandler;
import org.example.Kangnaixi.tiandao.client.ClientSetup;
import org.example.Kangnaixi.tiandao.config.CultivationConfig;
import org.example.Kangnaixi.tiandao.config.Config;
import org.example.Kangnaixi.tiandao.core.registry.ModParticles;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Tiandao.MODID)
public class Tiandao {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "tiandao";
    public static final String MOD_ID = "tiandao"; // 娑撹桨绨￠崗鐓庮啇閹勫潑閸?    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    
    // 閼宠棄濮忓▔銊ュ斀
    public static final Capability<ICultivation> CULTIVATION_CAPABILITY = CapabilityManager.get(new CapabilityToken<ICultivation>() {});
    // Create a Deferred Register to hold Blocks which will all be registered under the "tiandao" namespace
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "tiandao" namespace
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "tiandao" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    // Create a Deferred Register to hold BlockEntityTypes which will all be registered under the "tiandao" namespace
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);

    // 娣囶喕绮伴弬鐟版健
    public static final RegistryObject<Block> CULTIVATION_ALTAR = BLOCKS.register("cultivation_altar", 
        () -> new CultivationAltarBlock(0.2f, 0.1f));
    public static final RegistryObject<Block> SPIRIT_GATHERING_BLOCK = BLOCKS.register("spirit_gathering_block", 
        () -> new SpiritGatheringBlock(5.0f, 20 * 5, 5));
    
    // 娣囶喕绮伴弬鐟版健閻椻晛鎼?
    public static final RegistryObject<Item> CULTIVATION_ALTAR_ITEM = ITEMS.register("cultivation_altar", 
        () -> new BlockItem(CULTIVATION_ALTAR.get(), new Item.Properties()));
    public static final RegistryObject<Item> SPIRIT_GATHERING_BLOCK_ITEM = ITEMS.register("spirit_gathering_block", 
        () -> new BlockItem(SPIRIT_GATHERING_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> SPELL_JADE_SLIP = ITEMS.register("spell_jade_slip",
        () -> new SpellJadeSlipItem(new Item.Properties().stacksTo(16)));

    // 閺傜懓娼＄€圭偘缍嬬猾璇茬€?
    public static final RegistryObject<BlockEntityType<CultivationAltarBlockEntity>> CULTIVATION_ALTAR_BLOCK_ENTITY = 
            BLOCK_ENTITY_TYPES.register("cultivation_altar_block_entity", 
                    () -> BlockEntityType.Builder.of(CultivationAltarBlockEntity::new, CULTIVATION_ALTAR.get()).build(null));

    // 娣囶喕绮伴崚娑⑩偓鐘衬佸蹇旂垼缁涢箖銆?
    public static final RegistryObject<CreativeModeTab> CULTIVATION_TAB = CREATIVE_MODE_TABS.register("cultivation_tab", () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> CULTIVATION_ALTAR_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                // 濞ｈ濮炴穱顔荤舶閺傜懓娼?
                output.accept(CULTIVATION_ALTAR_ITEM.get());
                output.accept(SPIRIT_GATHERING_BLOCK_ITEM.get());
                output.accept(SPELL_JADE_SLIP.get());
            }).build());

    @SuppressWarnings("removal")
    public Tiandao() {
        // Get the mod event bus
        IEventBus modEventBus = net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerCapabilities);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);
        // Register the menu types
        ModMenuTypes.register(modEventBus);
        // Register the block entity types
        BLOCK_ENTITY_TYPES.register(modEventBus);
        
        // Register the particle types
        ModParticles.register(modEventBus);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new CultivationEvents());
        MinecraftForge.EVENT_BUS.register(new ParticleEventHandler());

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        // 注册配置文件
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CultivationConfig.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("天道修仙系统初始化");
        LOGGER.info("功法与阵法注册完毕");
        
        // 注册网络处理器
        event.enqueueWork(() -> {
            org.example.Kangnaixi.tiandao.network.NetworkHandler.register();
        });
        
        // 初始化修炼系统注册表
        event.enqueueWork(() -> {
            org.example.Kangnaixi.tiandao.practice.PracticeRegistry.init();
        });
        
        // 初始化功法注册表
        event.enqueueWork(() -> {
            org.example.Kangnaixi.tiandao.technique.TechniqueRegistry.getInstance();
            LOGGER.info("功法注册表已初始化");
        });
        
        // 初始化术法注册表
        event.enqueueWork(() -> {
            org.example.Kangnaixi.tiandao.spell.SpellRegistry.getInstance();
            LOGGER.info("术法注册表已初始化");
        });
        
        // 初始化术法组件与蓝图库
        event.enqueueWork(SpellComponentLibrary::init);
        event.enqueueWork(SpellBlueprintLibrary::init);
    }
    
    private void registerCapabilities(final RegisterCapabilitiesEvent event) {
        event.register(ICultivation.class);
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        // 添加物品到创造模式标签页
    }
    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("天道修仙服务器启动");
    }
    
    /**
     * 获取命令前缀
     * @return 命令前缀
     */
    public String prefix() {
        return MODID;
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            // Some client setup code
            LOGGER.info("天道修仙客户端初始化");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
            
            // 注册客户端事件
            ClientSetup.init(event);
        }
        
        @SubscribeEvent
        public static void registerParticleProviders(net.minecraftforge.client.event.RegisterParticleProvidersEvent event)
        {
            // 濞夈劌鍞界划鎺戠摍閹绘劒绶甸懓?            ModParticles.registerParticleProviders(event);
        }
    }
}

