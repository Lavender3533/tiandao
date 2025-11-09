package org.example.Kangnaixi.tiandao.menu;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.container.CultivationContainer;

/**
 * 模组菜单类型注册类
 */
public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, "tiandao");
    
    public static final RegistryObject<MenuType<CultivationContainer>> CULTIVATION_MENU = 
            MENU_TYPES.register("cultivation_menu", 
                    () -> IForgeMenuType.create((windowId, inv, data) -> 
                            new CultivationContainer(windowId, inv, data.readBlockPos())));
    
    public static void register(IEventBus eventBus) {
        MENU_TYPES.register(eventBus);
    }
}