package fr.arnaud.nexus.item.weapon.enchantment;

import java.util.Collections;
import java.util.Map;

/**
 * Flat stats add a fixed value directly to the player's stat.
 * Curve stats multiply a base stat value by the stored multiplier.
 */
public final class EnchantmentStatDefinition {

    public enum StatType {
        FLAT,
        CURVE
    }

    private final String id;
    private final String displayName;
    private final StatType type;

    private final Map<Integer, Double> values;

    public EnchantmentStatDefinition(String id, String displayName,
                                     StatType type, Map<Integer, Double> values) {
        this.id = id;
        this.displayName = displayName;
        this.type = type;
        this.values = Collections.unmodifiableMap(values);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public StatType getType() {
        return type;
    }

    public double getStatValueForLevel(int level) {
        return values.getOrDefault(level, 0.0);
    }

    public double computeStateValueForLevel(int level, double base) {
        double raw = getStatValueForLevel(level);
        return type == StatType.CURVE ? base * raw : raw;
    }
}
