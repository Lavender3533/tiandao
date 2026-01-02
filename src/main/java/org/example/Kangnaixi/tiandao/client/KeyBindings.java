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
        GLFW.GLFW_KEY_L,  // L 键 - 术法编辑器（已禁用）
        CATEGORY
    );

    public static final KeyMapping OPEN_STAR_CHART = new KeyMapping(
        "key.tiandao.open_star_chart",
        GLFW.GLFW_KEY_K,  // K 键 - 打开星盘
        CATEGORY
    );

    public static final KeyMapping TOGGLE_MINDSEA_HOLO = new KeyMapping(
        "key.tiandao.toggle_mindsea_holo",
        GLFW.GLFW_KEY_B,  // B 键（内视）
        CATEGORY
    );

    public static final KeyMapping TOGGLE_SPELL_HAND_WHEEL = new KeyMapping(
        "key.tiandao.toggle_spell_hand_wheel",
        GLFW.GLFW_KEY_V,  // V 键切换手持法盘
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
        event.register(OPEN_STAR_CHART);
        event.register(TOGGLE_MINDSEA_HOLO);
        event.register(TOGGLE_SPELL_HAND_WHEEL);
        Tiandao.LOGGER.info("修仙按键绑定已注册");
    }
}
