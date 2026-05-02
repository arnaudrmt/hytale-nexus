package fr.arnaud.nexus.item.weapon.enchantment.impl;

import fr.arnaud.nexus.item.weapon.enchantment.event.EnchantEffectHandler;

public final class EnchantThorns implements EnchantEffectHandler {

    public static final EnchantThorns INSTANCE = new EnchantThorns();
    public static final String ENCHANT_ID = "Enchant_Thorns";

    public static final String STAT_THORNS_PERCENT = "ThornsPercent";

    private EnchantThorns() {
    }
}
