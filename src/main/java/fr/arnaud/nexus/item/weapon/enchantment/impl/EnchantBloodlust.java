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
    public static final String ENCHANT_ID = "Enchant_Bloodlust";

    public static final String STAT_BONUS_PER_STACK = "BloodlustBonusPerStack";
    public static final String STAT_MAX_STACKS = "BloodlustMaxStacks";
    public static final String STAT_RESET_TIME = "BloodlustResetTime";

    private static final Map<Integer, BloodlustState> stateMap = new ConcurrentHashMap<>();

    private EnchantBloodlust() {
    }

    public static double getBonusMultiplier(Ref<EntityStore> attackerRef, int enchantLevel) {
        BloodlustState state = stateMap.get(attackerRef.getIndex());
        if (state == null || state.stacks <= 0) return 0.0;

        EnchantmentDefinition def = EnchantmentRegistry.getInstance().getDefinition(ENCHANT_ID);
        if (def == null) return 0.0;

        EnchantmentStatDefinition bonusStat = def.getEnchantmentStatById(STAT_BONUS_PER_STACK);
        if (bonusStat == null) return 0.0;

        return bonusStat.getStatValueForLevel(enchantLevel) * state.stacks;
    }

    @Override
    public void onKill(NexusEnchantEvent event, int enchantLevel) {
        EnchantmentDefinition def = EnchantmentRegistry.getInstance().getDefinition(ENCHANT_ID);
        if (def == null) return;

        EnchantmentStatDefinition maxStacksStat = def.getEnchantmentStatById(STAT_MAX_STACKS);
        EnchantmentStatDefinition resetTimeStat = def.getEnchantmentStatById(STAT_RESET_TIME);
        if (maxStacksStat == null || resetTimeStat == null) return;

        int maxStacks = (int) maxStacksStat.getStatValueForLevel(enchantLevel);
        long resetMs = (long) (resetTimeStat.getStatValueForLevel(enchantLevel) * 1000.0);

        int refIndex = event.attacker().getIndex();
        BloodlustState state = stateMap.computeIfAbsent(refIndex, _ -> new BloodlustState());

        if (state.resetThread != null) {
            state.resetThread.interrupt();
            state.resetThread = null;
        }

        state.stacks = Math.min(state.stacks + 1, maxStacks);

        final long capturedResetMs = resetMs;
        final Ref<EntityStore> attacker = event.attacker();
        final World world = event.store().getExternalData().getWorld();

        state.resetThread = Thread.ofVirtual().start(() -> {
            try {
                Thread.sleep(capturedResetMs);
            } catch (InterruptedException ignored) {
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
    }

    private static final class BloodlustState {
        volatile int stacks = 0;
        volatile Thread resetThread = null;
    }
}
