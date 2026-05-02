package fr.arnaud.nexus.item.weapon.enchantment.impl;

import fr.arnaud.nexus.item.weapon.enchantment.event.EnchantEffectHandler;

public final class EnchantSwiftness implements EnchantEffectHandler {

    public static final EnchantSwiftness INSTANCE = new EnchantSwiftness();
    public static final String ENCHANT_ID = "Enchant_Swiftness";

    public static final String STAT_SWIFTNESS_BOOST = "Swiftness";

    private EnchantSwiftness() {
    }
}
