package fr.arnaud.nexus.item.weapon.enchantment.impl;

import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentDefinition;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentRegistry;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentStatDefinition;
import fr.arnaud.nexus.item.weapon.enchantment.event.EnchantEffectHandler;
import fr.arnaud.nexus.item.weapon.enchantment.event.NexusEnchantEvent;

public final class EnchantCripple implements EnchantEffectHandler {

    public static final EnchantCripple INSTANCE = new EnchantCripple();
    private static final String ENCHANT_ID = "Enchant_Cripple";

    private EnchantCripple() {
    }

    @Override
    public void onHit(NexusEnchantEvent event, int enchantLevel) {
        EnchantmentDefinition def = EnchantmentRegistry.get().getDefinition(ENCHANT_ID);
        if (def == null) return;

        EnchantmentStatDefinition stat = def.getEnchantmentStatById("CrippleDuration");
        if (stat == null) return;

        float duration = (float) stat.getStatValueForLevel(enchantLevel);
        EnchantEffectUtil.applyEffect(event.target(), event.cmd(), "Slow", duration);
    }
}
