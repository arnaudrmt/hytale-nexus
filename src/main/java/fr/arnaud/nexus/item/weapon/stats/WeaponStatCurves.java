package fr.arnaud.nexus.item.weapon.stats;

/**
 * Stat growth parameters for a single weapon quality tier.
 * Cap fields of 0 mean uncapped.
 */
public record WeaponStatCurves(
    float levelCostBase,
    float levelCostRate,
    float damageMultiplierBase,
    float damageMultiplierPerLevel,
    float healthBonusBase,
    float healthBonusPerLevel,
    float healthBonusCap,
    float movementSpeedBonusBase,
    float movementSpeedBonusPerLevel,
    float movementSpeedBonusCap
) {
}
