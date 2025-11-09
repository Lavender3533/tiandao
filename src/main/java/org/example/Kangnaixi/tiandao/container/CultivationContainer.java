package org.example.Kangnaixi.tiandao.container;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.example.Kangnaixi.tiandao.blockentity.CultivationAltarBlockEntity;
import org.example.Kangnaixi.tiandao.menu.ModMenuTypes;

/**
 * 修炼容器类，用于管理修炼界面的物品槽
 */
public class CultivationContainer extends AbstractContainerMenu {
    private final BlockPos pos;
    
    public CultivationContainer(int windowId, Inventory playerInventory, BlockPos pos) {
        super(ModMenuTypes.CULTIVATION_MENU.get(), windowId);
        this.pos = pos;
        
        // 玩家物品槽
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        
        // 玩家快捷栏
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 142));
        }
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
    
    @Override
    public boolean stillValid(Player player) {
        return true;
    }
    
    public BlockPos getPos() {
        return pos;
    }
}