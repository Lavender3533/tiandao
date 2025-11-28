package org.example.Kangnaixi.tiandao.client.mindsea;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.client.KeyBindings;
import org.example.Kangnaixi.tiandao.client.event.RenderEventHandler;

/**
 * 识海内视模式 - 事件处理器（简化版）
 * 处理按键切换和基本交互
 */
@Mod.EventBusSubscriber(modid = Tiandao.MOD_ID, value = Dist.CLIENT)
public class MindSeaHoloEvents {

    /**
     * 客户端Tick事件处理
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        // B键识海内视功能已禁用
        // 处理B键切换
        // while (KeyBindings.TOGGLE_MINDSEA_HOLO.consumeClick()) {
        //     RenderEventHandler.toggleMindSeaRendering();
        //
        //     boolean enabled = RenderEventHandler.isMindSeaEnabled();
        //
        //     String message = enabled
        //         ? "§6【识海全息】§f已开启 - 11个术法光球环绕显示"
        //         : "§6【识海全息】§f已关闭";
        //     mc.player.sendSystemMessage(Component.literal(message));
        // }
    }
}
