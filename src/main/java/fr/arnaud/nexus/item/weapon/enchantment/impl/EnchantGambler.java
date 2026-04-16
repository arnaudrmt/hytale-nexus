package fr.arnaud.nexus.item.weapon.enchantment.impl;

import fr.arnaud.nexus.item.weapon.enchantment.event.EnchantEffectHandler;

/**
 * Gambler — randomly doubles or halves outgoing damage.
 * <p>
 * Since we cannot modify the in-flight Damage object from the bus,
 * and applyDamage is not available on CommandBuffer, Gambler instead
 * works by modifying the damage multiplier BEFORE it reaches the bus.
 * <p>
 * This is handled specially in OnHitSystem — see EnchantmentDamageInterceptor.
 * This class exists as a registration marker only.
 * <p>
 * The actual roll logic lives in OnHitSystem.applyGamblerRoll().
 */
public final class EnchantGambler implements EnchantEffectHandler {

    public static final EnchantGambler INSTANCE = new EnchantGambler();
    private static final String ENCHANT_ID = "Enchant_Gambler";

    // Accessed by OnHitSystem
    public static final String STAT_DOUBLE = "GamblerDoubleChance";
    public static final String STAT_HALVE = "GamblerHalveChance";

    private EnchantGambler() {
    }

    // No onHit override — handled in OnHitSystem before damage is set
}
