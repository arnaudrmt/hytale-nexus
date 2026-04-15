package fr.arnaud.nexus.item.weapon.enchantment.impl;

import fr.arnaud.nexus.item.weapon.enchantment.event.EnchantEffectHandler;

/**
 * Swiftness — passive movement speed bonus only.
 * Applied by {@link fr.arnaud.nexus.item.weapon.system.WeaponPassiveApplicator}
 * via {@link fr.arnaud.nexus.feature.ressource.PlayerStatsManager#addMovementSpeed}.
 * No runtime trigger needed.
 */
public final class EnchantMovementSpeed implements EnchantEffectHandler {
    public static final EnchantMovementSpeed INSTANCE = new EnchantMovementSpeed();

    private EnchantMovementSpeed() {
    }
    // No runtime triggers — all handled passively on equip.
}
