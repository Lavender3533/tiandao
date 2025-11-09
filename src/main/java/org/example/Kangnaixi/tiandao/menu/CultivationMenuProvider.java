package org.example.Kangnaixi.tiandao.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.example.Kangnaixi.tiandao.container.CultivationContainer;
import org.jetbrains.annotations.Nullable;

/**
 * 修炼菜单提供者，用于打开修炼界面
 */
public class CultivationMenuProvider implements MenuProvider {
    private final BlockPos pos;
    
    public CultivationMenuProvider(BlockPos pos) {
        this.pos = pos;
    }
    
    @Override
    public Component getDisplayName() {
        return Component.literal("修炼");
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
        return new CultivationContainer(windowId, playerInventory, pos);
    }
}