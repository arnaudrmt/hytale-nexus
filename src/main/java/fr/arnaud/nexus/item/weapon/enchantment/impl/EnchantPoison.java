package fr.arnaud.nexus.item.weapon.enchantment.impl;

import fr.arnaud.nexus.item.weapon.enchantment.EnchantEffectUtil;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentDefinition;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentRegistry;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentStatDefinition;
import fr.arnaud.nexus.item.weapon.enchantment.event.EnchantEffectHandler;
import fr.arnaud.nexus.item.weapon.enchantment.event.NexusEnchantEvent;

public final class EnchantPoison implements EnchantEffectHandler {

    public static final EnchantPoison INSTANCE = new EnchantPoison();
    public static final String ENCHANT_ID = "Enchant_Poison";

    public static final String STAT_POISON_DURATION = "PoisonDuration";

    private EnchantPoison() {
    }

    @Override
    public void onHit(NexusEnchantEvent event, int enchantLevel) {
        EnchantmentDefinition def = EnchantmentRegistry.getInstance().getDefinition(ENCHANT_ID);
        if (def == null) return;

        EnchantmentStatDefinition durationStat = def.getEnchantmentStatById(STAT_POISON_DURATION);
        if (durationStat == null) return;

        float duration = (float) durationStat.getStatValueForLevel(enchantLevel);
        EnchantEffectUtil.applyEffect(event.target(), event.cmd(), "Poison", duration);
    }
}
