package org.example.Kangnaixi.tiandao.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.capability.ICultivation;
import org.example.Kangnaixi.tiandao.spell.SpellData;
import org.example.Kangnaixi.tiandao.spell.SpellRegistry;

/**
 * 术法系统 HUD 渲染器
 * 用于在游戏界面上显示术法快捷栏和状态
 */
public class SpellHudOverlay {
    private static final int SLOT_SIZE = 24; // 槽位大小
    private static final int SLOT_SPACING = 4; // 槽位间距
    private static final int SLOT_COUNT = 4; // 槽位数量
    private static final int BACKGROUND_COLOR = 0x80000000; // 背景颜色（半透明黑色）
    private static final int BORDER_COLOR = 0xFF000000; // 边框颜色（黑色）
    private static final int ACTIVE_BORDER_COLOR = 0xFF00FF00; // 激活槽位边框（绿色）
    private static final int COOLDOWN_COLOR = 0x80FF0000; // 冷却遮罩颜色（半透明红色）
    private static final int TEXT_COLOR = 0xFFFFFF; // 文本颜色（白色）
    
    /**
     * 渲染术法 HUD 主入口
     * 
     * @param guiGraphics GuiGraphics 实例
     * @param partialTick 部分 tick
     */
    public static void render(GuiGraphics guiGraphics, float partialTick) {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            Player player = minecraft.player;
            
            if (player == null || minecraft.options.hideGui) {
                return;
            }
            
            // 获取玩家修仙能力
            player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
                Font font = minecraft.font;
                
                // 计算 HUD 位置（屏幕底部中央偏右）
                int screenWidth = minecraft.getWindow().getGuiScaledWidth();
                int screenHeight = minecraft.getWindow().getGuiScaledHeight();
                
                int hudX = screenWidth / 2 + 10; // 中央偏右
                int hudY = screenHeight - 60; // 距离底部60像素
                
                // 渲染术法快捷栏
                renderSpellHotbar(guiGraphics, font, cultivation, hudX, hudY);
                
                // 渲染激活的持续性术法状态
                renderActiveSpells(guiGraphics, font, cultivation, hudX, hudY - 40);
            });
        } catch (Exception e) {
            // 捕获渲染异常，避免游戏崩溃
            Tiandao.LOGGER.error("术法 HUD 渲染时发生错误", e);
        }
    }
    
    /**
     * 渲染术法快捷栏
     * 
     * @param guiGraphics GuiGraphics 实例
     * @param font 字体
     * @param cultivation 修仙能力
     * @param startX 起始 X 坐标
     * @param startY 起始 Y 坐标
     */
    private static void renderSpellHotbar(GuiGraphics guiGraphics, Font font, ICultivation cultivation,
                                           int startX, int startY) {
        String[] hotbar = cultivation.getSpellHotbar();
        
        for (int i = 0; i < SLOT_COUNT; i++) {
            int slotX = startX + i * (SLOT_SIZE + SLOT_SPACING);
            int slotY = startY;
            
            String spellId = hotbar[i];
            SpellData spell = null;
            if (spellId != null) {
                spell = SpellRegistry.getInstance().getSpellById(spellId);
            }
            
            // 确定边框颜色（激活状态为绿色）
            boolean isActive = spell != null && cultivation.isSpellActive(spellId);
            int borderColor = isActive ? ACTIVE_BORDER_COLOR : BORDER_COLOR;
            
            // 绘制槽位背景
            guiGraphics.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, BACKGROUND_COLOR);
            
            // 绘制槽位边框
            guiGraphics.fill(slotX - 1, slotY - 1, slotX + SLOT_SIZE + 1, slotY, borderColor); // 上边框
            guiGraphics.fill(slotX - 1, slotY + SLOT_SIZE, slotX + SLOT_SIZE + 1, slotY + SLOT_SIZE + 1, borderColor); // 下边框
            guiGraphics.fill(slotX - 1, slotY, slotX, slotY + SLOT_SIZE, borderColor); // 左边框
            guiGraphics.fill(slotX + SLOT_SIZE, slotY, slotX + SLOT_SIZE + 1, slotY + SLOT_SIZE, borderColor); // 右边框
            
            // 如果槽位有术法，绘制术法信息
            if (spell != null) {
                // 绘制术法名称首字符（简化显示）
                String spellIcon = spell.getName().substring(0, Math.min(2, spell.getName().length()));
                int textX = slotX + (SLOT_SIZE - font.width(spellIcon)) / 2;
                int textY = slotY + (SLOT_SIZE - font.lineHeight) / 2;
                guiGraphics.drawString(font, spellIcon, textX, textY, TEXT_COLOR);
                
                // 检查冷却状态
                int cooldownRemaining = cultivation.getSpellCooldownRemaining(spellId);
                if (cooldownRemaining > 0) {
                    // 计算冷却进度（0.0 = 完全冷却，1.0 = 冷却中）
                    float cooldownProgress = (float) cooldownRemaining / spell.getCooldown();
                    
                    // 获取平滑旋转角度
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player == null) continue;
                    float partialTick = mc.getFrameTime();
                    float rotation = HudAnimationManager.getInstance().getCooldownRotation(
                        mc.player.getUUID(), i, cooldownProgress, partialTick);
                    
                    // 绘制冷却遮罩（从顶部开始，根据旋转角度）
                    // 使用简化的方法：绘制从顶部旋转的遮罩
                    int cooldownHeight = (int) (SLOT_SIZE * cooldownProgress);
                    guiGraphics.fill(slotX, slotY + SLOT_SIZE - cooldownHeight, 
                                    slotX + SLOT_SIZE, slotY + SLOT_SIZE, COOLDOWN_COLOR);
                    
                    // 绘制旋转的冷却指示器（在遮罩上方绘制一个旋转的线条）
                    int centerX = slotX + SLOT_SIZE / 2;
                    int centerY = slotY + SLOT_SIZE / 2;
                    drawRotatingIndicator(guiGraphics, centerX, centerY, SLOT_SIZE / 2, rotation);
                    
                    // 绘制冷却时间文本
                    String cooldownText = String.valueOf(cooldownRemaining);
                    int cooldownTextX = slotX + (SLOT_SIZE - font.width(cooldownText)) / 2;
                    int cooldownTextY = slotY + SLOT_SIZE - font.lineHeight - 1;
                    guiGraphics.drawString(font, cooldownText, cooldownTextX, cooldownTextY, 0xFFFF0000);
                }
                
                // 绘制槽位编号
                String slotNumber = String.valueOf(i + 1);
                guiGraphics.drawString(font, slotNumber, slotX + 1, slotY + 1, 0xFFAAAAAA);
            } else {
                // 空槽位，显示槽位编号
                String slotNumber = String.valueOf(i + 1);
                int textX = slotX + (SLOT_SIZE - font.width(slotNumber)) / 2;
                int textY = slotY + (SLOT_SIZE - font.lineHeight) / 2;
                guiGraphics.drawString(font, slotNumber, textX, textY, 0xFF666666);
            }
        }
        
        // 绘制快捷键提示（在槽位下方）
        int hintY = startY + SLOT_SIZE + 4;
        String hintText = "术法快捷栏 (Shift+1/2/3/4 使用)";
        int hintX = startX + ((SLOT_SIZE + SLOT_SPACING) * SLOT_COUNT - font.width(hintText)) / 2;
        guiGraphics.drawString(font, hintText, hintX, hintY, 0xFF888888);
    }
    
    /**
     * 渲染激活的持续性术法状态
     * 
     * @param guiGraphics GuiGraphics 实例
     * @param font 字体
     * @param cultivation 修仙能力
     * @param startX 起始 X 坐标
     * @param startY 起始 Y 坐标
     */
    private static void renderActiveSpells(GuiGraphics guiGraphics, Font font, ICultivation cultivation,
                                            int startX, int startY) {
        var activeSpells = cultivation.getActiveSpells();
        
        if (activeSpells.isEmpty()) {
            return;
        }
        
        int lineHeight = font.lineHeight + 2;
        int currentY = startY;
        
        // 标题
        guiGraphics.drawString(font, "激活的术法:", startX, currentY, 0xFFFFFFFF);
        currentY += lineHeight;
        
        // 遍历所有激活的术法
        long currentTime = System.currentTimeMillis();
        for (var entry : activeSpells.entrySet()) {
            String spellId = entry.getKey();
            long endTime = entry.getValue();
            
            SpellData spell = SpellRegistry.getInstance().getSpellById(spellId);
            if (spell == null) {
                continue;
            }
            
            // 计算剩余时间（秒）
            int remainingSeconds = (int) Math.max(0, (endTime - currentTime) / 1000);
            
            // 绘制术法名称和剩余时间
            String statusText = String.format("§e%s §7(%ds)", spell.getName(), remainingSeconds);
            guiGraphics.drawString(font, statusText, startX, currentY, 0xFFFFFFFF);
            
            // 绘制简单的进度条
            int barWidth = 80;
            int barHeight = 4;
            int barX = startX + font.width(statusText) + 4;
            int barY = currentY + 2;
            
            float progress = (float) remainingSeconds / (spell.getDuration() / 1000);
            int fillWidth = (int) (barWidth * progress);
            
            // 背景
            guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF333333);
            
            // 填充
            if (fillWidth > 0) {
                int fillColor = 0xFF00FF00; // 绿色
                guiGraphics.fill(barX, barY, barX + fillWidth, barY + barHeight, fillColor);
            }
            
            currentY += lineHeight;
        }
    }
    
    /**
     * 绘制旋转的冷却指示器（从中心到边缘的线条）
     * 
     * @param guiGraphics GuiGraphics 实例
     * @param centerX 中心X坐标
     * @param centerY 中心Y坐标
     * @param radius 半径
     * @param rotation 旋转角度（度）
     */
    private static void drawRotatingIndicator(GuiGraphics guiGraphics, int centerX, int centerY, 
                                              int radius, float rotation) {
        // 计算旋转后的边缘点（从顶部开始，顺时针旋转）
        float angle = (float) Math.toRadians(-90.0f + rotation);
        int endX = centerX + (int) (radius * Math.cos(angle));
        int endY = centerY + (int) (radius * Math.sin(angle));
        
        // 绘制从中心到边缘的线条（使用简单的矩形近似）
        int lineWidth = 2;
        int lineLength = radius;
        
        // 计算线条的起点和终点
        int startX = centerX;
        int startY = centerY;
        
        // 绘制旋转的指示器线条（使用多个小矩形近似）
        for (int i = 0; i < lineLength; i++) {
            float t = (float) i / lineLength;
            int x = (int) (startX + (endX - startX) * t);
            int y = (int) (startY + (endY - startY) * t);
            
            // 绘制一个小矩形作为线条的一部分
            guiGraphics.fill(x - lineWidth / 2, y - lineWidth / 2, 
                            x + lineWidth / 2, y + lineWidth / 2, 0xFFFF0000);
        }
    }
}

