package org.example.Kangnaixi.tiandao.commands;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.commands.SpellBlueprintCommand;
import org.example.Kangnaixi.tiandao.command.TestRuneCommand;
import org.example.Kangnaixi.tiandao.command.OpenRuneEditorCommand;
import org.example.Kangnaixi.tiandao.command.NodeSpellTestCommand;
import org.example.Kangnaixi.tiandao.command.OpenNodeEditorCommand;
import org.example.Kangnaixi.tiandao.command.NodeSpellDemoCommand;
import org.example.Kangnaixi.tiandao.command.ProjectileTestCommand;
import org.example.Kangnaixi.tiandao.command.SpellDebugCommand;

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
        SpellBlueprintCommand.register(event.getDispatcher());

        // 注册测试命令 /tiandaotest
        TiandaoTestCommand.register(event.getDispatcher());

        // 注册符文测试命令 /testrune
        TestRuneCommand.register(event.getDispatcher());

        // 注册符文编辑器命令 /runeeditor
        OpenRuneEditorCommand.register(event.getDispatcher());

        // 注册节点术法测试命令 /nodespell
        NodeSpellTestCommand.register(event.getDispatcher());

        // 注册节点编辑器命令 /nodeeditor
        OpenNodeEditorCommand.register(event.getDispatcher());

        // 注册节点演示命令 /nodedemo
        NodeSpellDemoCommand.register(event.getDispatcher());

        // 注册弹道测试命令 /projectiletest
        ProjectileTestCommand.register(event.getDispatcher());

        // 修仙术法编辑器命令 /spell_editor
        org.example.Kangnaixi.tiandao.command.SpellEditorCommand.register(event.getDispatcher());

        // 调试命令 /spelldebug
        SpellDebugCommand.register(event.getDispatcher());

        SpellRuntimeCommand.register(event.getDispatcher());

        Tiandao.LOGGER.info("天道修仙系统命令已注册 v" + COMMAND_VERSION);
        Tiandao.LOGGER.info("- 主命令: /tiandao (别名: /cultivation)");
        Tiandao.LOGGER.info("- 测试命令: /tiandaotest");
        Tiandao.LOGGER.info("- 符文测试命令: /testrune");
        Tiandao.LOGGER.info("- 符文编辑器: /runeeditor");
        Tiandao.LOGGER.info("- 节点术法: /nodespell");
        Tiandao.LOGGER.info("- 节点编辑器: /nodeeditor");
        Tiandao.LOGGER.info("- 节点演示: /nodedemo");
        Tiandao.LOGGER.info("- 弹道测试: /projectiletest");
    }
}

