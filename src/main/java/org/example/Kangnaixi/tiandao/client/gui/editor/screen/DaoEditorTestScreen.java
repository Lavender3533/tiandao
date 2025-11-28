package org.example.Kangnaixi.tiandao.client.gui.editor.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.example.Kangnaixi.tiandao.client.gui.editor.widget.DaoCardWidget;

/**
 * 修仙术法编辑器测试屏幕
 * 用于测试DaoCardWidget组件
 */
public class DaoEditorTestScreen extends Screen {
    // 使用新的ResourceLocation构造方法
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
        
        // 创建测试卡片
//        testCard1 = new DaoCardWidget(
//            50, 50, 200, 120,
//            Component.literal("基础剑气"),
//            Component.literal("将灵力凝聚于剑身，释放出锋利的剑气攻击敌人"),
//            Component.literal("攻击类")
//        );
//
//        testCard2 = new DaoCardWidget(
//            50, 190, 200, 120,
//            Component.literal("治愈木灵"),
//            Component.literal("召唤木灵之力，恢复自身或友方的生命值"),
//            Component.literal("治疗类")
//        );
//
//        testCard3 = new DaoCardWidget(
//            50, 330, 200, 120,
//            Component.literal("雷霆之剑"),
//            Component.literal("将雷电之力注入剑身，造成范围性的雷电伤害"),
//            Component.literal("攻击类")
//        );
        
        // 添加到渲染列表
        addRenderableWidget(testCard1);
        addRenderableWidget(testCard2);
        addRenderableWidget(testCard3);
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 渲染背景
        renderBackground(guiGraphics);
        
        // 渲染标题
        guiGraphics.drawString(Minecraft.getInstance().font, this.title, this.width / 2 - this.title.getString().length() * 3, 20, 0xFFFFFF);
        
        // 渲染说明文字
        guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("鼠标悬停查看效果，点击卡片进行选择"), 50, this.height - 30, 0xAAAAAA);
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
        // 渲染半透明背景
        guiGraphics.fill(0, 0, this.width, this.height, 0x80000000);
        
        // 渲染网格背景
        RenderSystem.setShaderTexture(0, BACKGROUND_TEXTURE);
        int x = (this.width - 256) / 2;
        int y = (this.height - 256) / 2;
        guiGraphics.blit(BACKGROUND_TEXTURE, x, y, 0, 0, 256, 256);
    }
}