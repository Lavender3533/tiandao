package org.example.Kangnaixi.tiandao.spell.runtime;

import java.util.List;

public interface IPlayerSpells {

    List<Spell> getSpells();

    void addSpell(Spell spell);

    Spell getActiveSpell();

    void setActiveSpell(Spell spell);

    void copyFrom(IPlayerSpells other);
}
