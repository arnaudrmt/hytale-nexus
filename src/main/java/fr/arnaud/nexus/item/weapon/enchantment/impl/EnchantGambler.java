package fr.arnaud.nexus.item.weapon.enchantment.impl;

import fr.arnaud.nexus.item.weapon.enchantment.event.EnchantEffectHandler;

public final class EnchantGambler implements EnchantEffectHandler {

    public static final EnchantGambler INSTANCE = new EnchantGambler();
    private static final String ENCHANT_ID = "Enchant_Gambler";

    public static final String STAT_DOUBLE = "GamblerDoubleChance";
    public static final String STAT_HALVE = "GamblerHalveChance";

    private EnchantGambler() {
    }
}
