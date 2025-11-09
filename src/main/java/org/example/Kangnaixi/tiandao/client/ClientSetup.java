package org.example.Kangnaixi.tiandao.client;

import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.client.event.RenderEventHandler;

/**
 * 客户端设置类
 * 处理客户端初始化
 */
public class ClientSetup {
    
    /**
     * 客户端设置事件
     * 
     * @param event 客户端设置事件
     */
    public static void init(FMLClientSetupEvent event) {
        Tiandao.LOGGER.info("天道修仙系统客户端设置完成");
        Tiandao.LOGGER.info("HUD 覆盖层将在 RegisterGuiOverlaysEvent 中注册");
        Tiandao.LOGGER.info("按键绑定将在 RegisterKeyMappingsEvent 中注册");
        
        // 强制加载 RenderEventHandler 类，确保 @Mod.EventBusSubscriber 注解被处理
        try {
            Class.forName(RenderEventHandler.class.getName());
            Tiandao.LOGGER.info("RenderEventHandler 类已加载，事件监听器已注册");
        } catch (ClassNotFoundException e) {
            Tiandao.LOGGER.error("无法加载 RenderEventHandler 类", e);
        }
    }
}
