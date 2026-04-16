package fr.arnaud.nexus.item.weapon.enchantment.impl;

import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.feature.ressource.PlayerStatsManager;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentDefinition;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentRegistry;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentStatDefinition;
import fr.arnaud.nexus.item.weapon.enchantment.event.EnchantEffectHandler;
import fr.arnaud.nexus.item.weapon.enchantment.event.NexusEnchantEvent;

public final class EnchantSwiftnessOnKill implements EnchantEffectHandler {

    public static final EnchantSwiftnessOnKill INSTANCE = new EnchantSwiftnessOnKill();
    private static final String ENCHANT_ID = "Enchant_SwiftnessOnKill";

    private EnchantSwiftnessOnKill() {
    }

    @Override
    public void onKill(NexusEnchantEvent event, int enchantLevel) {
        EnchantmentDefinition def = EnchantmentRegistry.get().getDefinition(ENCHANT_ID);
        if (def == null) return;

        EnchantmentStatDefinition speedStat = def.getStat("PredatorSpeedBonus");
        EnchantmentStatDefinition durationStat = def.getStat("PredatorDuration");
        if (speedStat == null || durationStat == null) return;

        float speedBonus = (float) speedStat.getValue(enchantLevel);
        float duration = (float) durationStat.getValue(enchantLevel);

        PlayerStatsManager psm = Nexus.get().getPlayerStatsManager();
        if (!psm.isReady()) return;

        psm.addMovementSpeed(event.attacker(), event.store(), speedBonus);

        // TODO: replace with correct delayed execution API once confirmed.
        // Need: world.executeAfter(Runnable, durationSeconds) or equivalent.
        // For now, schedule via a thread sleep as temporary workaround — REPLACE THIS.
        final var attackerRef = event.attacker();
        final var store = event.store();
        Thread.ofVirtual().start(() -> {
            try {
                Thread.sleep((long) (duration * 1000));
            } catch (InterruptedException ignored) {
            }
            store.getExternalData().getWorld().execute(() -> {
                if (attackerRef.isValid()) {
                    psm.addMovementSpeed(attackerRef, attackerRef.getStore(), -speedBonus);
                }
            });
        });
    }
}
