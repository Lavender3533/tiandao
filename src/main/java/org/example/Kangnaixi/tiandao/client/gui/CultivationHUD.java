package org.example.Kangnaixi.tiandao.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.capability.ICultivation;
import org.example.Kangnaixi.tiandao.config.CultivationConfig;
import org.example.Kangnaixi.tiandao.cultivation.FoundationSystem;
import org.example.Kangnaixi.tiandao.cultivation.SpiritualRoot;
import org.example.Kangnaixi.tiandao.cultivation.SpiritualRootType;
import org.example.Kangnaixi.tiandao.cultivation.SubRealm;

/**
 * 修仙系统HUD渲染器
 * 用于在游戏界面上显示灵力信息
 * 
 * 重要：本类所有方法均为静态方法，直接使用 Forge 提供的 GuiGraphics 实例进行渲染
 */
public class CultivationHUD {
    private static final int BAR_WIDTH = 100; // 进度条宽度
    private static final int BAR_HEIGHT = 8; // 进度条高度
    private static final int TEXT_COLOR = 0xFFFFFF; // 文本颜色（白色）
    private static final int BACKGROUND_COLOR = 0x80000000; // 背景颜色（半透明黑色）
    private static final int BORDER_COLOR = 0xFF000000; // 边框颜色（黑色）
    private static final int LINE_HEIGHT = 12; // 文本行高
    
    private static boolean collapsed = true;
    
    public static void toggleCollapsed() {
        setCollapsed(!collapsed);
    }
    
    public static void setCollapsed(boolean value) {
        collapsed = value;
    }
    
    public static boolean isCollapsed() {
        return collapsed;
    }
    
    private static int getSpiritPowerTextLines() {
        return CultivationConfig.SHOW_SPIRIT_POWER_TEXT.get() ? 1 : 0;
    }
    
    private static int getSpiritualRootLines() {
        return (!collapsed && CultivationConfig.SHOW_SPIRIT_ROOT_INFO.get()) ? 2 : 0;
    }
    
    private static int getRealmLines() {
        return CultivationConfig.SHOW_REALM_INFO.get() ? 1 : 0;
    }
    
    private static int getFoundationLines() {
        return (!collapsed && CultivationConfig.SHOW_FOUNDATION_INFO.get()) ? 1 : 0;
    }
    
    private static int getRecoveryLines() {
        return (!collapsed && CultivationConfig.SHOW_RECOVERY_RATE.get()) ? 1 : 0;
    }
    
    private static int getPracticeLines(ICultivation cultivation) {
        if (collapsed) {
            return 0;
        }
        return (cultivation != null && cultivation.isPracticing()) ? 2 : 1;
    }
    
    private static int getLineOffsetBeforeRoot() {
        return getSpiritPowerTextLines();
    }
    
    private static int getLineOffsetBeforeRealm() {
        return getLineOffsetBeforeRoot() + getSpiritualRootLines();
    }
    
    private static int getLineOffsetBeforeFoundation() {
        return getLineOffsetBeforeRealm() + getRealmLines();
    }
    
    private static int getLineOffsetBeforeRecovery() {
        return getLineOffsetBeforeFoundation() + getFoundationLines();
    }
    
    private static int getLineOffsetBeforePractice() {
        return getLineOffsetBeforeRecovery() + getRecoveryLines();
    }
    
    private static int computeTextY(int hudY, int lineOffset) {
        return hudY + BAR_HEIGHT + 3 + (LINE_HEIGHT * lineOffset);
    }
    
    /**
     * 渲染HUD主入口
     * 注意：直接使用传入的 guiGraphics 参数，不要创建新实例
     * 
     * @param guiGraphics Forge 提供的 GuiGraphics 实例
     * @param partialTick 部分tick
     */
    public static void render(GuiGraphics guiGraphics, float partialTick) {
        try {
        // 检查是否应该显示HUD
        if (!CultivationConfig.SHOW_HUD.get()) {
            return;
        }
        
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        
        if (player == null) {
            return;
        }
        
        // 获取玩家修仙能力
        player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            // 检查玩家是否有灵根
            SpiritualRootType rootType = cultivation.getSpiritualRoot();
            if (rootType == null || rootType == SpiritualRootType.NONE) {
                // 凡人无法感知灵气，检查配置是否允许显示
                if (!org.example.Kangnaixi.tiandao.Config.mortalShowHud) {
                return;
                }
                // 如果配置允许，继续显示（调试模式）
            }
            
            // 清理不再存在的玩家的动画状态
            HudAnimationManager.getInstance().cleanup();
            
            Font font = minecraft.font;
            
            // 获取灵根颜色（如果是凡人调试模式，使用灰色）
            int rootColor = (rootType == null || rootType == SpiritualRootType.NONE) ? 
                0xFF888888 : rootType.getColor();
            
            // 获取HUD位置
            int hudX = CultivationConfig.HUD_X.get();
            int hudY = CultivationConfig.HUD_Y.get();
            
            // 绘制背景
                drawBackground(guiGraphics, hudX, hudY, cultivation);
            
            // 根据配置决定是否显示各个元素
            if (CultivationConfig.SHOW_SPIRIT_POWER_BAR.get()) {
                // 绘制灵力进度条
                    renderSpiritPowerBar(guiGraphics, cultivation, rootColor, hudX, hudY, font);
            }
            
            if (CultivationConfig.SHOW_SPIRIT_POWER_TEXT.get()) {
                // 绘制灵力数值
                    renderSpiritPowerText(guiGraphics, font, cultivation, rootColor, hudX, hudY);
            }
            
            if (!collapsed && CultivationConfig.SHOW_SPIRIT_ROOT_INFO.get()) {
                // 绘制灵根信息
                    renderSpiritualRootText(guiGraphics, font, cultivation, rootColor, hudX, hudY);
            }
            
            if (CultivationConfig.SHOW_REALM_INFO.get()) {
                // 绘制境界信息
                    renderRealmText(guiGraphics, font, cultivation, hudX, hudY);
            }
            
            if (!collapsed && CultivationConfig.SHOW_FOUNDATION_INFO.get()) {
                renderFoundationText(guiGraphics, font, cultivation, hudX, hudY);
            }
            
            if (!collapsed && CultivationConfig.SHOW_RECOVERY_RATE.get()) {
                // 绘制恢复速率信息
                    renderRecoveryRateText(guiGraphics, font, cultivation, hudX, hudY);
            }
            
            if (!collapsed) {
                // ?????????????????
                renderPracticeStatus(guiGraphics, font, cultivation, hudX, hudY);
            }
        });
        } catch (Exception e) {
            // 捕获渲染异常，避免游戏崩溃
            Tiandao.LOGGER.error("HUD 渲染时发生错误", e);
        }
    }
    
    /**
     * 绘制HUD背景
     * 
     * @param guiGraphics GuiGraphics 实例
     * @param hudX HUD X 坐标
     * @param hudY HUD Y 坐标
     * @param cultivation 修仙能力
     */
    private static void drawBackground(GuiGraphics guiGraphics, int hudX, int hudY, ICultivation cultivation) {
        int lineCount = getSpiritPowerTextLines();
        lineCount += getSpiritualRootLines();
        lineCount += getRealmLines();
        lineCount += getFoundationLines();
        lineCount += getRecoveryLines();
        lineCount += getPracticeLines(cultivation);

        int bgHeight = BAR_HEIGHT + (LINE_HEIGHT * lineCount) + 4;

        int left = hudX - 2;
        int top = hudY - 2;
        int right = hudX + BAR_WIDTH + 2;
        int bottom = hudY + bgHeight;

        int topColor = 0x80222222;
        int bottomColor = 0x80111111;
        guiGraphics.fillGradient(left, top, right, bottom, topColor, bottomColor);

        int vignetteAlpha = 0x30000000;
        guiGraphics.fill(left, top, right, top + 2, vignetteAlpha);
        guiGraphics.fill(left, bottom - 2, right, bottom, vignetteAlpha);
        guiGraphics.fill(left, top, left + 2, bottom, vignetteAlpha);
        guiGraphics.fill(right - 2, top, right, bottom, vignetteAlpha);
    }
    
    /**
     * 渲染灵力进度条
     * 
     * @param guiGraphics GuiGraphics 实例
     * @param cultivation 修仙能力
     * @param color 灵根颜色
     * @param hudX HUD X 坐标
     * @param hudY HUD Y 坐标
     * @param font 字体
     */
    private static void renderSpiritPowerBar(GuiGraphics guiGraphics, ICultivation cultivation, 
                                              int color, int hudX, int hudY, Font font) {
        // 获取实际值
        double targetPower = cultivation.getCurrentSpiritPower();
        double targetMaxPower = cultivation.getMaxSpiritPower();
        
        // 获取平滑过渡后的显示值
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        float partialTick = mc.getFrameTime();
        double displayPower = HudAnimationManager.getInstance().getSmoothSpiritPower(
            mc.player.getUUID(), targetPower, partialTick);
        double displayMaxPower = HudAnimationManager.getInstance().getSmoothMaxSpiritPower(
            mc.player.getUUID(), targetMaxPower, partialTick);
        
        // 计算填充宽度（使用平滑过渡后的值）
        int fillWidth = displayMaxPower > 0 ? (int) ((displayPower / displayMaxPower) * BAR_WIDTH) : 0;
        
        // 绘制进度条边框
        guiGraphics.fill(hudX - 1, hudY - 1, hudX + BAR_WIDTH + 1, hudY, BORDER_COLOR); // 上边框
        guiGraphics.fill(hudX - 1, hudY + BAR_HEIGHT, hudX + BAR_WIDTH + 1, hudY + BAR_HEIGHT + 1, BORDER_COLOR); // 下边框
        guiGraphics.fill(hudX - 1, hudY, hudX, hudY + BAR_HEIGHT, BORDER_COLOR); // 左边框
        guiGraphics.fill(hudX + BAR_WIDTH, hudY, hudX + BAR_WIDTH + 1, hudY + BAR_HEIGHT, BORDER_COLOR); // 右边框
        
        // 绘制进度条背景
        guiGraphics.fill(hudX, hudY, hudX + BAR_WIDTH, hudY + BAR_HEIGHT, 0xFF333333);
        
        // 绘制进度条填充 - 根据灵力强度调整颜色（使用平滑过渡后的值）
        if (fillWidth > 0) {
            double intensity = displayMaxPower > 0 ? displayPower / displayMaxPower : 0;
            int barColor = getIntensityBasedColor(color, intensity);
            guiGraphics.fill(hudX, hudY, hudX + fillWidth, hudY + BAR_HEIGHT, barColor | 0xFF000000);
        }
    }
    
    /**
     * 渲染灵力数值文本
     * 
     * @param guiGraphics GuiGraphics 实例
     * @param font 字体
     * @param cultivation 修仙能力
     * @param color 灵根颜色
     * @param hudX HUD X 坐标
     * @param hudY HUD Y 坐标
     */
    private static void renderSpiritPowerText(GuiGraphics guiGraphics, Font font, ICultivation cultivation, 
                                               int color, int hudX, int hudY) {
        // 获取实际值
        double targetPower = cultivation.getCurrentSpiritPower();
        double targetMaxPower = cultivation.getMaxSpiritPower();
        
        // 获取平滑过渡后的显示值
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        float partialTick = mc.getFrameTime();
        double displayPower = HudAnimationManager.getInstance().getSmoothSpiritPower(
            mc.player.getUUID(), targetPower, partialTick);
        double displayMaxPower = HudAnimationManager.getInstance().getSmoothMaxSpiritPower(
            mc.player.getUUID(), targetMaxPower, partialTick);
        
        // 格式化灵力值文本（使用平滑过渡后的值）
        String powerText = String.format("灵力: %.1f/%.1f", displayPower, displayMaxPower);
        
        // 根据灵力强度调整颜色（使用平滑过渡后的值）
        double intensity = displayMaxPower > 0 ? displayPower / displayMaxPower : 0;
        int textColor = getIntensityBasedColor(color, intensity) | 0xFF000000;
        
        // 绘制灵力值文本（在进度条下方）
        int textY = hudY + BAR_HEIGHT + 3;
        drawTextWithShadow(guiGraphics, font, powerText, hudX, textY, textColor);
    }
    
    /**
     * 渲染灵根信息文本
     * 
     * @param guiGraphics GuiGraphics 实例
     * @param font 字体
     * @param cultivation 修仙能力
     * @param color 灵根颜色
     * @param hudX HUD X 坐标
     * @param hudY HUD Y 坐标
     */
    private static void renderSpiritualRootText(GuiGraphics guiGraphics, Font font, ICultivation cultivation, 
                                                 int color, int hudX, int hudY) {
        // 获取灵根类型
        SpiritualRootType rootType = cultivation.getSpiritualRoot();
        String rootTypeName = rootType.getDisplayName();
        
        // 获取灵根品质
                SpiritualRoot spiritualRoot = cultivation.getSpiritualRootObject();
        String rootQuality = "未知";
        int qualityColor = TEXT_COLOR;
        
        if (spiritualRoot != null && spiritualRoot.getQuality() != null) {
            rootQuality = spiritualRoot.getQuality().getDisplayName();
            qualityColor = spiritualRoot.getQuality().getColor() | 0xFF000000;
        }
                
                // 计算文本位置（在灵力数值下方）
                int lineOffset = getLineOffsetBeforeRoot();
                int textY = computeTextY(hudY, lineOffset);
                
                // 绘制灵根类型文本
        drawTextWithShadow(guiGraphics, font, "灵根: " + rootTypeName, hudX, textY, color | 0xFF000000);
                
                // 绘制灵根品质文本
        drawTextWithShadow(guiGraphics, font, "品质: " + rootQuality, hudX, textY + LINE_HEIGHT, qualityColor);
    }
    
    /**
     * 渲染境界信息文本
     * 
     * @param guiGraphics GuiGraphics 实例
     * @param font 字体
     * @param cultivation 修仙能力
     * @param hudX HUD X 坐标
     * @param hudY HUD Y 坐标
     */
    private static void renderRealmText(GuiGraphics guiGraphics, Font font, ICultivation cultivation, 
                                         int hudX, int hudY) {
        // 获取境界、小境界和等级
        String realmName = cultivation.getRealm().getDisplayName();
        SubRealm subRealm = cultivation.getSubRealm();
        String subRealmName = subRealm != null ? subRealm.getDisplayName() : null;
        
        // 使用境界自己的颜色
        int realmColor = cultivation.getRealm().getColor() | 0xFF000000;
        
        // 计算文本位置（根据已显示的元素动态调整）
        int lineOffset = getLineOffsetBeforeRealm();
        int textY = computeTextY(hudY, lineOffset);
        
        // 绘制境界文本（追加小境界显示）
        String realmText = "境界: " + realmName;
        if (subRealmName != null) {
            realmText += " " + subRealmName;
        }
        // 旧的等级系统已淘汰，不再显示 Lv.
        drawTextWithShadow(guiGraphics, font, realmText, hudX, textY, realmColor);
    }
    
    /**
     * 渲染根基信息文本
     */
    private static void renderFoundationText(GuiGraphics guiGraphics, Font font, ICultivation cultivation,
                                             int hudX, int hudY) {
        int foundation = cultivation.getFoundation();
        FoundationSystem.FoundationDescriptor descriptor = FoundationSystem.describeFoundation(foundation);
        
        int lineOffset = getLineOffsetBeforeFoundation();
        int textY = computeTextY(hudY, lineOffset);
        String foundationText = String.format("根基: %d (%s)", foundation, descriptor.label());
        drawTextWithShadow(guiGraphics, font, foundationText, hudX, textY, descriptor.color() | 0xFF000000);
    }
    
    /**
     * 渲染灵力恢复速率信息文本
     * 
     * @param guiGraphics GuiGraphics 实例
     * @param font 字体
     * @param cultivation 修仙能力
     * @param hudX HUD X 坐标
     * @param hudY HUD Y 坐标
     */
    private static void renderRecoveryRateText(GuiGraphics guiGraphics, Font font, ICultivation cultivation, 
                                                int hudX, int hudY) {
        // 获取基础恢复速率和环境密度
        double baseRate = cultivation.getSpiritPowerRecoveryRate();
        double environmentalDensity = cultivation.getEnvironmentalDensity();
        double actualRate = baseRate * environmentalDensity;
        
        // 计算文本位置（根据已显示的元素动态调整）
        int lineOffset = getLineOffsetBeforeRecovery();
        int textY = computeTextY(hudY, lineOffset);
        
        // 获取恢复速率对应的颜色
        int recoveryColor = getRecoveryRateColor(actualRate) | 0xFF000000;
        
        // 绘制恢复速率文本，显示实际恢复速率和环境系数
        String recoveryText;
        if (environmentalDensity != 1.0) {
            // 如果环境密度不是1.0，显示详细信息
            recoveryText = String.format("恢复: %.2fx (环境×%.2f)", actualRate, environmentalDensity);
        } else {
            // 标准环境，只显示总速率
            recoveryText = String.format("恢复: %.2fx", actualRate);
        }
        drawTextWithShadow(guiGraphics, font, recoveryText, hudX, textY, recoveryColor);
    }
    
    /**
     * 根据灵力强度调整颜色
     * 
     * @param baseColor 基础颜色（灵根颜色）
     * @param intensity 灵力强度比例（0.0-1.0）
     * @return 调整后的颜色（不包含 alpha 通道）
     */
    private static int getIntensityBasedColor(int baseColor, double intensity) {
        // 提取RGB分量
        int r = (baseColor >> 16) & 0xFF;
        int g = (baseColor >> 8) & 0xFF;
        int b = baseColor & 0xFF;
        
        // 根据灵力强度调整亮度
        // 灵力低时颜色偏暗，灵力高时颜色明亮
        float brightness = 0.5f + (float)intensity * 0.5f;
        
        // 应用亮度调整
        r = Math.min(255, (int)(r * brightness));
        g = Math.min(255, (int)(g * brightness));
        b = Math.min(255, (int)(b * brightness));
        
        // 特殊情况：灵力极低时添加红色警告色调
        if (intensity < 0.2) {
            r = Math.min(255, r + 50);
        }
        
        return (r << 16) | (g << 8) | b;
    }
    
    /**
     * 根据恢复速率获取对应的颜色
     * 
     * @param recoveryRate 恢复速率
     * @return 对应的颜色（不包含 alpha 通道）
     */
    private static int getRecoveryRateColor(double recoveryRate) {
        // 根据恢复速率返回不同颜色
        if (recoveryRate >= 2.0) {
            return 0x00FF00; // 绿色 - 高恢复速率
        } else if (recoveryRate >= 1.5) {
            return 0x55FF55; // 浅绿色 - 中高恢复速率
        } else if (recoveryRate >= 1.0) {
            return 0xFFFFFF; // 白色 - 正常恢复速率
        } else if (recoveryRate >= 0.5) {
            return 0xFFFF55; // 黄色 - 低恢复速率
        } else {
            return 0xFF5555; // 红色 - 极低恢复速率
        }
    }
    
    /**
     * 渲染修炼状态和经验条
     * 
     * @param guiGraphics GuiGraphics 实例
     * @param font 字体
     * @param cultivation 修仙能力
     * @param hudX HUD X 坐标
     * @param hudY HUD Y 坐标
     */
    private static void renderPracticeStatus(GuiGraphics guiGraphics, Font font, ICultivation cultivation, 
                                              int hudX, int hudY) {
        // 计算文本位置（根据已显示的元素动态调整）
        int lineOffset = getLineOffsetBeforePractice();
        int textY = computeTextY(hudY, lineOffset);
        
        // 显示修炼状态
        if (cultivation.isPracticing()) {
            // 获取修炼方式名称
            String methodId = cultivation.getCurrentPracticeMethod();
            org.example.Kangnaixi.tiandao.practice.PracticeMethod method = 
                org.example.Kangnaixi.tiandao.practice.PracticeRegistry.getInstance().getPracticeMethod(methodId);
            
            String methodName = (method != null) ? method.getDisplayName() : methodId;
            String practiceText = "修炼中: " + methodName;
            
            // 闪烁效果（每秒闪烁一次）
            long time = System.currentTimeMillis();
            boolean shouldBlink = (time / 500) % 2 == 0;
            int practiceColor = shouldBlink ? 0xFFFFAA00 : 0xFFFFCC00; // 橙色闪烁
            
            drawTextWithShadow(guiGraphics, font, practiceText, hudX, textY, practiceColor);
            textY += LINE_HEIGHT;
        }
        
        // 显示修炼经验条
        int currentExp = cultivation.getCultivationExperience();
        int requiredExp = cultivation.getRequiredExperienceForSubRealm();
        if (requiredExp <= 0) {
            requiredExp = cultivation.getRequiredExperienceForLevel();
        }
        
        if (requiredExp > 0) {
            // 绘制经验文本
            String expText = String.format("修炼经验: %d/%d", currentExp, requiredExp);
            drawTextWithShadow(guiGraphics, font, expText, hudX, textY, 0xFFAAFF00);
            textY += LINE_HEIGHT + 2;
            
            // 绘制经验条
            int expBarWidth = BAR_WIDTH;
            int expBarHeight = 6;
            int fillWidth = (int) (Math.min((double) currentExp / requiredExp, 1.0) * expBarWidth);
            
            // 边框
            guiGraphics.fill(hudX - 1, textY - 1, hudX + expBarWidth + 1, textY, BORDER_COLOR);
            guiGraphics.fill(hudX - 1, textY + expBarHeight, hudX + expBarWidth + 1, textY + expBarHeight + 1, BORDER_COLOR);
            guiGraphics.fill(hudX - 1, textY, hudX, textY + expBarHeight, BORDER_COLOR);
            guiGraphics.fill(hudX + expBarWidth, textY, hudX + expBarWidth + 1, textY + expBarHeight, BORDER_COLOR);
            
            // 背景
            guiGraphics.fill(hudX, textY, hudX + expBarWidth, textY + expBarHeight, 0xFF222222);
            
            // 填充 - 渐变颜色（从黄色到金色）
            if (fillWidth > 0) {
                double progress = (double) currentExp / requiredExp;
                int expColor;
                if (progress >= 1.0) {
                    expColor = 0xFFFFD700; // 金色 - 已满
                } else if (progress >= 0.75) {
                    expColor = 0xFFFFCC00; // 橙黄色
                } else if (progress >= 0.5) {
                    expColor = 0xFFFFFF00; // 黄色
                } else {
                    expColor = 0xFFAAAA00; // 暗黄色
                }
                guiGraphics.fill(hudX, textY, hudX + fillWidth, textY + expBarHeight, expColor);
            }
            
            textY += expBarHeight + 4;
        }
        
        // 显示装备的功法信息
        if (cultivation.hasEquippedTechnique()) {
            org.example.Kangnaixi.tiandao.technique.TechniqueData technique = cultivation.getEquippedTechnique();
            if (technique != null) {
                String techniqueName = String.format("§7功法: §e%s §7Lv.%d", technique.getName(), technique.getLevel());
                drawTextWithShadow(guiGraphics, font, techniqueName, hudX, textY, 0xFFFFFFFF);
                textY += LINE_HEIGHT;
                String efficiencyText = String.format("§7效率: §a%.1f%%", technique.getEfficiencyBonus() * 100);
                drawTextWithShadow(guiGraphics, font, efficiencyText, hudX, textY, 0xFFFFFFFF);
            }
        }
    }

    private static void drawTextWithShadow(GuiGraphics g, Font font, String text, int x, int y, int color) {
        int shadow = 0x40000000;
        g.drawString(font, text, x + 1, y + 1, shadow);
        g.drawString(font, text, x, y, color);
    }
    
}
