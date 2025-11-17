package org.example.Kangnaixi.tiandao.spell.runtime;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 服务器侧术法定义。可序列化到 NBT 以便 Capability 持久化。
 */
public class Spell {

    private final String id;
    private final String name;
    private final SourceType source;
    private final CarrierType carrier;
    private final FormType form;
    private final List<AttributeType> attributes;
    private final List<EffectType> effects;
    private final double baseDamage;
    private final double baseSpiritCost;
    private final double baseCooldown;
    private final double baseRange;

    public Spell(String id,
                 String name,
                 SourceType source,
                 CarrierType carrier,
                 FormType form,
                 List<AttributeType> attributes,
                 List<EffectType> effects,
                 double baseDamage,
                 double baseSpiritCost,
                 double baseCooldown,
                 double baseRange) {
        this.id = id;
        this.name = name;
        this.source = source;
        this.carrier = carrier;
        this.form = form;
        this.attributes = List.copyOf(attributes);
        this.effects = List.copyOf(effects);
        this.baseDamage = baseDamage;
        this.baseSpiritCost = baseSpiritCost;
        this.baseCooldown = baseCooldown;
        this.baseRange = baseRange;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public SourceType getSource() {
        return source;
    }

    public CarrierType getCarrier() {
        return carrier;
    }

    public FormType getForm() {
        return form;
    }

    public List<AttributeType> getAttributes() {
        return attributes;
    }

    public List<EffectType> getEffects() {
        return effects;
    }

    public double getBaseDamage() {
        return baseDamage;
    }

    public double getBaseSpiritCost() {
        return baseSpiritCost;
    }

    public double getBaseCooldown() {
        return baseCooldown;
    }

    public double getBaseRange() {
        return baseRange;
    }

    public Spell copy() {
        return new Spell(
            this.id,
            this.name,
            this.source,
            this.carrier,
            this.form,
            new ArrayList<>(this.attributes),
            new ArrayList<>(this.effects),
            this.baseDamage,
            this.baseSpiritCost,
            this.baseCooldown,
            this.baseRange
        );
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Id", id);
        tag.putString("Name", name);
        tag.putString("Source", source.name());
        tag.putString("Carrier", carrier.name());
        tag.putString("Form", form.name());
        tag.putDouble("BaseDamage", baseDamage);
        tag.putDouble("BaseSpiritCost", baseSpiritCost);
        tag.putDouble("BaseCooldown", baseCooldown);
        tag.putDouble("BaseRange", baseRange);

        ListTag attrList = new ListTag();
        for (AttributeType attribute : attributes) {
            attrList.add(StringTag.valueOf(attribute.name()));
        }
        tag.put("Attributes", attrList);

        ListTag effectList = new ListTag();
        for (EffectType effect : effects) {
            effectList.add(StringTag.valueOf(effect.name()));
        }
        tag.put("Effects", effectList);
        return tag;
    }

    public static Spell fromTag(CompoundTag tag) {
        String id = tag.getString("Id");
        String name = tag.getString("Name");
        SourceType source = SourceType.valueOf(tag.getString("Source"));
        CarrierType carrier = CarrierType.valueOf(tag.getString("Carrier"));
        FormType form = FormType.valueOf(tag.getString("Form"));
        double damage = tag.getDouble("BaseDamage");
        double spirit = tag.getDouble("BaseSpiritCost");
        double cooldown = tag.getDouble("BaseCooldown");
        double range = tag.getDouble("BaseRange");

        List<AttributeType> attributes = new ArrayList<>();
        ListTag attrList = tag.getList("Attributes", Tag.TAG_STRING);
        for (Tag element : attrList) {
            String value = element.getAsString();
            attributes.add(AttributeType.valueOf(value.toUpperCase(Locale.ROOT)));
        }

        List<EffectType> effects = new ArrayList<>();
        ListTag effList = tag.getList("Effects", Tag.TAG_STRING);
        for (Tag element : effList) {
            String value = element.getAsString();
            effects.add(EffectType.valueOf(value.toUpperCase(Locale.ROOT)));
        }

        return new Spell(id, name, source, carrier, form, attributes, effects, damage, spirit, cooldown, range);
    }
}
