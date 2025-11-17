package org.example.Kangnaixi.tiandao.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.capability.ICultivation;
import org.example.Kangnaixi.tiandao.item.SpellJadeSlipItem;
import org.example.Kangnaixi.tiandao.spell.definition.SpellDefinition;
import org.example.Kangnaixi.tiandao.spell.SpellRegistry;
import org.example.Kangnaixi.tiandao.spell.SpellLocalization;
import org.example.Kangnaixi.tiandao.spell.blueprint.SpellBlueprint;
import org.example.Kangnaixi.tiandao.spell.blueprint.SpellBlueprintMetrics;
import java.util.Locale;

/**
 * 鏈硶绯荤粺 HUD 娓叉煋鍣?
 * 鐢ㄤ簬鍦ㄦ父鎴忕晫闈笂鏄剧ず鏈硶蹇嵎鏍忓拰鐘舵€?
 */
public class SpellHudOverlay {
    private static final int SLOT_SIZE = 24; // 妲戒綅澶у皬
    private static final int SLOT_SPACING = 4; // 妲戒綅闂磋窛
    private static final int SLOT_COUNT = 4; // 妲戒綅鏁伴噺
    private static final int BACKGROUND_COLOR = 0x80000000; // 鑳屾櫙棰滆壊锛堝崐閫忔槑榛戣壊锛?
    private static final int BORDER_COLOR = 0xFF000000; // 杈规棰滆壊锛堥粦鑹诧級
    private static final int ACTIVE_BORDER_COLOR = 0xFF00FF00; // 婵€娲绘Ы浣嶈竟妗嗭紙缁胯壊锛?
    private static final int COOLDOWN_COLOR = 0x80FF0000; // 鍐峰嵈閬僵棰滆壊锛堝崐閫忔槑绾㈣壊锛?
    private static final int TEXT_COLOR = 0xFFFFFF; // 鏂囨湰棰滆壊锛堢櫧鑹诧級
    
    /**
     * 娓叉煋鏈硶 HUD 涓诲叆鍙?
     * 
     * @param guiGraphics GuiGraphics 瀹炰緥
     * @param partialTick 閮ㄥ垎 tick
     */
    public static void render(GuiGraphics guiGraphics, float partialTick) {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            Player player = minecraft.player;
            
            if (player == null || minecraft.options.hideGui) {
                return;
            }
            
            // 鑾峰彇鐜╁淇粰鑳藉姏
            player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
                Font font = minecraft.font;
                
                // 璁＄畻 HUD 浣嶇疆锛堝睆骞曞簳閮ㄤ腑澶亸鍙筹級
                int screenWidth = minecraft.getWindow().getGuiScaledWidth();
                int screenHeight = minecraft.getWindow().getGuiScaledHeight();
                
                int hudX = screenWidth / 2 + 10; // 涓ぎ鍋忓彸
                int hudY = screenHeight - 60; // 璺濈搴曢儴60鍍忕礌
                
                // 娓叉煋鏈硶蹇嵎鏍?
                renderSpellHotbar(guiGraphics, font, cultivation, hudX, hudY);
                
                // 娓叉煋婵€娲荤殑鎸佺画鎬ф湳娉曠姸鎬?
                renderActiveSpells(guiGraphics, font, cultivation, hudX, hudY - 40);

                // 鏄剧ず鎵嬫寔鏈硶鐜夌畝鐨勯珮绾ф憳瑕?
                renderHeldBlueprintSummary(guiGraphics, font, player, screenWidth);
            });
        } catch (Exception e) {
            // 鎹曡幏娓叉煋寮傚父锛岄伩鍏嶆父鎴忓穿婧?            Tiandao.LOGGER.error("Spell HUD render failed", e);
        }
    }
    
    /**
     * 娓叉煋鏈硶蹇嵎鏍?
     * 
     * @param guiGraphics GuiGraphics 瀹炰緥
     * @param font 瀛椾綋
     * @param cultivation 淇粰鑳藉姏
     * @param startX 璧峰 X 鍧愭爣
     * @param startY 璧峰 Y 鍧愭爣
     */
    private static void renderSpellHotbar(GuiGraphics guiGraphics, Font font, ICultivation cultivation,
                                           int startX, int startY) {
        String[] hotbar = cultivation.getSpellHotbar();
        
        for (int i = 0; i < SLOT_COUNT; i++) {
            int slotX = startX + i * (SLOT_SIZE + SLOT_SPACING);
            int slotY = startY;
            
            String spellId = hotbar[i];
            SpellDefinition spell = null;
            if (spellId != null) {
                spell = SpellRegistry.getInstance().getSpellById(spellId);
            }
            
            // 纭畾杈规棰滆壊锛堟縺娲荤姸鎬佷负缁胯壊锛?
            boolean isActive = spell != null && cultivation.isSpellActive(spellId);
            int borderColor = isActive ? ACTIVE_BORDER_COLOR : BORDER_COLOR;
            
            // 缁樺埗妲戒綅鑳屾櫙
            guiGraphics.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, BACKGROUND_COLOR);
            
            // 缁樺埗妲戒綅杈规
            guiGraphics.fill(slotX - 1, slotY - 1, slotX + SLOT_SIZE + 1, slotY, borderColor); // 涓婅竟妗?
            guiGraphics.fill(slotX - 1, slotY + SLOT_SIZE, slotX + SLOT_SIZE + 1, slotY + SLOT_SIZE + 1, borderColor); // 涓嬭竟妗?
            guiGraphics.fill(slotX - 1, slotY, slotX, slotY + SLOT_SIZE, borderColor); // 宸﹁竟妗?
            guiGraphics.fill(slotX + SLOT_SIZE, slotY, slotX + SLOT_SIZE + 1, slotY + SLOT_SIZE, borderColor); // 鍙宠竟妗?
            
            // 濡傛灉妲戒綅鏈夋湳娉曪紝缁樺埗鏈硶淇℃伅
            if (spell != null) {
                // 缁樺埗鏈硶鍚嶇О棣栧瓧绗︼紙绠€鍖栨樉绀猴級
                String displayName = spell.getMetadata().displayName();
                String spellIcon = displayName.substring(0, Math.min(2, displayName.length()));
                int textX = slotX + (SLOT_SIZE - font.width(spellIcon)) / 2;
                int textY = slotY + (SLOT_SIZE - font.lineHeight) / 2;
                guiGraphics.drawString(font, spellIcon, textX, textY, TEXT_COLOR);
                
                // 妫€鏌ュ喎鍗寸姸鎬?
                int cooldownRemaining = cultivation.getSpellCooldownRemaining(spellId);
                if (cooldownRemaining > 0) {
                    // 璁＄畻鍐峰嵈杩涘害锛?.0 = 瀹屽叏鍐峰嵈锛?.0 = 鍐峰嵈涓級
                    float cooldownProgress = (float) cooldownRemaining / (float) spell.getBaseStats().cooldownSeconds();
                    
                    // 鑾峰彇骞虫粦鏃嬭浆瑙掑害
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player == null) continue;
                    float partialTick = mc.getFrameTime();
                    float rotation = HudAnimationManager.getInstance().getCooldownRotation(
                        mc.player.getUUID(), i, cooldownProgress, partialTick);
                    
                    // 缁樺埗鍐峰嵈閬僵锛堜粠椤堕儴寮€濮嬶紝鏍规嵁鏃嬭浆瑙掑害锛?
                    // 浣跨敤绠€鍖栫殑鏂规硶锛氱粯鍒朵粠椤堕儴鏃嬭浆鐨勯伄缃?
                    int cooldownHeight = (int) (SLOT_SIZE * cooldownProgress);
                    guiGraphics.fill(slotX, slotY + SLOT_SIZE - cooldownHeight, 
                                    slotX + SLOT_SIZE, slotY + SLOT_SIZE, COOLDOWN_COLOR);
                    
                    // 缁樺埗鏃嬭浆鐨勫喎鍗存寚绀哄櫒锛堝湪閬僵涓婃柟缁樺埗涓€涓棆杞殑绾挎潯锛?
                    int centerX = slotX + SLOT_SIZE / 2;
                    int centerY = slotY + SLOT_SIZE / 2;
                    drawRotatingIndicator(guiGraphics, centerX, centerY, SLOT_SIZE / 2, rotation);
                    
                    // 缁樺埗鍐峰嵈鏃堕棿鏂囨湰
                    String cooldownText = String.valueOf(cooldownRemaining);
                    int cooldownTextX = slotX + (SLOT_SIZE - font.width(cooldownText)) / 2;
                    int cooldownTextY = slotY + SLOT_SIZE - font.lineHeight - 1;
                    guiGraphics.drawString(font, cooldownText, cooldownTextX, cooldownTextY, 0xFFFF0000);
                }
                
                // 缁樺埗妲戒綅缂栧彿
                String slotNumber = String.valueOf(i + 1);
                guiGraphics.drawString(font, slotNumber, slotX + 1, slotY + 1, 0xFFAAAAAA);
            } else {
                // 绌烘Ы浣嶏紝鏄剧ず妲戒綅缂栧彿
                String slotNumber = String.valueOf(i + 1);
                int textX = slotX + (SLOT_SIZE - font.width(slotNumber)) / 2;
                int textY = slotY + (SLOT_SIZE - font.lineHeight) / 2;
                guiGraphics.drawString(font, slotNumber, textX, textY, 0xFF666666);
            }
        }
        
        // 缁樺埗蹇嵎閿彁绀猴紙鍦ㄦЫ浣嶄笅鏂癸級
        int hintY = startY + SLOT_SIZE + 4;
        String hintText = "鏈硶蹇嵎鏍?(Shift+1/2/3/4 浣跨敤)";
        int hintX = startX + ((SLOT_SIZE + SLOT_SPACING) * SLOT_COUNT - font.width(hintText)) / 2;
        guiGraphics.drawString(font, hintText, hintX, hintY, 0xFF888888);
    }
    
    /**
     * 娓叉煋婵€娲荤殑鎸佺画鎬ф湳娉曠姸鎬?
     * 
     * @param guiGraphics GuiGraphics 瀹炰緥
     * @param font 瀛椾綋
     * @param cultivation 淇粰鑳藉姏
     * @param startX 璧峰 X 鍧愭爣
     * @param startY 璧峰 Y 鍧愭爣
     */
    private static void renderActiveSpells(GuiGraphics guiGraphics, Font font, ICultivation cultivation,
                                            int startX, int startY) {
        var activeSpells = cultivation.getActiveSpells();
        
        if (activeSpells.isEmpty()) {
            return;
        }
        
        int lineHeight = font.lineHeight + 2;
        int currentY = startY;
        
        // 鏍囬
        guiGraphics.drawString(font, "婵€娲荤殑鏈硶:", startX, currentY, 0xFFFFFFFF);
        currentY += lineHeight;
        
        // 閬嶅巻鎵€鏈夋縺娲荤殑鏈硶
        long currentTime = System.currentTimeMillis();
        for (var entry : activeSpells.entrySet()) {
            String spellId = entry.getKey();
            long endTime = entry.getValue();
            
            SpellDefinition spell = SpellRegistry.getInstance().getSpellById(spellId);
            if (spell == null) {
                continue;
            }
            
            // 璁＄畻鍓╀綑鏃堕棿锛堢锛?
            int remainingSeconds = (int) Math.max(0, (endTime - currentTime) / 1000);
            
            // 缁樺埗鏈硶鍚嶇О鍜屽墿浣欐椂闂?
            String statusText = String.format("§e%s §7(%ds)", spell.getMetadata().displayName(), remainingSeconds);
            guiGraphics.drawString(font, statusText, startX, currentY, 0xFFFFFFFF);
            
            // 缁樺埗绠€鍗曠殑杩涘害鏉?
            int barWidth = 80;
            int barHeight = 4;
            int barX = startX + font.width(statusText) + 4;
            int barY = currentY + 2;
            
            float totalSeconds = Math.max(1f, spell.getBaseStats().durationTicks() / 20f);
            float progress = (float) remainingSeconds / totalSeconds;
            int fillWidth = (int) (barWidth * progress);
            
            // 鑳屾櫙
            guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF333333);
            
            // 濉厖
            if (fillWidth > 0) {
                int fillColor = 0xFF00FF00; // 缁胯壊
                guiGraphics.fill(barX, barY, barX + fillWidth, barY + barHeight, fillColor);
            }
            
            currentY += lineHeight;
        }
    }
    
    /**
     * 缁樺埗鏃嬭浆鐨勫喎鍗存寚绀哄櫒锛堜粠涓績鍒拌竟缂樼殑绾挎潯锛?
     * 
     * @param guiGraphics GuiGraphics 瀹炰緥
     * @param centerX 涓績X鍧愭爣
     * @param centerY 涓績Y鍧愭爣
     * @param radius 鍗婂緞
     * @param rotation 鏃嬭浆瑙掑害锛堝害锛?
     */
    private static void drawRotatingIndicator(GuiGraphics guiGraphics, int centerX, int centerY, 
                                              int radius, float rotation) {
        // 璁＄畻鏃嬭浆鍚庣殑杈圭紭鐐癸紙浠庨《閮ㄥ紑濮嬶紝椤烘椂閽堟棆杞級
        float angle = (float) Math.toRadians(-90.0f + rotation);
        int endX = centerX + (int) (radius * Math.cos(angle));
        int endY = centerY + (int) (radius * Math.sin(angle));
        
        // 缁樺埗浠庝腑蹇冨埌杈圭紭鐨勭嚎鏉★紙浣跨敤绠€鍗曠殑鐭╁舰杩戜技锛?
        int lineWidth = 2;
        int lineLength = radius;
        
        // 璁＄畻绾挎潯鐨勮捣鐐瑰拰缁堢偣
        int startX = centerX;
        int startY = centerY;
        
        // 缁樺埗鏃嬭浆鐨勬寚绀哄櫒绾挎潯锛堜娇鐢ㄥ涓皬鐭╁舰杩戜技锛?
        for (int i = 0; i < lineLength; i++) {
            float t = (float) i / lineLength;
            int x = (int) (startX + (endX - startX) * t);
            int y = (int) (startY + (endY - startY) * t);
            
            // 缁樺埗涓€涓皬鐭╁舰浣滀负绾挎潯鐨勪竴閮ㄥ垎
            guiGraphics.fill(x - lineWidth / 2, y - lineWidth / 2, 
                            x + lineWidth / 2, y + lineWidth / 2, 0xFFFF0000);
        }
    }
    private static void renderHeldBlueprintSummary(GuiGraphics guiGraphics, Font font, Player player, int screenWidth) {
        ItemStack stack = findBlueprintStack(player);
        if (stack.isEmpty()) {
            return;
        }
        SpellBlueprint blueprint = SpellJadeSlipItem.getBlueprint(stack);
        if (blueprint == null) {
            return;
        }
        SpellBlueprint.AdvancedData data = blueprint.getAdvancedData().orElse(null);
        double complexity = SpellBlueprintMetrics.computeComplexity(data);
        double mana = SpellBlueprintMetrics.estimateManaCost(data, blueprint.getSpiritCost());
        double cooldown = SpellBlueprintMetrics.estimateCooldown(data, blueprint.getCooldownSeconds());
        double overload = SpellBlueprintMetrics.estimateOverloadThreshold(data, blueprint);

        int panelWidth = 170;
        int panelHeight = 60;
        int left = screenWidth / 2 - panelWidth - 130;
        int top = 30;
        guiGraphics.fill(left, top, left + panelWidth, top + panelHeight, 0x90000000);
        guiGraphics.drawString(font, SpellLocalization.gui("hud.held_header").withStyle(ChatFormatting.YELLOW),
            left + 4, top + 4, 0xFFFFFF, false);
        guiGraphics.drawString(font,
            SpellLocalization.gui("hud.held_element", SpellLocalization.element(blueprint.getElement())),
            left + 4, top + 16, 0xFFFFFF, false);
        guiGraphics.drawString(font,
            SpellLocalization.gui("hud.held_summary",
                String.format(Locale.ROOT, "%.1f", complexity),
                String.format(Locale.ROOT, "%.1f", mana)),
            left + 4, top + 28, 0xFFFFFF, false);
        guiGraphics.drawString(font,
            SpellLocalization.gui("hud.held_overload",
                String.format(Locale.ROOT, "%.1f", cooldown),
                String.format(Locale.ROOT, "%.1f", overload)),
            left + 4, top + 40, 0xFFFFFF, false);
    }
    private static ItemStack findBlueprintStack(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof SpellJadeSlipItem) {
            return main;
        }
        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof SpellJadeSlipItem) {
            return off;
        }
        return ItemStack.EMPTY;
    }
}
