package fr.arnaud.nexus.item.weapon.enchantment.impl;

import fr.arnaud.nexus.item.weapon.enchantment.event.EnchantEffectHandler;

/**
 * Fortitude — chance to completely negate an incoming hit.
 * Logic handled in OnReceiveHitSystem where damage.setAmount(0) can be called.
 */
public final class EnchantFortitude implements EnchantEffectHandler {
    public static final EnchantFortitude INSTANCE = new EnchantFortitude();

    private EnchantFortitude() {
    }
}
