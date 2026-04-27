package fr.arnaud.nexus.item.weapon.enchantment.impl;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.feature.ressource.PlayerStatsManager;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentDefinition;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentRegistry;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentStatDefinition;
import fr.arnaud.nexus.item.weapon.enchantment.event.EnchantEffectHandler;
import fr.arnaud.nexus.item.weapon.enchantment.event.NexusEnchantEvent;

import java.util.Map;
import java.util.concurrent.*;

public final class EnchantSwiftnessOnKill implements EnchantEffectHandler {

    public static final EnchantSwiftnessOnKill INSTANCE = new EnchantSwiftnessOnKill();
    private static final String ENCHANT_ID = "Enchant_SwiftnessOnKill";

    private final Map<Ref<EntityStore>, ScheduledFuture<?>> pendingRemovals = new ConcurrentHashMap<>();
    private final Map<Ref<EntityStore>, Float> activeBonuses = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();


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

        Ref<EntityStore> attackerRef = event.attacker();
        Store<EntityStore> store = event.store();

        ScheduledFuture<?> existing = pendingRemovals.remove(attackerRef);
        if (existing != null) {
            existing.cancel(false);
        } else {
            activeBonuses.put(attackerRef, 0f);
        }

        float currentBonus = activeBonuses.getOrDefault(attackerRef, 0f);

        if (currentBonus > 0f) {
            psm.addMovementSpeed(attackerRef, store, -currentBonus);
        }
        psm.addMovementSpeed(attackerRef, store, speedBonus);
        activeBonuses.put(attackerRef, speedBonus);

        ScheduledFuture<?> future = SCHEDULER.schedule(() ->
                store.getExternalData().getWorld().execute(() -> {
                    if (!attackerRef.isValid()) return;
                    float bonus = activeBonuses.remove(attackerRef);
                    pendingRemovals.remove(attackerRef);
                    psm.addMovementSpeed(attackerRef, store, -bonus);
                }),
            (long) (duration * 1000), TimeUnit.MILLISECONDS
        );

        pendingRemovals.put(attackerRef, future);
    }
}
