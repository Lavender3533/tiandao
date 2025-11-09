package org.example.Kangnaixi.tiandao.client.event;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.client.KeyBindings;
import org.example.Kangnaixi.tiandao.client.gui.CultivationStatusScreen;
import org.example.Kangnaixi.tiandao.config.CultivationConfig;
import org.example.Kangnaixi.tiandao.network.NetworkHandler;
import org.example.Kangnaixi.tiandao.network.packet.SpellCastPacket;
import org.lwjgl.glfw.GLFW;

/**
 * 客户端 Tick 事件处理器
 * 用于处理每个客户端 tick 的按键检查
 */
@Mod.EventBusSubscriber(modid = Tiandao.MOD_ID, value = Dist.CLIENT)
public class ClientTickHandler {
    
    /**
     * 客户端 Tick 事件处理
     * 在每个 tick 结束时检查按键状态
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        // 只在 tick 结束阶段处理
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        
        // 处理按键输入
        handleKeyInput();
    }
    
    /**
     * 处理按键输入
     */
    private static void handleKeyInput() {
        Minecraft minecraft = Minecraft.getInstance();
        
        // 处理修仙状态界面按键 (C键)
        while (KeyBindings.CULTIVATION_STATUS.consumeClick()) {
            openCultivationStatus();
        }
        
        // 处理HUD切换按键 (H键)
        while (KeyBindings.TOGGLE_HUD.consumeClick()) {
            toggleHud();
        }
        
        // 处理术法快捷键（需要按住Shift键）
        if (minecraft.player != null) {
            boolean isShiftPressed = GLFW.glfwGetKey(minecraft.getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS ||
                                    GLFW.glfwGetKey(minecraft.getWindow().getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
            
            if (isShiftPressed) {
                // 检查术法快捷键 1-4
                while (KeyBindings.CAST_SPELL_1.consumeClick()) {
                    castSpellFromHotbar(0);
                }
                while (KeyBindings.CAST_SPELL_2.consumeClick()) {
                    castSpellFromHotbar(1);
                }
                while (KeyBindings.CAST_SPELL_3.consumeClick()) {
                    castSpellFromHotbar(2);
                }
                while (KeyBindings.CAST_SPELL_4.consumeClick()) {
                    castSpellFromHotbar(3);
                }
            }
        }
    }
    
    /**
     * 打开修仙状态界面
     */
    private static void openCultivationStatus() {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) {
                CultivationStatusScreen.open();
                Tiandao.LOGGER.debug("打开修仙状态界面");
            }
        } catch (Exception e) {
            Tiandao.LOGGER.error("打开修仙状态界面时出错", e);
        }
    }
    
    /**
     * 切换HUD显示状态
     */
    private static void toggleHud() {
        try {
            boolean currentState = CultivationConfig.SHOW_HUD.get();
            CultivationConfig.SHOW_HUD.set(!currentState);
            CultivationConfig.SPEC.save();
            
            // 发送消息给玩家
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) {
                String message = !currentState ? 
                    "修仙HUD已开启" : 
                    "修仙HUD已关闭";
                minecraft.player.sendSystemMessage(Component.literal(message));
                Tiandao.LOGGER.debug("HUD状态切换: {}", !currentState);
            }
        } catch (Exception e) {
            Tiandao.LOGGER.error("切换HUD状态时出错", e);
        }
    }
    
    /**
     * 从术法快捷栏施放术法
     * 
     * @param slotIndex 槽位索引（0-3）
     */
    private static void castSpellFromHotbar(int slotIndex) {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) {
                return;
            }
            
            // 获取槽位中的术法
            minecraft.player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
                String[] hotbar = cultivation.getSpellHotbar();
                
                if (slotIndex < 0 || slotIndex >= hotbar.length) {
                    return;
                }
                
                String spellId = hotbar[slotIndex];
                
                if (spellId == null || spellId.isEmpty()) {
                    minecraft.player.sendSystemMessage(Component.literal("§c槽位 " + (slotIndex + 1) + " 未设置术法"));
                    return;
                }
                
                // 发送网络包到服务器请求施放术法
                NetworkHandler.sendSpellCastToServer(new SpellCastPacket(spellId));
                Tiandao.LOGGER.debug("玩家请求施放术法: {} (槽位 {})", spellId, slotIndex + 1);
            });
        } catch (Exception e) {
            Tiandao.LOGGER.error("施放术法时出错", e);
        }
    }
}

