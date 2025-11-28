package org.example.Kangnaixi.tiandao.client.gui;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import org.example.Kangnaixi.tiandao.Tiandao;

/**
 * 占位的快捷栏 Overlay（已禁用 GUI 绘制）。
 * 按需求移除了全部 HUD/2D 渲染逻辑。
 */
@Mod.EventBusSubscriber(modid = Tiandao.MODID, value = Dist.CLIENT)
public class SpellHotbarOverlay {
    // no-op
}
