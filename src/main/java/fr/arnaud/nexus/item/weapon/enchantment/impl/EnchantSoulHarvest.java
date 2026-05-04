package fr.arnaud.nexus.item.weapon.enchantment.impl;

import fr.arnaud.nexus.item.weapon.enchantment.event.EnchantEffectHandler;

public final class EnchantSoulHarvest implements EnchantEffectHandler {

    public static final EnchantSoulHarvest INSTANCE = new EnchantSoulHarvest();
    public static final String ENCHANT_ID = "Enchant_SoulHarvest";

    public static final String STAT_HARVEST_MULTIPLIER = "SoulHarvestMultiplier";

    private EnchantSoulHarvest() {
    }
}
