package fr.arnaud.nexus.item.weapon.enchantment.impl;

import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentDefinition;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentRegistry;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentStatDefinition;
import fr.arnaud.nexus.item.weapon.enchantment.event.EnchantEffectHandler;
import fr.arnaud.nexus.item.weapon.enchantment.event.NexusEnchantEvent;

public final class EnchantFreezeAspect implements EnchantEffectHandler {

    public static final EnchantFreezeAspect INSTANCE = new EnchantFreezeAspect();
    private static final String ENCHANT_ID = "Enchant_FreezeAspect";

    private EnchantFreezeAspect() {
    }

    @Override
    public void onHit(NexusEnchantEvent event, int enchantLevel) {
        EnchantmentDefinition def = EnchantmentRegistry.get().getDefinition(ENCHANT_ID);
        if (def == null) return;

        EnchantmentStatDefinition stat = def.getStat("StunDuration");
        if (stat == null) return;

        float duration = (float) stat.getValue(enchantLevel);
        EnchantEffectUtil.applyEffect(event.target(), event.cmd(), "Freeze", duration);
    }
}
