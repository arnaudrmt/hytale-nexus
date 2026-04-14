package fr.arnaud.nexus.item.weapon.enchantment;

public final class EnchantmentCostCalculator {

    private EnchantmentCostCalculator() {
    }

    /**
     * Cost to upgrade an enchantment to the given target level.
     * Uses baseCost * curve^(targetLevel - 1).
     */
    public static int costForLevel(EnchantmentDefinition def, int targetLevel) {
        if (targetLevel <= 1) return def.getBaseCost();
        return (int) Math.round(def.getBaseCost() * Math.pow(def.getCostCurve(), targetLevel - 1));
    }
}
