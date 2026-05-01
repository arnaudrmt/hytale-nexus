package fr.arnaud.nexus.item.weapon.enchantment.impl;

import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.feature.resource.PlayerStatsManager;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentDefinition;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentRegistry;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentStatDefinition;
import fr.arnaud.nexus.item.weapon.enchantment.event.EnchantEffectHandler;
import fr.arnaud.nexus.item.weapon.enchantment.event.NexusEnchantEvent;

public final class EnchantLifeDrain implements EnchantEffectHandler {

    public static final EnchantLifeDrain INSTANCE = new EnchantLifeDrain();
    private static final String ENCHANT_ID = "Enchant_LifeDrain";

    private EnchantLifeDrain() {
    }

    @Override
    public void onHit(NexusEnchantEvent event, int enchantLevel) {
        EnchantmentDefinition def = EnchantmentRegistry.get().getDefinition(ENCHANT_ID);
        if (def == null) return;

        EnchantmentStatDefinition stat = def.getEnchantmentStatById("LifeDrainAmount");
        if (stat == null) return;

        float healAmount = (float) stat.getStatValueForLevel(enchantLevel);
        PlayerStatsManager psm = Nexus.getInstance().getPlayerStatsManager();
        if (!psm.isReady()) return;

        psm.addHealth(event.attacker(), event.store(), healAmount);
    }
}
