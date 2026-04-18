package fr.arnaud.nexus.item.weapon.enchantment.impl;

import fr.arnaud.nexus.item.weapon.enchantment.event.EnchantEffectHandler;

/**
 * Warding — reduces incoming damage by a percentage, capped at a max flat value.
 * Logic handled in OnReceiveHitSystem where the Damage object is accessible.
 */
public final class EnchantWarding implements EnchantEffectHandler {
    public static final EnchantWarding INSTANCE = new EnchantWarding();

    private EnchantWarding() {
    }
}
