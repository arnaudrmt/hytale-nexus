package fr.arnaud.nexus.item.weapon.enchantment.impl;

import fr.arnaud.nexus.item.weapon.enchantment.event.EnchantEffectHandler;

public final class EnchantResilience implements EnchantEffectHandler {
    
    public static final EnchantResilience INSTANCE = new EnchantResilience();
    public static final String ENCHANT_ID = "Enchant_Resilience";

    public static final String STAT_DAMAGE_REDUCTION = "DamageReduction";

    private EnchantResilience() {
    }
}
