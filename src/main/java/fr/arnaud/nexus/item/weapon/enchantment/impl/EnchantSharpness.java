package fr.arnaud.nexus.item.weapon.enchantment.impl;

import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentDefinition;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentRegistry;
import fr.arnaud.nexus.item.weapon.enchantment.event.EnchantEffectHandler;
import fr.arnaud.nexus.item.weapon.enchantment.event.NexusEnchantEvent;

/**
 * Sharpness — passive damage multiplier only.
 * Applied by {@link fr.arnaud.nexus.item.weapon.system.WeaponPassiveApplicator}
 * as a MULTIPLICATIVE modifier on WeaponDamage max.
 * No runtime trigger needed.
 */
public final class EnchantSharpness implements EnchantEffectHandler {
    public static final EnchantSharpness INSTANCE = new EnchantSharpness();

    private EnchantSharpness() {
    }
    // No runtime triggers — all handled passively on equip.

    @Override
    public void onHit(NexusEnchantEvent event, int enchantLevel) {
        EnchantmentDefinition def = EnchantmentRegistry.get().getDefinition("Enchant_Sharpness");
        if (def == null) return;
        double multiplier = def.getStat("DamageMultiplier").getValue(enchantLevel);
        event.damage();
        System.out.println("here");// — but Damage object isn't on the event, only the amount
    }
}
