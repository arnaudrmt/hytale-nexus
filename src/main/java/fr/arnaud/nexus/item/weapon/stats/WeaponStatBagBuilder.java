package fr.arnaud.nexus.item.weapon.stats;

import fr.arnaud.nexus.item.weapon.component.WeaponInstanceComponent;
import fr.arnaud.nexus.item.weapon.data.EnchantmentSlot;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentDefinition;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentRegistry;

public final class WeaponStatBagBuilder {

    private static final String KEY_DAMAGE_BONUS = "DamageBonus";
    private static final String KEY_HEALTH_BOOST = "HealthBoost";
    private static final String KEY_MOVEMENT_SPEED = "MovementSpeedBoost";

    private WeaponStatBagBuilder() {}

    /**
     * Builds a fully computed WeaponStatBag from the weapon's base curves
     * plus any stat-modifier enchantments that are unlocked.
     */
    public static WeaponStatBag build(WeaponInstanceComponent instance) {
        double damageMultiplier = instance.damageMultiplierCurve;
        double healthBoost = instance.healthBoostCurve;
        double movementSpeed = instance.movementSpeedCurve;

        for (EnchantmentSlot slot : instance.enchantmentSlots) {
            if (!slot.isUnlocked()) continue;

            EnchantmentDefinition def = EnchantmentRegistry.get().getDefinition(slot.chosen());
            if (def == null) continue;

            int level = slot.currentLevel();
            damageMultiplier += def.getValue(level, KEY_DAMAGE_BONUS, 0.0);
            healthBoost += def.getValue(level, KEY_HEALTH_BOOST, 0.0);
            movementSpeed += def.getValue(level, KEY_MOVEMENT_SPEED, 0.0);
        }

        return new WeaponStatBag(damageMultiplier, healthBoost, movementSpeed);
    }
}
