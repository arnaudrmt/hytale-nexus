package fr.arnaud.nexus.item.weapon.enchantment.impl;

import fr.arnaud.nexus.item.weapon.enchantment.event.EnchantEffectHandler;

/**
 * Critical Strike — chance to deal double damage.
 * Logic handled directly in OnHitSystem where the Damage object
 * is still mutable via setAmount. Registered here for the enchant bus.
 */
public final class EnchantCriticalStrike implements EnchantEffectHandler {
    public static final EnchantCriticalStrike INSTANCE = new EnchantCriticalStrike();

    private EnchantCriticalStrike() {
    }
}
