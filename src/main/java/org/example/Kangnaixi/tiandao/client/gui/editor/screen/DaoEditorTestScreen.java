package org.example.Kangnaixi.tiandao.client.gui.editor.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.example.Kangnaixi.tiandao.client.gui.editor.ComponentData;
import org.example.Kangnaixi.tiandao.client.gui.editor.DaoTheme;
import org.example.Kangnaixi.tiandao.client.gui.editor.widget.DaoCardWidget;

/**
 * 修仙术法编辑器测试屏幕
 * 用于测试DaoCardWidget组件
 */
public class DaoEditorTestScreen extends Screen {
    private static final ResourceLocation BACKGROUND_TEXTURE = ResourceLocation.withDefaultNamespace("minecraft:textures/gui/options_background.png");

    private DaoCardWidget testCard1;
    private DaoCardWidget testCard2;
    private DaoCardWidget testCard3;

    public DaoEditorTestScreen() {
        super(Component.literal("修仙术法编辑器测试"));
    }

    @Override
    protected void init() {
        super.init();

        // 创建测试ComponentData
        ComponentData data1 = ComponentData.create(
            "test_sword", "基础剑气", "⚔",
            "将灵力凝聚于剑身，释放出锋利的剑气攻击敌人"
        );

        ComponentData data2 = ComponentData.create(
            "test_heal", "治愈木灵", "✦",
            "召唤木灵之力，恢复自身或友方的生命值"
        );

        ComponentData data3 = ComponentData.create(
            "test_thunder", "雷霆之剑", "⚡",
            "将雷电之力注入剑身，造成范围性的雷电伤害"
        );

        // 创建测试卡片（使用DaoTheme尺寸）
        testCard1 = new DaoCardWidget(50, 50, DaoTheme.CARD_WIDTH, DaoTheme.CARD_HEIGHT, data1);
        testCard1.setOnClickCallback(() -> {
            testCard1.setSelected(!testCard1.isSelected());
        });

        testCard2 = new DaoCardWidget(50, 190, DaoTheme.CARD_WIDTH, DaoTheme.CARD_HEIGHT, data2);
        testCard2.setOnClickCallback(() -> {
            testCard2.setSelected(!testCard2.isSelected());
        });

        testCard3 = new DaoCardWidget(50, 330, DaoTheme.CARD_WIDTH, DaoTheme.CARD_HEIGHT, data3);
        testCard3.setOnClickCallback(() -> {
            testCard3.setSelected(!testCard3.isSelected());
        });

        // 添加到渲染列表
        addRenderableWidget(testCard1);
        addRenderableWidget(testCard2);
        addRenderableWidget(testCard3);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 渲染羊皮纸背景
        guiGraphics.fill(0, 0, this.width, this.height, DaoTheme.BG_PARCHMENT);

        // 渲染标题
        guiGraphics.drawString(Minecraft.getInstance().font, this.title,
            this.width / 2 - this.title.getString().length() * 3, 20, DaoTheme.TEXT_PRIMARY);

        // 渲染说明文字
        guiGraphics.drawString(Minecraft.getInstance().font,
            Component.literal("鼠标悬停查看效果，点击卡片进行选择"),
            50, this.height - 30, DaoTheme.TEXT_SECONDARY);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}
