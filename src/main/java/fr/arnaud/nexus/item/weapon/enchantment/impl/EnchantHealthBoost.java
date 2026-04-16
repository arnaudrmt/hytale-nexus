package fr.arnaud.nexus.item.weapon.enchantment.impl;

import fr.arnaud.nexus.item.weapon.enchantment.event.EnchantEffectHandler;

public final class EnchantHealthBoost implements EnchantEffectHandler {
    public static final EnchantHealthBoost INSTANCE = new EnchantHealthBoost();

    private EnchantHealthBoost() {
    }
}
