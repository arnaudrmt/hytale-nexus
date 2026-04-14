package fr.arnaud.nexus.item.weapon.enchantment;

import fr.arnaud.nexus.item.weapon.data.WeaponTag;

import java.util.Collections;
import java.util.List;

public final class EnchantmentDefinition {

    private final String id;
    private final String name;
    private final String description;
    private final String icon;
    private final WeaponTag compatibleTag;
    private final int baseCost;
    private final double costCurve;
    private final int maxLevel;
    private final List<EnchantmentStatDefinition> stats;

    public EnchantmentDefinition(
        String id,
        String name,
        String description,
        String icon,
        WeaponTag compatibleTag,
        int baseCost,
        double costCurve,
        int maxLevel,
        List<EnchantmentStatDefinition> stats
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.compatibleTag = compatibleTag;
        this.baseCost = baseCost;
        this.costCurve = costCurve;
        this.maxLevel = maxLevel;
        this.stats = Collections.unmodifiableList(stats);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getIcon() {
        return icon;
    }

    public WeaponTag getCompatibleTag() {
        return compatibleTag;
    }

    public int getBaseCost() {
        return baseCost;
    }

    public double getCostCurve() {
        return costCurve;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public List<EnchantmentStatDefinition> getStats() {
        return stats;
    }

    /**
     * Convenience: find a stat definition by its id, or null.
     */
    public EnchantmentStatDefinition getStat(String statId) {
        for (EnchantmentStatDefinition s : stats) {
            if (s.getId().equals(statId)) return s;
        }
        return null;
    }
}
