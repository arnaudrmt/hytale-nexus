package fr.arnaud.nexus.item.weapon.enchantment;

import fr.arnaud.nexus.item.weapon.data.WeaponTag;

import java.util.Collections;
import java.util.List;

public record EnchantmentDefinition(String id, String name, String description, String icon, WeaponTag compatibleTag,
                                    int baseCost, double costMultiplierPerLevel, int maxLevel,
                                    List<EnchantmentStatDefinition> stats) {

    public EnchantmentDefinition(
        String id,
        String name,
        String description,
        String icon,
        WeaponTag compatibleTag,
        int baseCost,
        double costMultiplierPerLevel,
        int maxLevel,
        List<EnchantmentStatDefinition> stats
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.compatibleTag = compatibleTag;
        this.baseCost = baseCost;
        this.costMultiplierPerLevel = costMultiplierPerLevel;
        this.maxLevel = maxLevel;
        this.stats = Collections.unmodifiableList(stats);
    }

    public EnchantmentStatDefinition getEnchantmentStatById(String statId) {
        for (EnchantmentStatDefinition s : stats) {
            if (s.getId().equals(statId)) return s;
        }
        return null;
    }

    public int costForLevel(int targetLevel) {
        if (targetLevel <= 1) return baseCost();
        return (int) Math.round(baseCost() * Math.pow(costMultiplierPerLevel, targetLevel - 1));
    }
}
