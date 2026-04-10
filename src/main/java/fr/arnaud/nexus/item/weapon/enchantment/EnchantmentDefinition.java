package fr.arnaud.nexus.item.weapon.enchantment;

import fr.arnaud.nexus.item.weapon.data.WeaponTag;

import java.util.Collections;
import java.util.List;

public final class EnchantmentDefinition {

    private final String enchantmentId;
    private final int level;
    private final WeaponTag compatibleTag;
    private final String behaviorId;
    private final List<StatModifierEntry> statModifiers;

    public EnchantmentDefinition(
        String enchantmentId,
        int level,
        WeaponTag compatibleTag,
        String behaviorId,
        List<StatModifierEntry> statModifiers
    ) {
        this.enchantmentId = enchantmentId;
        this.level = level;
        this.compatibleTag = compatibleTag;
        this.behaviorId = behaviorId;
        this.statModifiers = Collections.unmodifiableList(statModifiers);
    }

    public String getEnchantmentId() {
        return enchantmentId;
    }

    public int getLevel() {
        return level;
    }

    public WeaponTag getCompatibleTag() {
        return compatibleTag;
    }

    public String getBehaviorId() {
        return behaviorId;
    }

    public List<StatModifierEntry> getStatModifiers() {
        return statModifiers;
    }

    public boolean isBehavioral() {
        return behaviorId != null && !behaviorId.isBlank();
    }

    public record StatModifierEntry(String statId, ModifierType type, float value) {
    }

    public enum ModifierType {ADD, MULTIPLY}
}
