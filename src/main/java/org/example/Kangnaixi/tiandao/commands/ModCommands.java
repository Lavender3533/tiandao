package org.example.Kangnaixi.tiandao.commands;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.Kangnaixi.tiandao.Tiandao;

/**
 * 天道修仙系统命令注册类
 */
@Mod.EventBusSubscriber(modid = Tiandao.MODID)
public class ModCommands {
    
    private static final String COMMAND_VERSION = "1.0";
    
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        // 注册主命令 /tiandao 和别名 /cultivation
        TiandaoCommand.register(event.getDispatcher());
        
        // 注册测试命令 /tiandaotest
        TiandaoTestCommand.register(event.getDispatcher());
        
        Tiandao.LOGGER.info("天道修仙系统命令已注册 v" + COMMAND_VERSION);
        Tiandao.LOGGER.info("- 主命令: /tiandao (别名: /cultivation)");
        Tiandao.LOGGER.info("- 测试命令: /tiandaotest");
    }
}