package fr.arnaud.nexus.item.weapon.enchantment.impl;

import fr.arnaud.nexus.item.weapon.enchantment.event.EnchantEffectHandler;

public final class EnchantCriticalStrike implements EnchantEffectHandler {

    public static final EnchantCriticalStrike INSTANCE = new EnchantCriticalStrike();
    public static final String ENCHANT_ID = "Enchant_CriticalStrike";

    public static final String STAT_CRIT_CHANCE = "CritChance";

    private EnchantCriticalStrike() {
    }
}
