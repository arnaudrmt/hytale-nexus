package fr.arnaud.nexus.item.weapon.enchantment.impl;

import fr.arnaud.nexus.item.weapon.enchantment.event.EnchantEffectHandler;

/**
 * Stamina Boost — passive max stamina bonus only.
 * Applied by {@link fr.arnaud.nexus.item.weapon.system.WeaponPassiveApplicator}
 * via {@link fr.arnaud.nexus.feature.ressource.PlayerStatsManager#setMaxStaminaBonus}.
 * No runtime trigger needed.
 */
public final class EnchantStaminaBoost implements EnchantEffectHandler {
    public static final EnchantStaminaBoost INSTANCE = new EnchantStaminaBoost();

    private EnchantStaminaBoost() {
    }
}
