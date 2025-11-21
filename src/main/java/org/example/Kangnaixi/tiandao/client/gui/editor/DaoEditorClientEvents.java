package org.example.Kangnaixi.tiandao.client.gui.editor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.client.gui.editor.screen.DaoEditorTestScreen;
import org.lwjgl.glfw.GLFW;

/**
 * 修仙术法编辑器客户端事件处理器
 */
@Mod.EventBusSubscriber(modid = Tiandao.MODID, value = Dist.CLIENT)
public class DaoEditorClientEvents {
    
    /**
     * 键盘输入事件处理
     */
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        // 检查是否按下了K键（用于测试）
        if (event.getKey() == GLFW.GLFW_KEY_K && event.getAction() == GLFW.GLFW_PRESS) {
            // 检查是否按住了Shift键
            if (Screen.hasShiftDown()) {
                // 打开修仙术法编辑器测试界面
                Minecraft.getInstance().setScreen(new DaoEditorTestScreen());
            }
        }
    }
}