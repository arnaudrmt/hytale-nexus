package fr.arnaud.nexus.item.weapon.enchantment.impl;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentDefinition;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentRegistry;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentStatDefinition;
import fr.arnaud.nexus.item.weapon.enchantment.event.EnchantEffectHandler;
import fr.arnaud.nexus.item.weapon.enchantment.event.NexusEnchantEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class EnchantBloodlust implements EnchantEffectHandler {

    public static final EnchantBloodlust INSTANCE = new EnchantBloodlust();
    private static final String ENCHANT_ID = "Enchant_Bloodlust";

    private static final Map<Integer, BloodlustState> stateMap = new ConcurrentHashMap<>();

    private EnchantBloodlust() {
    }

    public static double getBonusMultiplier(Ref<EntityStore> attackerRef, int enchantLevel) {
        BloodlustState state = stateMap.get(attackerRef.getIndex());
        if (state == null || state.stacks <= 0) return 0.0;

        EnchantmentDefinition def = EnchantmentRegistry.get().getDefinition(ENCHANT_ID);
        if (def == null) return 0.0;

        EnchantmentStatDefinition bonusStat = def.getStat("BloodlustBonusPerStack");
        if (bonusStat == null) return 0.0;

        return bonusStat.getValue(enchantLevel) * state.stacks;
    }

    @Override
    public void onKill(NexusEnchantEvent event, int enchantLevel) {
        EnchantmentDefinition def = EnchantmentRegistry.get().getDefinition(ENCHANT_ID);
        if (def == null) return;

        EnchantmentStatDefinition maxStacksStat = def.getStat("BloodlustMaxStacks");
        EnchantmentStatDefinition resetTimeStat = def.getStat("BloodlustResetTime");
        if (maxStacksStat == null || resetTimeStat == null) return;

        int maxStacks = (int) maxStacksStat.getValue(enchantLevel);
        long resetMs = (long) (resetTimeStat.getValue(enchantLevel) * 1000.0);

        int refIndex = event.attacker().getIndex();
        BloodlustState state = stateMap.computeIfAbsent(refIndex, k -> new BloodlustState());

        // Cancel the OLD reset thread BEFORE creating the new one
        if (state.resetThread != null) {
            state.resetThread.interrupt();
            state.resetThread = null;
        }

        state.stacks = Math.min(state.stacks + 1, maxStacks);

        // Capture for lambda
        final long capturedResetMs = resetMs;
        final Ref<EntityStore> attacker = event.attacker();
        final World world = event.store().getExternalData().getWorld();

        // Start new reset thread AFTER incrementing stacks
        Thread resetThread = Thread.ofVirtual().start(() -> {
            try {
                Thread.sleep(capturedResetMs);
            } catch (InterruptedException ignored) {
                // A new kill cancelled this reset — do nothing, stacks are managed by new thread
                return;
            }
            world.execute(() -> {
                if (!attacker.isValid()) {
                    stateMap.remove(refIndex);
                    return;
                }
                BloodlustState s = stateMap.get(refIndex);
                if (s != null) s.stacks = 0;
            });
        });

        state.resetThread = resetThread;
    }

    private static final class BloodlustState {
        volatile int stacks = 0;
        volatile Thread resetThread = null;
    }
}
