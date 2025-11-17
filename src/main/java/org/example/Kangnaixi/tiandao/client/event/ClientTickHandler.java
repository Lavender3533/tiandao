package org.example.Kangnaixi.tiandao.client.event;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.client.KeyBindings;
import org.example.Kangnaixi.tiandao.client.gui.CultivationHUD;
import org.example.Kangnaixi.tiandao.client.gui.CultivationStatusScreen;
import org.example.Kangnaixi.tiandao.client.gui.ArsNouveauStyleSpellEditorScreen;
import org.example.Kangnaixi.tiandao.config.CultivationConfig;

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
        
        // 处理 HUD 详情折叠/展开按键 (J键)
        while (KeyBindings.TOGGLE_HUD_DETAILS.consumeClick()) {
            toggleHudDetails();
        }

        while (KeyBindings.OPEN_SPELL_EDITOR.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                minecraft.setScreen(new ArsNouveauStyleSpellEditorScreen());
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
     * 切换 HUD 详情折叠/展开状态（客户端会话态）
     */
    private static void toggleHudDetails() {
        try {
            CultivationHUD.toggleCollapsed();
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) {
                boolean collapsed = CultivationHUD.isCollapsed();
                String message = collapsed
                    ? "HUD详情已折叠（仅显示灵力与境界）"
                    : "HUD详情已展开（显示全部信息）";
                minecraft.player.sendSystemMessage(Component.literal(message));
            }
        } catch (Exception e) {
            Tiandao.LOGGER.error("切换HUD详情状态时出错", e);
        }
    }
}
