package fr.arnaud.nexus.item.weapon.enchantment.impl;

import fr.arnaud.nexus.item.weapon.enchantment.event.EnchantEffectHandler;
import fr.arnaud.nexus.item.weapon.enchantment.event.NexusEnchantEvent;

/**
 * Soul Harvest — grants bonus essence when killing an enemy.
 * <p>
 * The bonus is calculated as:
 * bonusEssence = baseEssenceDrop * multiplierAtLevel
 * So the player receives their normal drop PLUS the bonus on top.
 * <p>
 * TODO: Wire into the essence drop system once it's implemented.
 * The multiplier values per level are:
 *   L1: x0.50, L2: x0.75, L3: x1.00, L4: x1.50, L5: x2.00
 * <p>
 * When implementing, retrieve the base essence drop amount from
 * SpawnerTagComponent (minEssence/maxEssence), compute the bonus,
 * and call PlayerStatsManager.addEssenceDust(attacker, store, bonus).
 */
public final class EnchantSoulHarvest implements EnchantEffectHandler {

    public static final EnchantSoulHarvest INSTANCE = new EnchantSoulHarvest();
    private static final String ENCHANT_ID = "Enchant_SoulHarvest";

    private EnchantSoulHarvest() {
    }

    @Override
    public void onKill(NexusEnchantEvent event, int enchantLevel) {
        // TODO: implement once essence drop system is in place.
        // Steps:
        // 1. Get SpawnerTagComponent from event.target() to read base essence range
        // 2. Roll or average the essence value
        // 3. Get multiplier: EnchantmentRegistry.get()
        //        .getDefinition(ENCHANT_ID).getStat("SoulHarvestMultiplier").getValue(enchantLevel)
        // 4. bonusEssence = baseEssence * multiplier
        // 5. Nexus.get().getPlayerStatsManager()
        //        .addEssenceDust(event.attacker(), event.store(), bonusEssence)
    }
}
