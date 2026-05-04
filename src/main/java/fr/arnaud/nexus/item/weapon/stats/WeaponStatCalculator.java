package fr.arnaud.nexus.item.weapon.stats;

import fr.arnaud.nexus.item.weapon.data.WeaponBsonSchema;
import fr.arnaud.nexus.math.StatMath;
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
        return (float) StatMath.exponential(curves.levelCostBase(), curves.levelCostRate(), level);
    }

    public static float calculateDamageMultiplier(BsonDocument doc) {
        int level = WeaponBsonSchema.readLevel(doc);
        WeaponStatCurves curves = resolveCurves(doc);
        return (float) StatMath.additive(curves.damageMultiplierBase(), curves.damageMultiplierPerLevel(), level);
    }

    public static float calculateHealthBonus(BsonDocument doc) {
        int level = WeaponBsonSchema.readLevel(doc);
        WeaponStatCurves curves = resolveCurves(doc);
        float raw = (float) StatMath.additive(curves.healthBonusBase(), curves.healthBonusPerLevel(), level);
        return (float) StatMath.cappedValue(raw, curves.healthBonusCap());
    }


    public static float calculateMovementSpeedBonus(BsonDocument doc) {
        int currentLevel = WeaponBsonSchema.readLevel(doc);
        WeaponStatCurves curves = resolveCurves(doc);
        float raw = (float) StatMath.additive(curves.movementSpeedBonusBase(), curves.movementSpeedBonusPerLevel(), currentLevel);
        return (float) StatMath.cappedValue(raw, curves.movementSpeedBonusCap());
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

        return new PassiveStats(damageMultiplier, healthBonus, movementSpeed);
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
