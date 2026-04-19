package fr.arnaud.nexus.item.weapon.enchantment.impl;

import fr.arnaud.nexus.item.weapon.enchantment.event.EnchantEffectHandler;

/**
 * Soul Harvest — grants bonus essence when killing a spawner mob.
 * Logic handled in SpawnerMobDeathSystem where the base essence
 * drop amount is available.
 * <p>
 * Bonus = baseEssenceDrop * multiplierAtLevel
 * L1: x0.50  L2: x0.75  L3: x1.00  L4: x1.50  L5: x2.00
 */
public final class EnchantSoulHarvest implements EnchantEffectHandler {
    public static final EnchantSoulHarvest INSTANCE = new EnchantSoulHarvest();

    private EnchantSoulHarvest() {
    }
}
