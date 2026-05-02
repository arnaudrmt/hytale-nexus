package fr.arnaud.nexus.item.weapon.stats;

import fr.arnaud.nexus.item.weapon.data.EnchantmentSlot;
import fr.arnaud.nexus.item.weapon.data.WeaponBsonSchema;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentDefinition;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentRegistry;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentStatDefinition;
import org.bson.BsonDocument;

/**
 * All stat derivations from a weapon BsonDocument.
 * Passive enchantment bonuses are handled in where relevant.
 */
public final class WeaponStatCalculator {

    private WeaponStatCalculator() {
    }

    public static float calculateUpgradeCost(BsonDocument doc) {
        int level = WeaponBsonSchema.readLevel(doc);
        WeaponStatCurves curves = resolveCurves(doc);
        return curves.upgradeCostBase() * (float) Math.pow(curves.upgradeCostMultiplierPerLevel(), level - 1);
    }

    public static float calculateDamageMultiplier(BsonDocument doc) {
        int level = WeaponBsonSchema.readLevel(doc);
        WeaponStatCurves curves = resolveCurves(doc);
        return curves.damageMultiplierBase() + (curves.damageMultiplierPerLevel() * (level - 1));
    }

    public static float calculateHealthBonus(BsonDocument doc) {
        int level = WeaponBsonSchema.readLevel(doc);
        WeaponStatCurves curves = resolveCurves(doc);
        float raw = curves.healthBonusBase() + (curves.healthBonusPerLevel() * level);
        return curves.healthBonusCap() > 0 ? Math.min(raw, curves.healthBonusCap()) : raw;
    }


    public static float calculateMovementSpeedBonus(BsonDocument doc) {
        int currentLevel = WeaponBsonSchema.readLevel(doc);
        WeaponStatCurves curves = resolveCurves(doc);
        float raw = curves.movementSpeedBonusBase() + (curves.movementSpeedBonusPerLevel() * currentLevel);
        return curves.movementSpeedBonusCap() > 0 ? Math.min(raw, curves.movementSpeedBonusCap()) : raw;
    }

    /**
     * Computes all three passive stat bonuses in one pass, including contributions
     * from unlocked enchantment slots.
     */
    public static PassiveStats calculatePassiveStats(BsonDocument doc, int forLevel) {
        WeaponStatCurves curves = resolveCurves(doc);

        double damageMultiplier = linearGrowth(curves.damageMultiplierBase(), curves.damageMultiplierPerLevel(), forLevel);
        double healthBonus = linearGrowth(curves.healthBonusBase(), curves.healthBonusPerLevel(), forLevel);
        double movementSpeed = linearGrowth(curves.movementSpeedBonusBase(), curves.movementSpeedBonusPerLevel(), forLevel);

        for (EnchantmentSlot slot : WeaponBsonSchema.readEnchantmentSlots(doc)) {
            if (!slot.isUnlocked()) continue;
            EnchantmentDefinition def = EnchantmentRegistry.getInstance().getDefinition(slot.chosen());
            if (def == null) continue;
            int enchantLevel = slot.currentLevel();
        }

        return new PassiveStats(damageMultiplier, healthBonus, movementSpeed);
    }


    private static double enchantStatContribution(EnchantmentDefinition def, String statKey,
                                                  int level, double baseForCurve) {
        EnchantmentStatDefinition stat = def.getEnchantmentStatById(statKey);
        if (stat == null) return 0.0;
        return stat.computeEffectiveValue(level, baseForCurve);
    }

    private static double linearGrowth(double base, double perLevel, int level) {
        return base + perLevel * (level - 1);
    }

    public static WeaponStatCurves resolveCurves(BsonDocument doc) {
        int qualityValue = WeaponBsonSchema.readQualityValue(doc);
        return WeaponStatRegistry.getInstance().getCurves(qualityValue);
    }

    // Flat snapshot of the three passive stats derived from a weapon document.
    public record PassiveStats(double damageMultiplier, double healthBonus, double movementSpeedBonus) {
    }
}
