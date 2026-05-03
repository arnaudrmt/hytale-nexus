package fr.arnaud.nexus.item.weapon.enchantment.impl;

import fr.arnaud.nexus.item.weapon.enchantment.event.EnchantEffectHandler;

public final class EnchantStaminaBoost implements EnchantEffectHandler {

    public static final EnchantStaminaBoost INSTANCE = new EnchantStaminaBoost();
    public static final String ENCHANT_ID = "Enchant_StaminaBoost";

    public static final String STAT_STAMINA_BOOST = "StaminaBonus";

    private EnchantStaminaBoost() {
    }
}
