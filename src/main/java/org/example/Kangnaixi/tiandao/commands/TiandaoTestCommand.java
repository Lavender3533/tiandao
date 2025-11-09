package org.example.Kangnaixi.tiandao.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.capability.CultivationCapability;
import org.example.Kangnaixi.tiandao.cultivation.TechniqueChecker;
import org.example.Kangnaixi.tiandao.network.CultivationDataSyncPacket;
import org.example.Kangnaixi.tiandao.network.NetworkHandler;
import org.example.Kangnaixi.tiandao.config.CultivationConfig;

/**
 * 天道修仙系统测试命令
 * 整合所有测试功能到 /tiandaotest 命令下
 */
public class TiandaoTestCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tiandaotest")
            .requires(source -> source.hasPermission(2)) // 需要OP权限
            .executes(TiandaoTestCommand::showHelp)
            
            // === 灵力测试命令 ===
            .then(Commands.literal("spirit")
                .executes(TiandaoTestCommand::showSpiritStatus)
                .then(Commands.literal("set")
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0, 10000))
                        .executes(TiandaoTestCommand::setSpiritPower)))
                .then(Commands.literal("recover")
                    .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 60))
                        .executes(TiandaoTestCommand::simulateRecovery)))
                .then(Commands.literal("info")
                    .executes(TiandaoTestCommand::showRecoveryInfo)))
            
            // === UI测试命令 ===
            .then(Commands.literal("ui")
                .executes(TiandaoTestCommand::showUIStatus)
                .then(Commands.literal("toggle")
                    .then(Commands.literal("hud")
                        .executes(TiandaoTestCommand::toggleHUD))
                    .then(Commands.literal("spiritbar")
                        .executes(TiandaoTestCommand::toggleSpiritBar))
                    .then(Commands.literal("spirittext")
                        .executes(TiandaoTestCommand::toggleSpiritText))
                    .then(Commands.literal("rootinfo")
                        .executes(TiandaoTestCommand::toggleRootInfo))
                    .then(Commands.literal("realminfo")
                        .executes(TiandaoTestCommand::toggleRealmInfo))
                    .then(Commands.literal("recoveryrate")
                        .executes(TiandaoTestCommand::toggleRecoveryRate)))
                .then(Commands.literal("position")
                    .then(Commands.argument("x", IntegerArgumentType.integer(0, 1000))
                        .then(Commands.argument("y", IntegerArgumentType.integer(0, 1000))
                            .executes(TiandaoTestCommand::setHUDPosition))))
                .then(Commands.literal("reset")
                    .executes(TiandaoTestCommand::resetHUDSettings)))
            
            // === Capability测试命令 ===
            .then(Commands.literal("capability")
                .executes(TiandaoTestCommand::showCapabilityInfo)
                .then(Commands.literal("sync")
                    .executes(TiandaoTestCommand::forceSyncCapability))
                .then(Commands.literal("validate")
                    .executes(TiandaoTestCommand::validateCapability)))
            
            // === 灵力密度测试命令 ===
            .then(Commands.literal("density")
                .executes(TiandaoTestCommand::showDensityInfo)
                .then(Commands.literal("set")
                    .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0, 10.0))
                        .executes(TiandaoTestCommand::setDensity))))
            
            // === 强制同步HUD命令 ===
            .then(Commands.literal("synchud")
                .executes(TiandaoTestCommand::forceSyncData))
        );
    }
    
    // ========== 帮助信息 ==========
    
    private static int showHelp(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("=== 天道测试命令 ==="), false);
        context.getSource().sendSuccess(() -> Component.literal("/tiandaotest spirit - 灵力测试命令"), false);
        context.getSource().sendSuccess(() -> Component.literal("/tiandaotest ui - UI测试命令"), false);
        context.getSource().sendSuccess(() -> Component.literal("/tiandaotest capability - Capability测试命令"), false);
        context.getSource().sendSuccess(() -> Component.literal("/tiandaotest density - 查看灵力密度"), false);
        context.getSource().sendSuccess(() -> Component.literal("/tiandaotest density set <value> - 强制设置密度（调试）"), false);
        context.getSource().sendSuccess(() -> Component.literal("/tiandaotest synchud - 强制同步HUD数据"), false);
        return 1;
    }
    
    // ========== 灵力测试命令 ==========
    
    private static int showSpiritStatus(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            double currentPower = cultivation.getSpiritPower();
            double maxPower = cultivation.getMaxSpiritPower();
            double recoveryRate = cultivation.getSpiritPowerRecoveryRate();
            
            player.sendSystemMessage(Component.literal("=== 灵力状态 ==="));
            player.sendSystemMessage(Component.literal("当前灵力: " + String.format("%.1f", currentPower) + " / " + String.format("%.1f", maxPower)));
            player.sendSystemMessage(Component.literal("恢复速率: " + String.format("%.2f", recoveryRate) + " 倍"));
            player.sendSystemMessage(Component.literal("灵根: " + cultivation.getSpiritualRoot().getDisplayName()));
            player.sendSystemMessage(Component.literal("境界: " + cultivation.getRealm().getDisplayName() + " Lv." + cultivation.getLevel()));
        });
        
        return 1;
    }
    
    private static int setSpiritPower(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        double amount = DoubleArgumentType.getDouble(context, "amount");
        
        player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            cultivation.setSpiritPower(amount);
            player.sendSystemMessage(Component.literal("灵力已设置为: " + String.format("%.1f", amount)));
            
            // 同步到客户端
            NetworkHandler.sendToPlayer(new CultivationDataSyncPacket(cultivation), player);
            Tiandao.LOGGER.debug("灵力数据已同步到客户端: " + amount);
        });
        
        return 1;
    }
    
    private static int simulateRecovery(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        int seconds = IntegerArgumentType.getInteger(context, "seconds");
        
        player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            double initialPower = cultivation.getSpiritPower();
            double recoveryRate = cultivation.getSpiritPowerRecoveryRate();
            
            // 获取强度加成
            double intensityBonus = 1.0;
            if (cultivation instanceof CultivationCapability) {
                CultivationCapability cap = (CultivationCapability) cultivation;
                intensityBonus = cap.getIntensityBasedRecoveryBonus(player);
            }
            
            // 模拟恢复
            for (int i = 0; i < seconds * 20; i++) { // 每秒20 ticks
                double recovery = recoveryRate * intensityBonus * 0.1; // 每0.1秒恢复一次
                cultivation.addSpiritPower(recovery);
            }
            
            double finalPower = cultivation.getSpiritPower();
            double recovered = finalPower - initialPower;
            
            player.sendSystemMessage(Component.literal("=== 模拟恢复 " + seconds + " 秒 ==="));
            player.sendSystemMessage(Component.literal("恢复前: " + String.format("%.1f", initialPower)));
            player.sendSystemMessage(Component.literal("恢复后: " + String.format("%.1f", finalPower)));
            player.sendSystemMessage(Component.literal("恢复量: " + String.format("%.1f", recovered)));
            player.sendSystemMessage(Component.literal("基础恢复速率: " + String.format("%.2f", recoveryRate) + " 倍"));
            player.sendSystemMessage(Component.literal("强度加成: " + String.format("%.2f", intensityBonus) + " 倍"));
            player.sendSystemMessage(Component.literal("实际恢复速率: " + String.format("%.2f", recoveryRate * intensityBonus) + " 倍"));
            player.sendSystemMessage(Component.literal("平均每秒恢复: " + String.format("%.2f", recovered / seconds)));
            
            // 同步数据到客户端
            NetworkHandler.sendToPlayer(new CultivationDataSyncPacket(cultivation), player);
        });
        
        return 1;
    }
    
    private static int showRecoveryInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            double recoveryRate = cultivation.getSpiritPowerRecoveryRate();
            
            player.sendSystemMessage(Component.literal("=== 灵力恢复信息 ==="));
            player.sendSystemMessage(Component.literal("总恢复速率: " + String.format("%.2f", recoveryRate) + " 倍"));
            
            // 计算各部分加成
            player.sendSystemMessage(Component.literal("--- 基础加成来源 ---"));
            player.sendSystemMessage(Component.literal("灵根类型加成: " + 
                String.format("%.2f", cultivation.getSpiritualRoot().getRecoveryBonus()) + " 倍"));
            
            player.sendSystemMessage(Component.literal("境界加成: " + 
                String.format("%.2f", cultivation.getRealm().getSpiritRecoveryBonus()) + " 倍"));
            
            // 显示基于灵力强度的恢复机制信息
            if (cultivation instanceof CultivationCapability) {
                CultivationCapability cap = (CultivationCapability) cultivation;
                double intensityBonus = cap.getIntensityBasedRecoveryBonus(player);
                
                player.sendSystemMessage(Component.literal("--- 强度恢复机制 ---"));
                player.sendSystemMessage(Component.literal("强度加成: " + String.format("%.2f", intensityBonus) + " 倍"));
                
                // 显示功法装备状态
                boolean hasBasicTechnique = TechniqueChecker.hasBasicTechniqueEquipped(player);
                player.sendSystemMessage(Component.literal("基础功法装备: " + (hasBasicTechnique ? "已装备" : "未装备")));
                
                if (hasBasicTechnique) {
                    int techniqueLevel = TechniqueChecker.getTechniqueLevel(player);
                    player.sendSystemMessage(Component.literal("功法等级: " + techniqueLevel));
                    
                    // 显示功法等级加成
                    double techniqueBonus = 1.0 + (techniqueLevel - 1) * 0.1;
                    player.sendSystemMessage(Component.literal("功法等级加成: " + String.format("%.2f", techniqueBonus) + " 倍"));
                }
                
                // 显示灵力比例
                double currentPower = cultivation.getSpiritPower();
                double maxPower = cultivation.getMaxSpiritPower();
                double powerRatio = currentPower / maxPower;
                player.sendSystemMessage(Component.literal("灵力比例: " + String.format("%.1f", powerRatio * 100) + "%"));
                
                // 显示实际恢复速率
                double actualRecoveryRate = recoveryRate * intensityBonus;
                player.sendSystemMessage(Component.literal("--- 实际恢复速率 ---"));
                player.sendSystemMessage(Component.literal("实际总恢复速率: " + String.format("%.2f", actualRecoveryRate) + " 倍"));
            }
            
            // 计算恢复时间
            double currentPower = cultivation.getSpiritPower();
            double maxPower = cultivation.getMaxSpiritPower();
            double missingPower = maxPower - currentPower;
            
            if (missingPower > 0) {
                double recoveryPerSecond = recoveryRate * 2.0; // 每秒恢复量
                int secondsToFull = (int) Math.ceil(missingPower / recoveryPerSecond);
                
                player.sendSystemMessage(Component.literal("--- 恢复时间预估 ---"));
                player.sendSystemMessage(Component.literal("当前灵力: " + String.format("%.1f", currentPower) + " / " + String.format("%.1f", maxPower)));
                player.sendSystemMessage(Component.literal("每秒恢复: " + String.format("%.2f", recoveryPerSecond)));
                player.sendSystemMessage(Component.literal("完全恢复需要: " + secondsToFull + " 秒 (" + 
                    String.format("%.1f", secondsToFull / 60.0) + " 分钟)"));
            } else {
                player.sendSystemMessage(Component.literal("灵力已满！"));
            }
        });
        
        return 1;
    }
    
    // ========== UI测试命令 ==========
    
    private static int showUIStatus(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        player.sendSystemMessage(Component.literal("=== 灵力UI状态 ==="));
        player.sendSystemMessage(Component.literal("HUD显示: " + (CultivationConfig.SHOW_HUD.get() ? "开启" : "关闭")));
        player.sendSystemMessage(Component.literal("灵力进度条: " + (CultivationConfig.SHOW_SPIRIT_POWER_BAR.get() ? "开启" : "关闭")));
        player.sendSystemMessage(Component.literal("灵力数值: " + (CultivationConfig.SHOW_SPIRIT_POWER_TEXT.get() ? "开启" : "关闭")));
        player.sendSystemMessage(Component.literal("灵根信息: " + (CultivationConfig.SHOW_SPIRIT_ROOT_INFO.get() ? "开启" : "关闭")));
        player.sendSystemMessage(Component.literal("境界信息: " + (CultivationConfig.SHOW_REALM_INFO.get() ? "开启" : "关闭")));
        player.sendSystemMessage(Component.literal("恢复速率: " + (CultivationConfig.SHOW_RECOVERY_RATE.get() ? "开启" : "关闭")));
        player.sendSystemMessage(Component.literal("HUD位置: (" + CultivationConfig.HUD_X.get() + ", " + CultivationConfig.HUD_Y.get() + ")"));
        
        return 1;
    }
    
    private static int toggleHUD(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        boolean newValue = !CultivationConfig.SHOW_HUD.get();
        CultivationConfig.SHOW_HUD.set(newValue);
        player.sendSystemMessage(Component.literal("HUD显示已" + (newValue ? "开启" : "关闭")));
        return 1;
    }
    
    private static int toggleSpiritBar(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        boolean newValue = !CultivationConfig.SHOW_SPIRIT_POWER_BAR.get();
        CultivationConfig.SHOW_SPIRIT_POWER_BAR.set(newValue);
        player.sendSystemMessage(Component.literal("灵力进度条已" + (newValue ? "开启" : "关闭")));
        return 1;
    }
    
    private static int toggleSpiritText(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        boolean newValue = !CultivationConfig.SHOW_SPIRIT_POWER_TEXT.get();
        CultivationConfig.SHOW_SPIRIT_POWER_TEXT.set(newValue);
        player.sendSystemMessage(Component.literal("灵力数值已" + (newValue ? "开启" : "关闭")));
        return 1;
    }
    
    private static int toggleRootInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        boolean newValue = !CultivationConfig.SHOW_SPIRIT_ROOT_INFO.get();
        CultivationConfig.SHOW_SPIRIT_ROOT_INFO.set(newValue);
        player.sendSystemMessage(Component.literal("灵根信息已" + (newValue ? "开启" : "关闭")));
        return 1;
    }
    
    private static int toggleRealmInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        boolean newValue = !CultivationConfig.SHOW_REALM_INFO.get();
        CultivationConfig.SHOW_REALM_INFO.set(newValue);
        player.sendSystemMessage(Component.literal("境界信息已" + (newValue ? "开启" : "关闭")));
        return 1;
    }
    
    private static int toggleRecoveryRate(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        boolean newValue = !CultivationConfig.SHOW_RECOVERY_RATE.get();
        CultivationConfig.SHOW_RECOVERY_RATE.set(newValue);
        player.sendSystemMessage(Component.literal("恢复速率已" + (newValue ? "开启" : "关闭")));
        return 1;
    }
    
    private static int setHUDPosition(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        int x = IntegerArgumentType.getInteger(context, "x");
        int y = IntegerArgumentType.getInteger(context, "y");
        
        CultivationConfig.HUD_X.set(x);
        CultivationConfig.HUD_Y.set(y);
        
        player.sendSystemMessage(Component.literal("HUD位置已设置为: (" + x + ", " + y + ")"));
        return 1;
    }
    
    private static int resetHUDSettings(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        // 重置所有HUD显示选项为默认开启状态
        CultivationConfig.SHOW_HUD.set(true);
        CultivationConfig.SHOW_SPIRIT_POWER_BAR.set(true);
        CultivationConfig.SHOW_SPIRIT_POWER_TEXT.set(true);
        CultivationConfig.SHOW_SPIRIT_ROOT_INFO.set(true);
        CultivationConfig.SHOW_REALM_INFO.set(true);
        CultivationConfig.SHOW_RECOVERY_RATE.set(true);
        
        // 重置HUD位置为默认值
        CultivationConfig.HUD_X.set(10);
        CultivationConfig.HUD_Y.set(10);
        
        // 保存配置
        CultivationConfig.SPEC.save();
        
        player.sendSystemMessage(Component.literal("=== HUD设置已重置 ==="));
        player.sendSystemMessage(Component.literal("所有HUD显示选项已开启"));
        player.sendSystemMessage(Component.literal("HUD位置已重置为 (10, 10)"));
        
        return 1;
    }
    
    // ========== Capability测试命令 ==========
    
    private static int showCapabilityInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            player.sendSystemMessage(Component.literal("=== Capability信息 ==="));
            player.sendSystemMessage(Component.literal("灵力: " + cultivation.getSpiritPower() + " / " + cultivation.getMaxSpiritPower()));
            player.sendSystemMessage(Component.literal("境界: " + cultivation.getRealm().getDisplayName() + " " + cultivation.getLevel()));
            player.sendSystemMessage(Component.literal("灵根: " + cultivation.getSpiritualRoot().getDisplayName()));
            if (cultivation.getSpiritualRootObject() != null) {
                player.sendSystemMessage(Component.literal("灵根品质: " + cultivation.getSpiritualRootObject().getQuality().getDisplayName()));
            }
            player.sendSystemMessage(Component.literal("修炼进度: " + cultivation.getCultivationProgress()));
        });
        
        return 1;
    }
    
    private static int forceSyncCapability(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            NetworkHandler.sendToPlayer(new CultivationDataSyncPacket(cultivation), player);
            player.sendSystemMessage(Component.literal("Capability数据已强制同步到客户端"));
        });
        
        return 1;
    }
    
    private static int validateCapability(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            player.sendSystemMessage(Component.literal("=== Capability验证 ==="));
            
            // 验证数据完整性
            boolean valid = true;
            if (cultivation.getMaxSpiritPower() <= 0) {
                player.sendSystemMessage(Component.literal("错误: 最大灵力 <= 0"));
                valid = false;
            }
            if (cultivation.getSpiritPower() < 0) {
                player.sendSystemMessage(Component.literal("错误: 当前灵力 < 0"));
                valid = false;
            }
            if (cultivation.getSpiritPower() > cultivation.getMaxSpiritPower()) {
                player.sendSystemMessage(Component.literal("警告: 当前灵力 > 最大灵力"));
            }
            if (cultivation.getRealm() == null) {
                player.sendSystemMessage(Component.literal("错误: 境界为null"));
                valid = false;
            }
            if (cultivation.getSpiritualRoot() == null) {
                player.sendSystemMessage(Component.literal("错误: 灵根为null"));
                valid = false;
            }
            
            if (valid) {
                player.sendSystemMessage(Component.literal("✓ Capability数据有效"));
            } else {
                player.sendSystemMessage(Component.literal("✗ Capability数据存在错误"));
            }
        });
        
        return 1;
    }
    
    // ========== 灵力密度测试 ==========
    
    private static int showDensityInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        // 计算各项密度系数
        double biomeDensity = org.example.Kangnaixi.tiandao.cultivation.SpiritualDensityCalculator.getBiomeDensityMultiplier(player.level(), player.blockPosition());
        double timeDensity = org.example.Kangnaixi.tiandao.cultivation.SpiritualDensityCalculator.getTimeDensityMultiplier(player.level());
        double blockBonus = org.example.Kangnaixi.tiandao.cultivation.SpiritualDensityCalculator.getBlockDensityBonus(player.level(), player.blockPosition());
        double heightMultiplier = org.example.Kangnaixi.tiandao.cultivation.SpiritualDensityCalculator.getHeightDensityMultiplier(player.blockPosition().getY());
        double totalDensity = org.example.Kangnaixi.tiandao.cultivation.SpiritualDensityCalculator.calculateTotalDensity(player);
        
        // 获取生物群系信息
        var biomeHolder = player.level().getBiome(player.blockPosition());
        var biomeKey = biomeHolder.unwrapKey();
        String biomeName = biomeKey.isPresent() ? biomeKey.get().location().toString() : "未知";
        
        // 获取时间信息
        long dayTime = player.level().getDayTime() % 24000;
        long moonPhase = player.level().getMoonPhase();
        String timeStr = String.format("%02d:%02d", (dayTime / 1000 + 6) % 24, (dayTime % 1000) * 60 / 1000);
        String moonStr = moonPhase == 0 ? "满月" : "月相" + moonPhase;
        
        // 显示密度信息
        player.sendSystemMessage(Component.literal("=== 当前位置灵力密度 ==="));
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§6环境信息:"));
        player.sendSystemMessage(Component.literal("  生物群系: " + biomeName));
        player.sendSystemMessage(Component.literal("  坐标: " + player.blockPosition().toShortString()));
        player.sendSystemMessage(Component.literal("  高度: Y=" + player.blockPosition().getY()));
        player.sendSystemMessage(Component.literal("  时间: " + timeStr + " (" + moonStr + ")"));
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§b密度系数:"));
        player.sendSystemMessage(Component.literal(String.format("  生物群系系数: §e×%.2f", biomeDensity)));
        player.sendSystemMessage(Component.literal(String.format("  时间系数: §e×%.2f", timeDensity)));
        player.sendSystemMessage(Component.literal(String.format("  高度系数: §e×%.2f", heightMultiplier)));
        player.sendSystemMessage(Component.literal(String.format("  方块加成: §e+%.2f", blockBonus)));
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal(String.format("§a总灵力密度: §e×%.2f", totalDensity)));
        
        // 根据密度给出评价
        String evaluation;
        if (totalDensity >= 1.5) {
            evaluation = "§a§l优秀 - 修炼圣地！";
        } else if (totalDensity >= 1.2) {
            evaluation = "§a良好 - 适合修炼";
        } else if (totalDensity >= 0.8) {
            evaluation = "§e一般 - 普通环境";
        } else if (totalDensity >= 0.5) {
            evaluation = "§6较差 - 灵气稀薄";
        } else {
            evaluation = "§c极差 - 不宜修炼";
        }
        player.sendSystemMessage(Component.literal("评价: " + evaluation));
        
        return 1;
    }
    
    /**
     * 强制设置灵力密度（调试用）
     */
    private static int setDensity(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        double densityValue = DoubleArgumentType.getDouble(context, "value");
        
        player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            // 设置环境密度
            cultivation.setEnvironmentalDensity(densityValue);
            
            // 同步到客户端
            NetworkHandler.sendToPlayer(new CultivationDataSyncPacket(cultivation), player);
            
            // 显示成功消息
            player.sendSystemMessage(Component.literal("§a已强制设置灵力密度为: §e×" + String.format("%.2f", densityValue)));
            player.sendSystemMessage(Component.literal("§7注意：移动位置或重新登录后将恢复正常计算"));
            
            // 显示影响
            String evaluation;
            if (densityValue >= 1.5) {
                evaluation = "§a§l优秀 - 修炼圣地！";
            } else if (densityValue >= 1.2) {
                evaluation = "§a良好 - 适合修炼";
            } else if (densityValue >= 0.8) {
                evaluation = "§e一般 - 普通环境";
            } else if (densityValue >= 0.5) {
                evaluation = "§6较差 - 灵气稀薄";
            } else {
                evaluation = "§c极差 - 不宜修炼";
            }
            player.sendSystemMessage(Component.literal("评价: " + evaluation));
        });
        
        return 1;
    }
    
    // ========== 强制同步数据 ==========
    
    private static int forceSyncData(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            // 强制同步到客户端
            NetworkHandler.sendToPlayer(new CultivationDataSyncPacket(cultivation), player);
            player.sendSystemMessage(Component.literal("§a已强制同步修仙数据到客户端"));
            
            // 显示同步的数据
            player.sendSystemMessage(Component.literal(String.format("境界: %s %d", 
                cultivation.getRealm().getDisplayName(), cultivation.getLevel())));
            player.sendSystemMessage(Component.literal(String.format("灵力: %.1f / %.1f", 
                cultivation.getSpiritPower(), cultivation.getMaxSpiritPower())));
            player.sendSystemMessage(Component.literal(String.format("灵根: %s", 
                cultivation.getSpiritualRoot().getDisplayName())));
        });
        
        return 1;
    }
}

