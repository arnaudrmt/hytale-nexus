package fr.arnaud.nexus.item.weapon.stats;

public record WeaponStatCurves(
    float levelCostBase,
    float levelCostCurve,
    float damageBase,
    float damageCurve,
    float healthBase,
    float healthCurve,
    float healthCap,
    float speedBase,
    float speedCurve,
    float speedCap
) {
}
