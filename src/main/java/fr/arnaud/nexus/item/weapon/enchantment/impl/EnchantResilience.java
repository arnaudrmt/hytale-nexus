package fr.arnaud.nexus.item.weapon.enchantment.impl;

import fr.arnaud.nexus.item.weapon.enchantment.event.EnchantEffectHandler;

/**
 * Resilience — reduces incoming damage.
 * Logic handled directly in OnReceiveHitSystem where the Damage object
 * is still mutable. Registered here for the enchant bus.
 */
public final class EnchantResilience implements EnchantEffectHandler {
    public static final EnchantResilience INSTANCE = new EnchantResilience();

    private EnchantResilience() {
    }
}
