package org.example.Kangnaixi.tiandao.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;
import org.example.Kangnaixi.tiandao.Tiandao;
import org.example.Kangnaixi.tiandao.capability.ICultivation;

import java.util.*;
import java.util.function.Supplier;

/**
 * 术法数据同步数据包
 * 服务器 -> 客户端：同步玩家的术法数据
 */
public class SpellDataSyncPacket {
    
    // 已解锁的术法ID列表
    private final List<String> unlockedSpells;
    
    // 术法快捷栏（4个槽位）
    private final String[] spellHotbar;
    
    // 术法冷却时间 <术法ID, 冷却结束时间戳>
    private final Map<String, Long> spellCooldowns;
    
    // 激活的持续性术法 <术法ID, 效果结束时间戳>
    private final Map<String, Long> activeSpells;
    
    /**
     * 从 ICultivation 构造数据包
     */
    public SpellDataSyncPacket(ICultivation cultivation) {
        this.unlockedSpells = new ArrayList<>(cultivation.getUnlockedSpells());
        this.spellHotbar = cultivation.getSpellHotbar().clone();
        this.spellCooldowns = new HashMap<>(cultivation.getSpellCooldowns());
        this.activeSpells = new HashMap<>(cultivation.getActiveSpells());
    }
    
    /**
     * 从网络缓冲区读取数据包
     */
    public SpellDataSyncPacket(FriendlyByteBuf buf) {
        // 读取已解锁的术法列表
        int unlockedCount = buf.readInt();
        this.unlockedSpells = new ArrayList<>();
        for (int i = 0; i < unlockedCount; i++) {
            unlockedSpells.add(buf.readUtf());
        }
        
        // 读取术法快捷栏
        this.spellHotbar = new String[4];
        for (int i = 0; i < 4; i++) {
            if (buf.readBoolean()) {
                spellHotbar[i] = buf.readUtf();
            } else {
                spellHotbar[i] = null;
            }
        }
        
        // 读取术法冷却时间
        int cooldownCount = buf.readInt();
        this.spellCooldowns = new HashMap<>();
        for (int i = 0; i < cooldownCount; i++) {
            String spellId = buf.readUtf();
            long endTime = buf.readLong();
            spellCooldowns.put(spellId, endTime);
        }
        
        // 读取激活的持续性术法
        int activeCount = buf.readInt();
        this.activeSpells = new HashMap<>();
        for (int i = 0; i < activeCount; i++) {
            String spellId = buf.readUtf();
            long endTime = buf.readLong();
            activeSpells.put(spellId, endTime);
        }
    }
    
    /**
     * 将数据包写入网络缓冲区
     */
    public void encode(FriendlyByteBuf buf) {
        // 写入已解锁的术法列表
        buf.writeInt(unlockedSpells.size());
        for (String spellId : unlockedSpells) {
            buf.writeUtf(spellId);
        }
        
        // 写入术法快捷栏
        for (int i = 0; i < 4; i++) {
            if (spellHotbar[i] != null) {
                buf.writeBoolean(true);
                buf.writeUtf(spellHotbar[i]);
            } else {
                buf.writeBoolean(false);
            }
        }
        
        // 写入术法冷却时间
        buf.writeInt(spellCooldowns.size());
        for (Map.Entry<String, Long> entry : spellCooldowns.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeLong(entry.getValue());
        }
        
        // 写入激活的持续性术法
        buf.writeInt(activeSpells.size());
        for (Map.Entry<String, Long> entry : activeSpells.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeLong(entry.getValue());
        }
    }
    
    /**
     * 处理数据包（客户端）
     */
    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // 客户端处理：更新本地玩家的术法数据
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                player.getCapability(Tiandao.CULTIVATION_CAPABILITY).ifPresent(cultivation -> {
                    // 清空现有数据
                    cultivation.getUnlockedSpells().clear();
                    
                    // 同步已解锁的术法
                    for (String spellId : unlockedSpells) {
                        cultivation.unlockSpell(spellId);
                    }
                    
                    // 同步术法快捷栏
                    for (int i = 0; i < 4; i++) {
                        cultivation.setSpellHotbar(i, spellHotbar[i]);
                    }
                    
                    // 同步术法冷却时间
                    cultivation.getSpellCooldowns().clear();
                    for (Map.Entry<String, Long> entry : spellCooldowns.entrySet()) {
                        cultivation.setSpellCooldown(entry.getKey(), entry.getValue());
                    }
                    
                    // 同步激活的持续性术法
                    cultivation.getActiveSpells().clear();
                    for (Map.Entry<String, Long> entry : activeSpells.entrySet()) {
                        cultivation.activateSpell(entry.getKey(), entry.getValue());
                    }
                    
                    Tiandao.LOGGER.debug("客户端术法数据已同步: {} 个术法已解锁", unlockedSpells.size());
                });
            }
        });
        context.setPacketHandled(true);
    }
}

