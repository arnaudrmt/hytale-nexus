package fr.arnaud.nexus.item.weapon.enchantment;

import fr.arnaud.nexus.math.StatMath;

import java.util.Collections;
import java.util.Map;

/**
 * Flat stats add a fixed value directly to the player's stat.
 * Curve stats multiply a base stat value by the stored multiplier.
 */
public final class EnchantmentStatDefinition {

    private final String id;
    private final String displayName;
    private final StatMath.GrowthType type;

    private final Map<Integer, Double> valuesByLevel;

    public EnchantmentStatDefinition(String id, String displayName,
                                     StatMath.GrowthType type, Map<Integer, Double> valuesByLevel) {
        this.id = id;
        this.displayName = displayName;
        this.type = type;
        this.valuesByLevel = Collections.unmodifiableMap(valuesByLevel);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public StatMath.GrowthType getType() {
        return type;
    }

    public double getStatValueForLevel(int level) {
        return valuesByLevel.getOrDefault(level, 0.0);
    }

    public double computeValue(int level, double base) {
        double raw = getStatValueForLevel(level);
        return type == StatMath.GrowthType.SCALAR ? base * raw : raw;
    }
}
