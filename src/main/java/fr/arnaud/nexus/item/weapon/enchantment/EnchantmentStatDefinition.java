package fr.arnaud.nexus.item.weapon.enchantment;

import java.util.Collections;
import java.util.Map;

/**
 * Describes one stat within an {@link EnchantmentDefinition}.
 *
 * <p>Flat stats add a fixed value directly to the player's stat.
 * Curve stats multiply a base stat value by the stored multiplier.
 */
public final class EnchantmentStatDefinition {

    public enum StatType {
        /**
         * Value is added directly: +10 Health Boost
         */
        FLAT,
        /**
         * Value is a multiplier applied to a base stat: ×1.5 Damage Multiplier
         */
        CURVE
    }

    private final String id;
    private final String displayName;
    private final StatType type;
    /**
     * level (1-based) → stat value
     */
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

    /**
     * Returns the raw value for the given level, or 0 if not defined.
     * For Flat: this is the additive bonus.
     * For Curve: this is the multiplier (e.g. 1.5).
     */
    public double getValue(int level) {
        return values.getOrDefault(level, 0.0);
    }

    /**
     * For Curve stats: applies the multiplier to the given base value.
     * For Flat stats: returns the flat value directly (base is ignored).
     */
    public double compute(int level, double base) {
        double raw = getValue(level);
        return type == StatType.CURVE ? base * raw : raw;
    }

    /**
     * Returns a display string for the stat at the given level.
     * Flat  → "+10 Health Boost"
     * Curve → "×1.50 Damage Multiplier"
     */
    public String format(int level) {
        double raw = getValue(level);
        if (type == StatType.CURVE) {
            return String.format("×%.2f %s", raw, displayName);
        } else {
            // Drop the decimal if it's a whole number
            if (raw == Math.floor(raw)) {
                return "+" + (int) raw + " " + displayName;
            }
            return String.format("+%.1f %s", raw, displayName);
        }
    }
}
