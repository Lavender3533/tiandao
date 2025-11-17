package org.example.Kangnaixi.tiandao.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.spell.runtime.hotbar.ISpellHotbar;

/**
 * 术法快捷栏HUD渲染器
 * 在屏幕右侧垂直显示9个技能槽位
 */
@Mod.EventBusSubscriber(modid = Tiandao.MODID, value = Dist.CLIENT)
public class SpellHotbarOverlay {

    // HUD配置
    private static final int SLOT_WIDTH = 80;      // 槽位宽度
    private static final int SLOT_HEIGHT = 20;     // 槽位高度
    private static final int SLOT_SPACING = 2;     // 槽位间距
    private static final int RIGHT_MARGIN = 10;    // 距离右边缘的距离
    private static final int TOP_MARGIN = 100;     // 距离顶部的距离

    // 颜色配置
    private static final int COLOR_BACKGROUND = 0x80000000;      // 黑色半透明背景
    private static final int COLOR_ACTIVE = 0xFF00FF00;          // 绿色边框（激活槽位）
    private static final int COLOR_INACTIVE = 0xFF888888;        // 灰色边框（未激活槽位）
    private static final int COLOR_TEXT = 0xFFFFFFFF;            // 白色文字
    private static final int COLOR_EMPTY_TEXT = 0xFF888888;      // 灰色文字（空槽位）

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null) {
            return;
        }

        // 获取玩家的术法快捷栏能力
        player.getCapability(Tiandao.SPELL_HOTBAR_CAP).ifPresent(hotbar -> {
            renderHotbar(event.getGuiGraphics(), mc, hotbar);
        });
    }

    /**
     * 渲染术法快捷栏
     */
    private static void renderHotbar(GuiGraphics guiGraphics, Minecraft mc, ISpellHotbar hotbar) {
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // 计算起始位置（右上角）
        int startX = screenWidth - SLOT_WIDTH - RIGHT_MARGIN;
        int startY = TOP_MARGIN;

        // 获取当前激活的槽位
        int activeIndex = hotbar.getActiveIndex();

        // 渲染9个槽位
        for (int i = 0; i < 9; i++) {
            int slotX = startX;
            int slotY = startY + i * (SLOT_HEIGHT + SLOT_SPACING);

            // 获取该槽位的术法ID
            String spellId = hotbar.getSlot(i);
            boolean isEmpty = (spellId == null || spellId.isEmpty());
            boolean isActive = (i == activeIndex);

            // 渲染槽位背景
            guiGraphics.fill(slotX, slotY, slotX + SLOT_WIDTH, slotY + SLOT_HEIGHT, COLOR_BACKGROUND);

            // 渲染槽位边框
            int borderColor = isActive ? COLOR_ACTIVE : COLOR_INACTIVE;
            drawBorder(guiGraphics, slotX, slotY, SLOT_WIDTH, SLOT_HEIGHT, borderColor);

            // 渲染槽位编号
            String slotNumber = String.valueOf(i + 1);
            guiGraphics.drawString(mc.font, slotNumber, slotX + 4, slotY + 6, COLOR_TEXT, false);

            // 渲染术法ID或"空"
            String displayText;
            int textColor;

            if (isEmpty) {
                displayText = "<空>";
                textColor = COLOR_EMPTY_TEXT;
            } else {
                // 截断过长的术法ID
                displayText = truncateText(spellId, SLOT_WIDTH - 20);
                textColor = COLOR_TEXT;
            }

            guiGraphics.drawString(mc.font, displayText, slotX + 16, slotY + 6, textColor, false);
        }
    }

    /**
     * 绘制边框
     */
    private static void drawBorder(GuiGraphics guiGraphics, int x, int y, int width, int height, int color) {
        // 上边
        guiGraphics.fill(x, y, x + width, y + 1, color);
        // 下边
        guiGraphics.fill(x, y + height - 1, x + width, y + height, color);
        // 左边
        guiGraphics.fill(x, y, x + 1, y + height, color);
        // 右边
        guiGraphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    /**
     * 截断过长的文本
     */
    private static String truncateText(String text, int maxWidth) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.font.width(text) <= maxWidth) {
            return text;
        }

        // 逐字符截断直到合适
        for (int i = text.length() - 1; i > 0; i--) {
            String truncated = text.substring(0, i) + "...";
            if (mc.font.width(truncated) <= maxWidth) {
                return truncated;
            }
        }

        return "...";
    }
}
