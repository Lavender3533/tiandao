package org.example.Kangnaixi.tiandao.commands;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.Kangnaixi.tiandao.Tiandao;

/**
 * 天道修仙系统命令注册类
 * 所有命令已整合到 /tiandao 主命令
 */
@Mod.EventBusSubscriber(modid = Tiandao.MODID)
public class ModCommands {

    private static final String COMMAND_VERSION = "2.0";

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        // 注册主命令 /tiandao 和别名 /cultivation
        // 所有功能都已整合到主命令中
        TiandaoCommand.register(event.getDispatcher());
        SpellBlueprintCommand.register(event.getDispatcher());

        Tiandao.LOGGER.info("天道修仙系统命令已注册 v" + COMMAND_VERSION);
        Tiandao.LOGGER.info("- 主命令: /tiandao (别名: /cultivation)");
        Tiandao.LOGGER.info("  ├── status - 查看修仙状态");
        Tiandao.LOGGER.info("  ├── foundation - 查看根基状态");
        Tiandao.LOGGER.info("  ├── spell - 术法系统");
        Tiandao.LOGGER.info("  │   ├── list - 列出已解锁术法");
        Tiandao.LOGGER.info("  │   ├── cast <id> - 施放术法");
        Tiandao.LOGGER.info("  │   ├── info <id> - 查看术法详情");
        Tiandao.LOGGER.info("  │   ├── blueprint - 术法蓝图管理");
        Tiandao.LOGGER.info("  │   ├── hotbar - 快捷栏管理");
        Tiandao.LOGGER.info("  │   ├── debug - 调试工具 (OP)");
        Tiandao.LOGGER.info("  │   └── editor - 打开术法编辑器");
        Tiandao.LOGGER.info("  ├── technique - 功法系统");
        Tiandao.LOGGER.info("  ├── practice - 修炼系统");
        Tiandao.LOGGER.info("  └── help - 完整命令帮助");
    }
}
