package fr.arnaud.nexus.item.weapon.enchantment.impl;

import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.feature.resource.PlayerStatsManager;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentDefinition;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentRegistry;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentStatDefinition;
import fr.arnaud.nexus.item.weapon.enchantment.event.EnchantEffectHandler;
import fr.arnaud.nexus.item.weapon.enchantment.event.NexusEnchantEvent;

public final class EnchantVampirism implements EnchantEffectHandler {

    public static final EnchantVampirism INSTANCE = new EnchantVampirism();
    public static final String ENCHANT_ID = "Enchant_Vampirism";

    private static final String STAT_HEALTH_REGEN = "HealthRegen";

    private EnchantVampirism() {
    }

    @Override
    public void onKill(NexusEnchantEvent event, int enchantLevel) {

        EnchantmentDefinition def = EnchantmentRegistry.getInstance().getDefinition(ENCHANT_ID);
        if (def == null) return;

        EnchantmentStatDefinition stat = def.getEnchantmentStatById(STAT_HEALTH_REGEN);
        if (stat == null) return;

        PlayerStatsManager psm = Nexus.getInstance().getPlayerStatsManager();
        if (!psm.isReady()) return;

        float maxHealth = psm.getMaxHealth(event.attacker(), event.store());
        float healAmount = (float) stat.computeEffectiveValue(enchantLevel, maxHealth);
        psm.addHealth(event.attacker(), event.store(), healAmount);
    }
}
