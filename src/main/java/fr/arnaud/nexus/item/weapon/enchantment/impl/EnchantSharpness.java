package fr.arnaud.nexus.item.weapon.enchantment.impl;

import fr.arnaud.nexus.item.weapon.enchantment.event.EnchantEffectHandler;

public final class EnchantSharpness implements EnchantEffectHandler {

    public static final EnchantSharpness INSTANCE = new EnchantSharpness();
    public static final String ENCHANT_ID = "Enchant_Sharpness";

    public static final String STAT_DAMAGE_MULTIPLIER = "DamageMultiplier";

    private EnchantSharpness() {
    }
}
