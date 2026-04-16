package fr.arnaud.nexus.item.weapon.enchantment.impl;

import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentDefinition;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentRegistry;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentStatDefinition;
import fr.arnaud.nexus.item.weapon.enchantment.event.EnchantEffectHandler;
import fr.arnaud.nexus.item.weapon.enchantment.event.NexusEnchantEvent;

/**
 * Poison — applies the Poison entity effect on hit.
 * Duration and damage per tick both scale with enchant level via JSON stats.
 * <p>
 * Note: the Poison effect asset controls the actual damage tick rate.
 * PoisonDamage here is stored as metadata for future custom poison damage
 * if the engine's built-in effect damage is insufficient.
 */
public final class EnchantPoison implements EnchantEffectHandler {

    public static final EnchantPoison INSTANCE = new EnchantPoison();
    private static final String ENCHANT_ID = "Enchant_Poison";

    private EnchantPoison() {
    }

    @Override
    public void onHit(NexusEnchantEvent event, int enchantLevel) {
        EnchantmentDefinition def = EnchantmentRegistry.get().getDefinition(ENCHANT_ID);
        if (def == null) return;

        EnchantmentStatDefinition durationStat = def.getStat("PoisonDuration");
        if (durationStat == null) return;

        float duration = (float) durationStat.getValue(enchantLevel);
        EnchantEffectUtil.applyEffect(event.target(), event.cmd(), "Poison", duration);
    }
}
