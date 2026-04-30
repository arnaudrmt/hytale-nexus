package fr.arnaud.nexus.item.weapon.stats;

public record WeaponStatCurves(
    float levelCostBase,
    float levelCostCurve,
    float damageBase,
    float damageCurve,
    float healthBase,
    float healthFlat,
    float healthCap,
    float speedBase,
    float speedFlat,
    float speedCap
) {
}
