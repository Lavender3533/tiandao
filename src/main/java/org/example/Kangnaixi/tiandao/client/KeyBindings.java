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
    
    // 术法快捷键（Shift + 数字键）
    public static final KeyMapping CAST_SPELL_1 = new KeyMapping(
        "key.tiandao.cast_spell_1", 
        GLFW.GLFW_KEY_1, 
        CATEGORY
    );
    
    public static final KeyMapping CAST_SPELL_2 = new KeyMapping(
        "key.tiandao.cast_spell_2", 
        GLFW.GLFW_KEY_2, 
        CATEGORY
    );
    
    public static final KeyMapping CAST_SPELL_3 = new KeyMapping(
        "key.tiandao.cast_spell_3", 
        GLFW.GLFW_KEY_3, 
        CATEGORY
    );
    
    public static final KeyMapping CAST_SPELL_4 = new KeyMapping(
        "key.tiandao.cast_spell_4", 
        GLFW.GLFW_KEY_4, 
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
        event.register(CAST_SPELL_1);
        event.register(CAST_SPELL_2);
        event.register(CAST_SPELL_3);
        event.register(CAST_SPELL_4);
        Tiandao.LOGGER.info("修仙按键绑定已注册（包括术法快捷键）");
            }
        }
