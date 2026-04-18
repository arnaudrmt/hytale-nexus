package fr.arnaud.nexus.item.weapon.enchantment.impl;

import fr.arnaud.nexus.item.weapon.enchantment.event.EnchantEffectHandler;

/**
 * Thorns — reflects a percentage of incoming damage back at the attacker.
 * Logic handled in OnReceiveHitSystem where the Damage object is accessible.
 */
public final class EnchantThorns implements EnchantEffectHandler {
    public static final EnchantThorns INSTANCE = new EnchantThorns();

    private EnchantThorns() {
    }
}
