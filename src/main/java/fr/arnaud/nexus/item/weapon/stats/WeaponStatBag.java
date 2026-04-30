package fr.arnaud.nexus.item.weapon.stats;

public final class WeaponStatBag {

    public final double damageMultiplier;
    public final double healthBoost;
    public final double movementSpeedBoost;

    public WeaponStatBag(double damageMultiplier, double healthBoost, double movementSpeedBoost) {
        this.damageMultiplier = damageMultiplier;
        this.healthBoost = healthBoost;
        this.movementSpeedBoost = movementSpeedBoost;
    }

    public static WeaponStatBag empty() {
        return new WeaponStatBag(1.0, 0.0, 0.0);
    }
}
