package org.example.Kangnaixi.tiandao.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.network.NetworkHandler;

import java.util.function.Supplier;

/**
 * 术法快捷栏设置数据包
 * 客户端 -> 服务器：玩家设置术法快捷栏
 */
public class SpellHotbarSetPacket {
    
    private final int slot;
    private final String spellId; // null 表示清空槽位
    
    public SpellHotbarSetPacket(int slot, String spellId) {
        this.slot = slot;
        this.spellId = spellId;
    }
    
    public SpellHotbarSetPacket(FriendlyByteBuf buf) {
        this.slot = buf.readInt();
        boolean hasSpell = buf.readBoolean();
        this.spellId = hasSpell ? buf.readUtf() : null;
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(slot);
        if (spellId != null) {
            buf.writeBoolean(true);
            buf.writeUtf(spellId);
        } else {
            buf.writeBoolean(false);
        }
    }
    
    /**
     * 处理数据包（服务器端）
     */
    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            
            // 验证槽位有效性
            if (slot < 0 || slot >= 4) {
                Tiandao.LOGGER.warn("玩家 {} 尝试设置无效的术法槽位: {}", player.getName().getString(), slot);
                return;
            }
            
            player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
                // 如果是设置术法（非清空），检查是否已解锁
                if (spellId != null && !cultivation.hasSpell(spellId)) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§c你还没有解锁该术法！"
                    ));
                    return;
                }
                
                // 设置快捷栏
                cultivation.setSpellHotbar(slot, spellId);
                
                if (spellId != null) {
                    org.example.Kangnaixi.tiandao.spell.SpellData spell = 
                        org.example.Kangnaixi.tiandao.spell.SpellRegistry.getInstance().getSpellById(spellId);
                    if (spell != null) {
                        Tiandao.LOGGER.debug("玩家 {} 设置术法快捷栏 [{}]: {}", 
                            player.getName().getString(), slot, spell.getName());
                    }
                } else {
                    Tiandao.LOGGER.debug("玩家 {} 清空术法快捷栏 [{}]", player.getName().getString(), slot);
                }
                
                // 同步数据到客户端
                NetworkHandler.sendSpellDataToPlayer(new SpellDataSyncPacket(cultivation), player);
            });
        });
        context.setPacketHandled(true);
    }
}

