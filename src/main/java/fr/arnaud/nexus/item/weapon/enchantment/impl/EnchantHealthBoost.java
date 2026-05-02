package fr.arnaud.nexus.item.weapon.enchantment.impl;

import fr.arnaud.nexus.item.weapon.enchantment.event.EnchantEffectHandler;

public final class EnchantHealthBoost implements EnchantEffectHandler {

    public static final EnchantHealthBoost INSTANCE = new EnchantHealthBoost();
    public static final String ENCHANT_ID = "Enchant_HealthBoost";

    public static final String STAT_HEALTH_BOOST = "HealthBoost";

    private EnchantHealthBoost() {
    }
}
