package fr.arnaud.nexus.item.weapon.enchantment.impl;

import fr.arnaud.nexus.item.weapon.enchantment.event.EnchantEffectHandler;

/**
 * Swiftness — passive movement speed bonus only.
 * Applied by {@link fr.arnaud.nexus.item.weapon.system.WeaponPassiveApplicator}
 * via {@link fr.arnaud.nexus.feature.ressource.PlayerStatsManager#addMovementSpeed}.
 * No runtime trigger needed.
 */
public final class EnchantSwiftness implements EnchantEffectHandler {
    public static final EnchantSwiftness INSTANCE = new EnchantSwiftness();

    private EnchantSwiftness() {
    }
    // No runtime triggers — all handled passively on equip.
}
