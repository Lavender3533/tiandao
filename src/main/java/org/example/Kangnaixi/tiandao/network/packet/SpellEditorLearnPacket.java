package org.example.Kangnaixi.tiandao.network.packet;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.example.Kangnaixi.tiandao.spell.runtime.PlayerSpellsHelper;
import org.example.Kangnaixi.tiandao.spell.runtime.Spell;

import java.util.function.Supplier;

/**
 * 客户端在术法编辑器中点击“保存”时发送, 由服务器将术法写入玩家能力.
 */
public class SpellEditorLearnPacket {

    private final CompoundTag spellTag;

    public SpellEditorLearnPacket(Spell spell) {
        this.spellTag = spell.toTag();
    }

    public SpellEditorLearnPacket(CompoundTag spellTag) {
        this.spellTag = spellTag;
    }

    public SpellEditorLearnPacket(FriendlyByteBuf buf) {
        this.spellTag = buf.readNbt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(spellTag);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            try {
                Spell spell = Spell.fromTag(spellTag);
                boolean added = PlayerSpellsHelper.addSpell(player, spell);
                if (added) {
                    PlayerSpellsHelper.setActive(player, spell.getId());
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a学会术法: " + spell.getName()));
                } else {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c未能保存术法, 请稍后重试"));
                }
            } catch (Exception ex) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c术法保存失败: " + ex.getMessage()));
            }
        });
        context.setPacketHandled(true);
    }
}
