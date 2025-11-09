package org.example.Kangnaixi.tiandao.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.cultivation.CultivationRealm;
import org.example.Kangnaixi.tiandao.cultivation.SpiritualRoot;
import org.example.Kangnaixi.tiandao.cultivation.SpiritualRootType;
import org.example.Kangnaixi.tiandao.capability.ICultivation;

/**
 * 修仙状态界面
 * 显示玩家的详细修仙信息
 */
public class CultivationStatusScreen extends Screen {
    private final Player player;
    private static final int PANEL_WIDTH = 256;
    private static final int PANEL_HEIGHT = 280; // 增加高度以容纳修炼信息
    private static final int PADDING = 10;
    private static final int LINE_HEIGHT = 12;
    
    private int panelLeft;
    private int panelTop;
    
    public CultivationStatusScreen(Player player) {
        super(Component.literal("修仙状态"));
        this.player = player;
    }
    
    @Override
    protected void init() {
        super.init();
        // 计算面板位置，使其居中
        this.panelLeft = (this.width - PANEL_WIDTH) / 2;
        this.panelTop = (this.height - PANEL_HEIGHT) / 2;
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        try {
            // 渲染背景
        this.renderBackground(guiGraphics);
        
            // 渲染面板
            renderPanel(guiGraphics);
            
            // 渲染内容
            renderContent(guiGraphics);
            
            // 调用父类渲染（用于渲染按钮等）
            super.render(guiGraphics, mouseX, mouseY, partialTick);
        } catch (Exception e) {
            Tiandao.LOGGER.error("渲染修仙状态界面时出错", e);
        }
    }
    
    /**
     * 渲染面板背景
     */
    private void renderPanel(GuiGraphics guiGraphics) {
        // 绘制半透明背景
        guiGraphics.fill(
            this.panelLeft, 
            this.panelTop, 
            this.panelLeft + PANEL_WIDTH, 
            this.panelTop + PANEL_HEIGHT, 
            0xE0EEEEEE
        );
        
        // 绘制边框
        int borderColor = 0xFF888888;
        guiGraphics.fill(this.panelLeft, this.panelTop, this.panelLeft + PANEL_WIDTH, this.panelTop + 1, borderColor); // 上
        guiGraphics.fill(this.panelLeft, this.panelTop + PANEL_HEIGHT - 1, this.panelLeft + PANEL_WIDTH, this.panelTop + PANEL_HEIGHT, borderColor); // 下
        guiGraphics.fill(this.panelLeft, this.panelTop, this.panelLeft + 1, this.panelTop + PANEL_HEIGHT, borderColor); // 左
        guiGraphics.fill(this.panelLeft + PANEL_WIDTH - 1, this.panelTop, this.panelLeft + PANEL_WIDTH, this.panelTop + PANEL_HEIGHT, borderColor); // 右
    }
    
    /**
     * 渲染内容
     */
    private void renderContent(GuiGraphics guiGraphics) {
        if (player == null) {
            return;
        }
        
        int x = this.panelLeft + PADDING;
        int y = this.panelTop + PADDING;
        
        // 渲染标题
        guiGraphics.drawString(this.font, this.title, x, y, 0xFF404040);
        final int startY = y + LINE_HEIGHT + 5;
        
        // 获取玩家修仙数据
        player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            int currentY = startY;
            
            // 检查是否为凡人
            SpiritualRootType rootType = cultivation.getSpiritualRoot();
            if (rootType == null || rootType == SpiritualRootType.NONE) {
                // 显示凡人提示
                guiGraphics.drawString(this.font, "=== 凡人状态 ===", x, currentY, 0xFF000000);
                currentY += LINE_HEIGHT + 5;
                guiGraphics.drawString(this.font, "你是凡人，无法感知灵气。", x + 10, currentY, 0xFF808080);
                currentY += LINE_HEIGHT;
                guiGraphics.drawString(this.font, "需要获得灵根才能踏入修仙之路。", x + 10, currentY, 0xFF808080);
                return;
            }
            
            // 渲染灵根信息
            currentY = renderSpiritualRootInfo(guiGraphics, cultivation, x, currentY);
            currentY += 5; // 间距
            
            // 渲染境界信息
            currentY = renderRealmInfo(guiGraphics, cultivation, x, currentY);
            currentY += 5; // 间距
            
            // 渲染灵力信息
            currentY = renderSpiritPowerInfo(guiGraphics, cultivation, x, currentY);
            currentY += 5; // 间距
            
            // 渲染修炼信息
            currentY = renderPracticeInfo(guiGraphics, cultivation, x, currentY);
            currentY += 5; // 间距
            
            // 渲染环境信息（仅有灵根的玩家可见）
            renderEnvironmentInfo(guiGraphics, x, currentY);
        });
    }
    
    /**
     * 渲染灵根信息
     */
    private int renderSpiritualRootInfo(GuiGraphics guiGraphics, ICultivation cultivation, int x, int y) {
        // 标题
        guiGraphics.drawString(this.font, "=== 灵根信息 ===", x, y, 0xFF000000);
        y += LINE_HEIGHT;
        
        // 灵根类型
            SpiritualRootType rootType = cultivation.getSpiritualRoot();
        if (rootType != null) {
            int rootColor = rootType.getColor() | 0xFF000000;
            guiGraphics.drawString(this.font, "类型: " + rootType.getDisplayName(), x + 10, y, rootColor);
            y += LINE_HEIGHT;
            
            // 灵根品质
            SpiritualRoot spiritualRoot = cultivation.getSpiritualRootObject();
            if (spiritualRoot != null && spiritualRoot.getQuality() != null) {
                int qualityColor = spiritualRoot.getQuality().getColor() | 0xFF000000;
                String qualityText = "品质: " + spiritualRoot.getQuality().getDisplayName();
                guiGraphics.drawString(this.font, qualityText, x + 10, y, qualityColor);
                y += LINE_HEIGHT;
            
                // 修炼加成
                float bonus = (float) cultivation.getCultivationBonus();
                String bonusText = String.format("修炼加成: %.1f%%", bonus * 100);
                guiGraphics.drawString(this.font, bonusText, x + 10, y, 0xFF404040);
                y += LINE_HEIGHT;
            }
        } else {
            guiGraphics.drawString(this.font, "类型: 无", x + 10, y, 0xFF808080);
            y += LINE_HEIGHT;
        }
        
        return y;
    }
    
    /**
     * 渲染境界信息
     */
    private int renderRealmInfo(GuiGraphics guiGraphics, ICultivation cultivation, int x, int y) {
        // 标题
        guiGraphics.drawString(this.font, "=== 境界信息 ===", x, y, 0xFF000000);
        y += LINE_HEIGHT;
        
        // 境界和等级
            CultivationRealm realm = cultivation.getRealm();
        int level = cultivation.getLevel();
        int realmColor = realm.getColor() | 0xFF000000;
            
        String realmText = "境界: " + realm.getDisplayName() + " " + level + "级";
        guiGraphics.drawString(this.font, realmText, x + 10, y, realmColor);
        y += LINE_HEIGHT;
            
        // 修炼进度
            double progress = cultivation.getCultivationProgress();
        double required = realm.getRequiredProgress(level);
        double percentage = required > 0 ? (progress / required) * 100 : 0;
            
        String progressText = String.format("修炼进度: %.1f%%", percentage);
        guiGraphics.drawString(this.font, progressText, x + 10, y, 0xFF404040);
        y += LINE_HEIGHT;
        
        // 进度条
        int barWidth = PANEL_WIDTH - (PADDING * 2) - 20;
        int barHeight = 6;
        int barX = x + 10;
        int barY = y;
        
        // 进度条背景
        guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF333333);
        
        // 进度条填充
        int fillWidth = (int) ((percentage / 100.0) * barWidth);
        if (fillWidth > 0) {
            guiGraphics.fill(barX, barY, barX + fillWidth, barY + barHeight, realmColor);
        }
        
        // 进度条边框
        guiGraphics.fill(barX - 1, barY - 1, barX + barWidth + 1, barY, 0xFF000000);
        guiGraphics.fill(barX - 1, barY + barHeight, barX + barWidth + 1, barY + barHeight + 1, 0xFF000000);
        guiGraphics.fill(barX - 1, barY, barX, barY + barHeight, 0xFF000000);
        guiGraphics.fill(barX + barWidth, barY, barX + barWidth + 1, barY + barHeight, 0xFF000000);
        
        y += barHeight + 5;
        
        return y;
    }
    
    /**
     * 渲染灵力信息
     */
    private int renderSpiritPowerInfo(GuiGraphics guiGraphics, ICultivation cultivation, int x, int y) {
        // 标题
        guiGraphics.drawString(this.font, "=== 灵力信息 ===", x, y, 0xFF000000);
        y += LINE_HEIGHT;
        
        // 灵力值
        double current = cultivation.getCurrentSpiritPower();
        double max = cultivation.getMaxSpiritPower();
        double percentage = max > 0 ? (current / max) * 100 : 0;
        
        String powerText = String.format("灵力: %.1f / %.1f (%.1f%%)", current, max, percentage);
        
        // 根据灵力百分比选择颜色
        int powerColor = 0xFF404040;
        if (percentage >= 80) {
            powerColor = 0xFF00AA00; // 绿色
        } else if (percentage >= 50) {
            powerColor = 0xFFAAAA00; // 黄色
        } else if (percentage < 30) {
            powerColor = 0xFFAA0000; // 红色
        }
            
        guiGraphics.drawString(this.font, powerText, x + 10, y, powerColor);
        y += LINE_HEIGHT;
        
        // 恢复速率
        double recoveryRate = cultivation.getSpiritPowerRecoveryRate();
        String recoveryText = String.format("恢复速率: %.2fx", recoveryRate);
        guiGraphics.drawString(this.font, recoveryText, x + 10, y, 0xFF404040);
        y += LINE_HEIGHT;
        
        return y;
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
    
    /**
     * 渲染环境信息（灵力密度）
     */
    private void renderEnvironmentInfo(GuiGraphics guiGraphics, int x, int y) {
        // 标题
        guiGraphics.drawString(this.font, "=== 环境信息 ===", x, y, 0xFF000000);
        y += LINE_HEIGHT;
        
        // 从玩家的Capability获取环境密度（已从服务器同步）
        final int densityY = y;
        player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            double environmentalDensity = cultivation.getEnvironmentalDensity();
            
            // 显示当前环境灵力密度
            String densityText = String.format("当前灵力密度: %.2fx", environmentalDensity);
            int densityColor = getDensityColor(environmentalDensity);
            guiGraphics.drawString(this.font, densityText, x + 10, densityY, densityColor);
        });
        y += LINE_HEIGHT;
        
        // 显示密度评价
        final int evalY = y;
        player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
            double density = cultivation.getEnvironmentalDensity();
            String evaluation = getDensityEvaluation(density);
            guiGraphics.drawString(this.font, evaluation, x + 10, evalY, 0xFF808080);
        });
        y += LINE_HEIGHT;
        y += 3; // 间距
        
        // 显示密度影响说明
        guiGraphics.drawString(this.font, "密度影响因素:", x + 10, y, 0xFF404040);
        y += LINE_HEIGHT;
        guiGraphics.drawString(this.font, "· 生物群系：高山、森林灵气充沛", x + 15, y, 0xFF00AA00);
        y += LINE_HEIGHT;
        guiGraphics.drawString(this.font, "· 时间：夜晚更佳，满月最好", x + 15, y, 0xFF5555FF);
        y += LINE_HEIGHT;
        guiGraphics.drawString(this.font, "· 高度：高山增强，地下减弱", x + 15, y, 0xFF888888);
        y += LINE_HEIGHT;
        guiGraphics.drawString(this.font, "· 使用命令 /tiandaotest density", x + 15, y, 0xFF666666);
        y += LINE_HEIGHT;
        guiGraphics.drawString(this.font, "  查看详细系数", x + 15, y, 0xFF666666);
    }
    
    /**
     * 根据密度获取显示颜色
     */
    private int getDensityColor(double density) {
        if (density >= 1.5) return 0xFF00AA00;      // 绿色 - 优秀
        if (density >= 1.2) return 0xFF55AA55;      // 浅绿 - 良好
        if (density >= 0.8) return 0xFFAAAA00;      // 黄色 - 一般
        if (density >= 0.5) return 0xFFAA5500;      // 橙色 - 较差
        return 0xFFAA0000;                           // 红色 - 极差
    }
    
    /**
     * 根据密度获取评价文本
     */
    private String getDensityEvaluation(double density) {
        if (density >= 1.5) return "评价: 优秀 - 修炼圣地！";
        if (density >= 1.2) return "评价: 良好 - 适合修炼";
        if (density >= 0.8) return "评价: 一般 - 普通环境";
        if (density >= 0.5) return "评价: 较差 - 灵气稀薄";
        return "评价: 极差 - 不宜修炼";
    }
    
    /**
     * 渲染修炼信息
     */
    private int renderPracticeInfo(GuiGraphics guiGraphics, ICultivation cultivation, int x, int y) {
        // 标题
        guiGraphics.drawString(this.font, "=== 修炼信息 ===", x, y, 0xFF000000);
        y += LINE_HEIGHT;
        
        // 修炼状态
        if (cultivation.isPracticing()) {
            // 获取修炼方式名称
            String methodId = cultivation.getCurrentPracticeMethod();
            org.example.Kangnaixi.tiandao.practice.PracticeMethod method = 
                org.example.Kangnaixi.tiandao.practice.PracticeRegistry.getInstance().getPracticeMethod(methodId);
            
            String methodName = (method != null) ? method.getDisplayName() : methodId;
            
            // 手动处理颜色
            guiGraphics.drawString(this.font, "状态: ", x + 10, y, 0xFF404040);
            int textWidth = this.font.width("状态: ");
            guiGraphics.drawString(this.font, "修炼中 (" + methodName + ")", x + 10 + textWidth, y, 0xFF00AA00);
        } else {
            // 检查是否在战斗状态
            if (cultivation.isInCombat()) {
                guiGraphics.drawString(this.font, "状态: ", x + 10, y, 0xFF404040);
                int textWidth = this.font.width("状态: ");
                guiGraphics.drawString(this.font, "战斗中 (无法修炼)", x + 10 + textWidth, y, 0xFFAA0000);
            } else {
                guiGraphics.drawString(this.font, "状态: 未在修炼", x + 10, y, 0xFF808080);
            }
        }
        y += LINE_HEIGHT;
        
        // 修炼经验
        int currentExp = cultivation.getCultivationExperience();
        int requiredExp = cultivation.getRequiredExperienceForLevel();
        double expPercentage = requiredExp > 0 ? (double) currentExp / requiredExp * 100 : 0;
        
        String expText = String.format("修炼经验: %d / %d (%.1f%%)", currentExp, requiredExp, expPercentage);
        int expColor = expPercentage >= 100 ? 0xFFFFD700 : 0xFF404040;
        guiGraphics.drawString(this.font, expText, x + 10, y, expColor);
        y += LINE_HEIGHT;
        
        // 经验进度条
        int barWidth = PANEL_WIDTH - (PADDING * 2) - 20;
        int barHeight = 6;
        int barX = x + 10;
        int barY = y;
        
        // 进度条背景
        guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF222222);
        
        // 进度条填充
        int fillWidth = (int) ((expPercentage / 100.0) * barWidth);
        if (fillWidth > 0) {
            // 根据进度选择颜色
            int barColor;
            if (expPercentage >= 100) {
                barColor = 0xFFFFD700; // 金色 - 已满
            } else if (expPercentage >= 75) {
                barColor = 0xFFFFCC00; // 橙黄色
            } else if (expPercentage >= 50) {
                barColor = 0xFFFFFF00; // 黄色
            } else {
                barColor = 0xFFAAAA00; // 暗黄色
            }
            guiGraphics.fill(barX, barY, barX + fillWidth, barY + barHeight, barColor);
        }
        
        // 进度条边框
        guiGraphics.fill(barX - 1, barY - 1, barX + barWidth + 1, barY, 0xFF000000);
        guiGraphics.fill(barX - 1, barY + barHeight, barX + barWidth + 1, barY + barHeight + 1, 0xFF000000);
        guiGraphics.fill(barX - 1, barY, barX, barY + barHeight, 0xFF000000);
        guiGraphics.fill(barX + barWidth, barY, barX + barWidth + 1, barY + barHeight, 0xFF000000);
        
        y += barHeight + 5;
        
        // 如果经验已满，提示可突破
        if (expPercentage >= 100) {
            guiGraphics.drawString(this.font, "§e⚡ 经验已满，可尝试突破！", x + 10, y, 0xFFFFAA00);
            y += LINE_HEIGHT;
        }
        
        return y;
    }
    
    /**
     * 打开修仙状态界面
     */
    public static void open() {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player != null) {
            minecraft.setScreen(new CultivationStatusScreen(player));
        }
    }
}
