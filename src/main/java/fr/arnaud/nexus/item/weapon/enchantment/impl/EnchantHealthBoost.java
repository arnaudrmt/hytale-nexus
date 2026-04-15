package fr.arnaud.nexus.item.weapon.enchantment.impl;

import fr.arnaud.nexus.item.weapon.enchantment.event.EnchantEffectHandler;

/**
 * Health Boost — passive only.
 * The stat bonus is applied entirely by {@link fr.arnaud.nexus.item.weapon.system.WeaponPassiveApplicator}
 * on weapon equip, so no runtime trigger is needed here.
 * <p>
 * This class exists to document the enchant and as a registration point
 * in case future levels trigger additional effects.
 */
public final class EnchantHealthBoost implements EnchantEffectHandler {
    public static final EnchantHealthBoost INSTANCE = new EnchantHealthBoost();

    private EnchantHealthBoost() {
    }
    // No runtime triggers — all handled passively on equip.
}
