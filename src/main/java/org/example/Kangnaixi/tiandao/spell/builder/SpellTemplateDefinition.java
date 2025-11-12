package org.example.Kangnaixi.tiandao.spell.builder;

import org.example.Kangnaixi.tiandao.cultivation.CultivationRealm;

import java.util.Collections;
import java.util.List;

/**
 * 预设模板，用于 GUI 快速生成术法链路。
 */
public class SpellTemplateDefinition {

    private String id = "";
    private String displayName = "";
    private String description = "";
    private String formId = "";
    private String effectId = "";
    private List<String> augmentIds = Collections.emptyList();
    private CultivationRealm recommendedRealm = CultivationRealm.QI_CONDENSATION;
    private int recommendedSubRealmLevel = 0;
    private String notes = "";

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getFormId() {
        return formId;
    }

    public String getEffectId() {
        return effectId;
    }

    public List<String> getAugmentIds() {
        return augmentIds == null ? Collections.emptyList() : Collections.unmodifiableList(augmentIds);
    }

    public CultivationRealm getRecommendedRealm() {
        return recommendedRealm == null ? CultivationRealm.QI_CONDENSATION : recommendedRealm;
    }

    public int getRecommendedSubRealmLevel() {
        return recommendedSubRealmLevel;
    }

    public String getNotes() {
        return notes;
    }
}
