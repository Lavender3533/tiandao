package org.example.Kangnaixi.tiandao.spell.runtime;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlayerSpellsCapability implements IPlayerSpells {

    private final List<Spell> spells = new ArrayList<>();
    private Spell activeSpell;

    @Override
    public List<Spell> getSpells() {
        return Collections.unmodifiableList(spells);
    }

    @Override
    public void addSpell(Spell spell) {
        spells.add(spell);
        if (activeSpell == null) {
            activeSpell = spell;
        }
    }

    @Override
    public Spell getActiveSpell() {
        return activeSpell;
    }

    @Override
    public void setActiveSpell(Spell spell) {
        this.activeSpell = spell;
    }

    @Override
    public void copyFrom(IPlayerSpells other) {
        this.spells.clear();
        for (Spell spell : other.getSpells()) {
            this.spells.add(spell.copy());
        }
        Spell reference = other.getActiveSpell();
        if (reference != null) {
            String targetId = reference.getId();
            this.activeSpell = this.spells.stream()
                .filter(spell -> spell.getId().equals(targetId))
                .findFirst()
                .orElse(this.spells.isEmpty() ? null : this.spells.get(0));
        } else {
            this.activeSpell = this.spells.isEmpty() ? null : this.spells.get(0);
        }
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        ListTag spellList = new ListTag();
        for (Spell spell : spells) {
            spellList.add(spell.toTag());
        }
        tag.put("Spells", spellList);
        if (activeSpell != null) {
            tag.putString("ActiveSpellId", activeSpell.getId());
        }
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        spells.clear();
        ListTag spellList = tag.getList("Spells", Tag.TAG_COMPOUND);
        for (Tag element : spellList) {
            CompoundTag entry = (CompoundTag) element;
            spells.add(Spell.fromTag(entry));
        }
        String activeId = tag.getString("ActiveSpellId");
        if (!activeId.isEmpty()) {
            for (Spell spell : spells) {
                if (spell.getId().equals(activeId)) {
                    activeSpell = spell;
                    break;
                }
            }
        } else {
            activeSpell = spells.isEmpty() ? null : spells.get(0);
        }
    }
}
