package fr.arnaud.nexus.item.weapon.enchantment.impl;

import fr.arnaud.nexus.item.weapon.enchantment.event.EnchantEffectHandler;

public final class EnchantMovementSpeed implements EnchantEffectHandler {
    public static final EnchantMovementSpeed INSTANCE = new EnchantMovementSpeed();

    private EnchantMovementSpeed() {
    }
}
