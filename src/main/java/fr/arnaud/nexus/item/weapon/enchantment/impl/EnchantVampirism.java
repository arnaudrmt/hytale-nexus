package fr.arnaud.nexus.item.weapon.enchantment.impl;

import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.feature.ressource.PlayerStatsManager;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentDefinition;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentRegistry;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentStatDefinition;
import fr.arnaud.nexus.item.weapon.enchantment.event.EnchantEffectHandler;
import fr.arnaud.nexus.item.weapon.enchantment.event.NexusEnchantEvent;

/**
 * Vampirism — restores health to the attacker when they kill an entity.
 * <p>
 * The HealthRegen stat is a Curve type, so the heal amount is:
 * multiplier * attacker's current max health
 * e.g. at level 1 with multiplier 1.5: heal = 1.5 * maxHealth
 * <p>
 * This is a runtime ON_KILL trigger — no passive stat bonus.
 */
public final class EnchantVampirism implements EnchantEffectHandler {

    public static final EnchantVampirism INSTANCE = new EnchantVampirism();

    private static final String ENCHANT_ID = "Enchant_Vampirism";
    private static final String STAT_ID = "HealthRegen";

    private EnchantVampirism() {
    }

    @Override
    public void onKill(NexusEnchantEvent event, int enchantLevel) {
        EnchantmentDefinition def = EnchantmentRegistry.get().getDefinition(ENCHANT_ID);
        if (def == null) return;

        EnchantmentStatDefinition stat = def.getStat(STAT_ID);
        if (stat == null) return;

        PlayerStatsManager psm = Nexus.get().getPlayerStatsManager();
        if (!psm.isReady()) return;

        float maxHealth = psm.getMaxHealth(event.attacker(), event.store());
        // Curve: heal = multiplier * maxHealth
        float healAmount = (float) stat.compute(enchantLevel, maxHealth);

        psm.addHealth(event.attacker(), event.store(), healAmount);
    }
}
