package org.example.Kangnaixi.tiandao.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.client.starchart.StarChartClientManager;
import org.example.Kangnaixi.tiandao.network.NetworkHandler;
import org.example.Kangnaixi.tiandao.network.packet.C2SCastActiveSpellPacket;
import org.example.Kangnaixi.tiandao.network.packet.C2SHotbarSelectPacket;
import org.lwjgl.glfw.GLFW;

/**
 * 术法快捷栏按键绑定处理器
 * 负责注册和处理数字键1~9以及施法键R
 */
@Mod.EventBusSubscriber(modid = Tiandao.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class KeybindHandler {

    // 按键分类
    private static final String CATEGORY = "key.categories.tiandao.spell";

    // 施法键（R键）
    public static final KeyMapping CAST_SPELL = new KeyMapping(
        "key.tiandao.cast_spell",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_R,
        CATEGORY
    );

    // 快捷栏数字键1~9
    public static final KeyMapping[] HOTBAR_KEYS = new KeyMapping[9];

    static {
        for (int i = 0; i < 9; i++) {
            HOTBAR_KEYS[i] = new KeyMapping(
                "key.tiandao.hotbar_" + (i + 1),
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_1 + i, // GLFW_KEY_1 到 GLFW_KEY_9
                CATEGORY
            );
        }
    }

    /**
     * 注册按键绑定
     */
    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        // 注册施法键
        event.register(CAST_SPELL);

        // 注册快捷栏数字键
        for (KeyMapping key : HOTBAR_KEYS) {
            event.register(key);
        }

        Tiandao.LOGGER.info("术法快捷栏按键已注册");
    }

    /**
     * 客户端Tick事件处理按键输入
     */
    @Mod.EventBusSubscriber(modid = Tiandao.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientTickHandler {
        
        // 按键状态（防止重复触发）
        private static boolean upKeyPressed = false;
        private static boolean downKeyPressed = false;

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            // 只在Tick结束阶段处理，避免重复
            if (event.phase != TickEvent.Phase.END) {
                return;
            }

            // 检查施法键
            // 注意：星盘开启时，R键用于联动填充手盘，不执行施法
            if (CAST_SPELL.consumeClick()) {
                // 如果星盘已开启，跳过施法（R键由HandWheelKeyHandler处理联动）
                if (StarChartClientManager.getInstance().isEnabled()) {
                    Tiandao.LOGGER.debug("星盘已开启，R键用于联动，跳过施法");
                    return;
                }
                
                // 获取当前选中的术法并施放
                org.example.Kangnaixi.tiandao.spell.blueprint.SpellBlueprint selectedSpell = 
                    org.example.Kangnaixi.tiandao.client.gui.HandWheelComboHud.getSelectedSpell();
                if (selectedSpell != null) {
                    Tiandao.LOGGER.info("R键施放术法: {}", selectedSpell.getName());
                    NetworkHandler.sendCastActiveSpellToServer(new C2SCastActiveSpellPacket(selectedSpell.getId()));
                } else {
                    Tiandao.LOGGER.info("R键被按下，发送施法数据包到服务器");
                    NetworkHandler.sendCastActiveSpellToServer(new C2SCastActiveSpellPacket());
                }
            }
            
            // 上下键选择术法
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.screen == null) {
                long window = mc.getWindow().getWindow();
                if (org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_UP) == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
                    // 防止重复触发
                    if (!upKeyPressed) {
                        org.example.Kangnaixi.tiandao.client.gui.HandWheelComboHud.selectPrev();
                        upKeyPressed = true;
                    }
                } else {
                    upKeyPressed = false;
                }
                
                if (org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN) == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
                    if (!downKeyPressed) {
                        org.example.Kangnaixi.tiandao.client.gui.HandWheelComboHud.selectNext();
                        downKeyPressed = true;
                    }
                } else {
                    downKeyPressed = false;
                }
            }

            // 检查快捷栏数字键
            for (int i = 0; i < 9; i++) {
                if (HOTBAR_KEYS[i].consumeClick()) {
                    Tiandao.LOGGER.info("数字键 {} 被按下，选择槽位 {}", i + 1, i);
                    NetworkHandler.sendHotbarSelectToServer(new C2SHotbarSelectPacket(i));
                }
            }
        }
    }
}
