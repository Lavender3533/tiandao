package org.example.Kangnaixi.tiandao.spell.runtime;

import org.example.Kangnaixi.tiandao.spell.definition.SpellDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SpellRuntimeResult {

    private final SpellDefinition definition;
    private final SpellDefinition.Numbers numbers;
    private final List<SpellDefinition.Effect> finalEffects;
    private final boolean swordQiTriggered;

    public SpellRuntimeResult(SpellDefinition definition,
                              SpellDefinition.Numbers numbers,
                              List<SpellDefinition.Effect> finalEffects,
                              boolean swordQiTriggered) {
        this.definition = definition;
        this.numbers = numbers;
        this.finalEffects = Collections.unmodifiableList(new ArrayList<>(finalEffects));
        this.swordQiTriggered = swordQiTriggered;
    }

    public SpellDefinition definition() {
        return definition;
    }

    public SpellDefinition.Numbers numbers() {
        return numbers;
    }

    public List<SpellDefinition.Effect> effects() {
        return finalEffects;
    }

    public boolean swordQiTriggered() {
        return swordQiTriggered;
    }
}
