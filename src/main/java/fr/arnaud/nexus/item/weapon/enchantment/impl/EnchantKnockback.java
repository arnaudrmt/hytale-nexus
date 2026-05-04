package fr.arnaud.nexus.item.weapon.enchantment.impl;

import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentDefinition;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentRegistry;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentStatDefinition;
import fr.arnaud.nexus.item.weapon.enchantment.event.EnchantEffectHandler;
import fr.arnaud.nexus.item.weapon.enchantment.event.NexusEnchantEvent;
import fr.arnaud.nexus.util.KnockbackUtil;

public final class EnchantKnockback implements EnchantEffectHandler {

    public static final EnchantKnockback INSTANCE = new EnchantKnockback();
    public static final String ENCHANT_ID = "Enchant_Knockback";

    public static final String STAT_KNOCKBACK_FORCE = "KnockbackForce";

    private EnchantKnockback() {
    }

    @Override
    public void onHit(NexusEnchantEvent event, int enchantLevel) {
        EnchantmentDefinition def = EnchantmentRegistry.getInstance().getDefinition(ENCHANT_ID);
        if (def == null) return;

        EnchantmentStatDefinition stat = def.getEnchantmentStatById(STAT_KNOCKBACK_FORCE);
        if (stat == null) return;

        float force = (float) stat.getStatValueForLevel(enchantLevel);
        KnockbackUtil.applyRadialKnockbackImpulse(
            event.attacker(), event.target(), force, event.cmd());
    }
}
