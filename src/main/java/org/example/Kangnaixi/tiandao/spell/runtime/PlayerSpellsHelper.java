package org.example.Kangnaixi.tiandao.spell.runtime;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullConsumer;
import org.example.Kangnaixi.tiandao.Tiandao;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * 小型辅助类，集中处理 PlayerSpells Capability 的常见操作，
 * 方便 GUI 与指令等入口调用。
 */
public final class PlayerSpellsHelper {

    private PlayerSpellsHelper() {}

    public static LazyOptional<IPlayerSpells> get(Player player) {
        return player.getCapability(Tiandao.PLAYER_SPELLS_CAP);
    }

    public static void with(Player player, Consumer<IPlayerSpells> action) {
        get(player).ifPresent(new NonNullConsumer<>() {
            @Override
            public void accept(IPlayerSpells cap) {
                action.accept(cap);
            }
        });
    }

    public static Optional<Spell> getActive(Player player) {
        return get(player)
            .resolve()
            .map(cap -> Optional.ofNullable(cap.getActiveSpell()))
            .orElse(Optional.empty());
    }

    public static boolean setActive(Player player, String spellId) {
        AtomicBoolean updated = new AtomicBoolean(false);
        with(player, cap -> cap.getSpells().stream()
            .filter(spell -> spell.getId().equalsIgnoreCase(spellId))
            .findFirst()
            .ifPresent(spell -> {
                cap.setActiveSpell(spell);
                updated.set(true);
            }));
        return updated.get();
    }

    public static boolean addSpell(Player player, Spell spell) {
        AtomicBoolean added = new AtomicBoolean(false);
        with(player, cap -> {
            cap.addSpell(spell);
            added.set(true);
        });
        return added.get();
    }
}
