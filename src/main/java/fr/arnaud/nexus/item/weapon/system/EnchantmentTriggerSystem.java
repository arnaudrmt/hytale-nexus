package fr.arnaud.nexus.item.weapon.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap.Predictable;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.item.weapon.component.WeaponInstanceComponent;
import fr.arnaud.nexus.item.weapon.component.WeaponInstanceComponent.ActiveEnchantmentState;
import fr.arnaud.nexus.item.weapon.enchantment.TriggerStatRegistry;
import fr.arnaud.nexus.item.weapon.enchantment.handlers.*;

public final class EnchantmentTriggerSystem extends EntityTickingSystem<EntityStore> {

    private final CleaveEnchantmentHandler cleave;
    private final ShockwaveEnchantmentHandler shockwave;
    private final AspirationEnchantmentHandler aspiration;
    private final ChainProjectileEnchantmentHandler chainProjectile;
    private final PiercingEnchantmentHandler piercing;
    private final BouncingProjectileEnchantmentHandler bouncing;

    public EnchantmentTriggerSystem(
        CleaveEnchantmentHandler cleave,
        ShockwaveEnchantmentHandler shockwave,
        AspirationEnchantmentHandler aspiration,
        ChainProjectileEnchantmentHandler chainProjectile,
        PiercingEnchantmentHandler piercing,
        BouncingProjectileEnchantmentHandler bouncing
    ) {
        this.cleave = cleave;
        this.shockwave = shockwave;
        this.aspiration = aspiration;
        this.chainProjectile = chainProjectile;
        this.piercing = piercing;
        this.bouncing = bouncing;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
            PlayerRef.getComponentType(),
            WeaponInstanceComponent.getComponentType(),
            EntityStatMap.getComponentType()
        );
    }

    @Override
    public void tick(
        float dt,
        int index,
        ArchetypeChunk<EntityStore> chunk,
        Store<EntityStore> store,
        CommandBuffer<EntityStore> cmd
    ) {
        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        EntityStatMap stats = chunk.getComponent(index, EntityStatMap.getComponentType());
        WeaponInstanceComponent instance = chunk.getComponent(index, WeaponInstanceComponent.getComponentType());

        TriggerStatRegistry triggers = TriggerStatRegistry.get();
        if (!triggers.isReady()) return;

        dispatchIfTriggered(ref, stats, instance, triggers.getTriggerCleaveIndex(), cmd,
            enchantId -> isCleaveActive(instance, enchantId),
            enchantId -> cleave.execute(ref, levelOf(instance, enchantId), cmd));

        dispatchIfTriggered(ref, stats, instance, triggers.getTriggerShockwaveIndex(), cmd,
            enchantId -> isEnchantActive(instance, enchantId),
            enchantId -> shockwave.execute(ref, levelOf(instance, enchantId), cmd));

        dispatchIfTriggered(ref, stats, instance, triggers.getTriggerAspirationIndex(), cmd,
            enchantId -> isEnchantActive(instance, enchantId),
            enchantId -> aspiration.execute(ref, levelOf(instance, enchantId), cmd));
    }

    private void dispatchIfTriggered(
        Ref<EntityStore> ref,
        EntityStatMap stats,
        WeaponInstanceComponent instance,
        int triggerIndex,
        CommandBuffer<EntityStore> cmd,
        java.util.function.Predicate<String> activeCheck,
        java.util.function.Consumer<String> executor
    ) {
        if (triggerIndex == Integer.MIN_VALUE) return;
        EntityStatValue trigger = stats.get(triggerIndex);
        if (trigger == null || trigger.get() <= 0f) return;

        String enchantId = findEnchantForTrigger(instance, triggerIndex);
        if (enchantId == null || !activeCheck.test(enchantId)) return;

        executor.accept(enchantId);

        stats.subtractStatValue(Predictable.SELF, triggerIndex, trigger.get());
        cmd.putComponent(ref, EntityStatMap.getComponentType(), stats);
    }

    private String findEnchantForTrigger(WeaponInstanceComponent instance, int triggerIndex) {
        TriggerStatRegistry triggers = TriggerStatRegistry.get();
        for (ActiveEnchantmentState state : instance.activeStates) {
            int expectedIndex = resolveExpectedTriggerIndex(state.enchantmentId(), triggers);
            if (expectedIndex == triggerIndex) return state.enchantmentId();
        }
        return null;
    }

    private int resolveExpectedTriggerIndex(String enchantmentId, TriggerStatRegistry triggers) {
        if (enchantmentId.contains("Cleave")) return triggers.getTriggerCleaveIndex();
        if (enchantmentId.contains("Shockwave")) return triggers.getTriggerShockwaveIndex();
        if (enchantmentId.contains("Aspiration")) return triggers.getTriggerAspirationIndex();
        if (enchantmentId.contains("ChainShot")) return triggers.getTriggerChainProjectileIndex();
        if (enchantmentId.contains("Piercing")) return triggers.getTriggerPiercingIndex();
        if (enchantmentId.contains("Bouncing")) return triggers.getTriggerBouncingIndex();
        return Integer.MIN_VALUE;
    }

    private boolean isEnchantActive(WeaponInstanceComponent instance, String enchantmentId) {
        for (ActiveEnchantmentState state : instance.activeStates) {
            if (state.enchantmentId().equals(enchantmentId)) return state.flowGateActive();
        }
        return false;
    }

    private boolean isCleaveActive(WeaponInstanceComponent instance, String enchantmentId) {
        return isEnchantActive(instance, enchantmentId);
    }

    private int levelOf(WeaponInstanceComponent instance, String enchantmentId) {
        for (ActiveEnchantmentState state : instance.activeStates) {
            if (state.enchantmentId().equals(enchantmentId)) return state.level();
        }
        return 1;
    }
}
