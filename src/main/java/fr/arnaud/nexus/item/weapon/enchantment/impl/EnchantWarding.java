package fr.arnaud.nexus.item.weapon.enchantment.impl;

import fr.arnaud.nexus.item.weapon.enchantment.event.EnchantEffectHandler;

public final class EnchantWarding implements EnchantEffectHandler {

    public static final EnchantWarding INSTANCE = new EnchantWarding();
    public static final String ENCHANT_ID = "Enchant_Warding";

    public static final String STAT_WARDING_PERCENT = "WardingPercent";
    public static final String STAT_WARDING_CAP = "WardingCap";

    private EnchantWarding() {
    }
}
