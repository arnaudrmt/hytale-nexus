package fr.arnaud.nexus.item.weapon.enchantment.impl;

import fr.arnaud.nexus.item.weapon.enchantment.event.EnchantEffectHandler;

public final class EnchantStaminaBoost implements EnchantEffectHandler {
    public static final EnchantStaminaBoost INSTANCE = new EnchantStaminaBoost();

    private EnchantStaminaBoost() {
    }
}
