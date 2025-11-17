package org.example.Kangnaixi.tiandao.client;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.lwjgl.glfw.GLFW;

/**
 * 客户端按键处理
 * 使用 ClientTickEvent 正确处理按键输入
 */
@Mod.EventBusSubscriber(modid = Tiandao.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class KeyBindings {
    private static final String CATEGORY = "key.categories.tiandao";
    
    public static final KeyMapping CULTIVATION_STATUS = new KeyMapping(
        "key.tiandao.cultivation_status", 
        GLFW.GLFW_KEY_C, 
        CATEGORY
    );
    
    public static final KeyMapping TOGGLE_HUD = new KeyMapping(
        "key.tiandao.toggle_hud", 
        GLFW.GLFW_KEY_H, 
        CATEGORY
    );
    // 折叠/展开 HUD 详情（J 键）
    public static final KeyMapping TOGGLE_HUD_DETAILS = new KeyMapping(
        "key.tiandao.toggle_hud_details",
        GLFW.GLFW_KEY_J,
        CATEGORY
    );

    public static final KeyMapping OPEN_SPELL_EDITOR = new KeyMapping(
        "key.tiandao.open_spell_editor",
        GLFW.GLFW_KEY_B,
        CATEGORY
    );
    
    /**
     * 注册按键绑定
     */
    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(CULTIVATION_STATUS);
        event.register(TOGGLE_HUD);
        event.register(TOGGLE_HUD_DETAILS);
        event.register(OPEN_SPELL_EDITOR);
        Tiandao.LOGGER.info("修仙按键绑定已注册");
    }
}
