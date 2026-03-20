package fr.arnaud.nexus.breach;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.Nexus;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Intercepts damage dealt to the frozen boss during an active Breach sequence.
 * <p>
 * Since {@link DamageEventSystem} provides no cancel mechanism, the hit is
 * negated immediately by healing the boss back by the same amount. The raw
 * damage value is then recorded in the attacker's {@link BreachComboComponent}
 * with the combo multiplier applied.
 * <p>
 * Only active when the attacker has an active {@link BreachComboComponent}
 * and the target is the boss stored in their {@link BreachSequenceComponent}.
 */
public final class BreachDamageInterceptor extends DamageEventSystem {

    private static final Logger LOGGER = Logger.getLogger(BreachDamageInterceptor.class.getName());

    /**
     * The Asset Editor stat ID used for entity health.
     * Update this if your project uses a different stat name.
     */
    private static final String HEALTH_STAT_ID = "Health";

    @NonNullDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void handle(int index, @NonNullDecl ArchetypeChunk<EntityStore> chunk,
                       @NonNullDecl Store<EntityStore> store,
                       @NonNullDecl CommandBuffer<EntityStore> cmd,
                       @NonNullDecl Damage damage) {
        if (!(damage.getSource() instanceof Damage.EntitySource entitySource)) return;

        Ref<EntityStore> attackerRef = entitySource.getRef();
        if (attackerRef == null || !attackerRef.isValid()) return;

        BreachComboComponent combo = store.getComponent(attackerRef, BreachComboComponent.getComponentType());
        if (combo == null || !combo.isActive()) return;

        BreachSequenceComponent breach = store.getComponent(attackerRef, BreachSequenceComponent.getComponentType());
        if (breach == null || breach.getState() != BreachSequenceComponent.State.ACTIVE) return;

        Ref<EntityStore> targetRef = chunk.getReferenceTo(index);
        Ref<EntityStore> bossRef = breach.getBossRef();
        if (bossRef == null || !bossRef.equals(targetRef)) return;

        float rawDamage = damage.getAmount();
        if (rawDamage <= 0f) return;

        healEntity(targetRef, store, cmd, rawDamage);

        float multiplied = combo.recordHit(rawDamage);
        cmd.run(s -> s.putComponent(attackerRef, BreachComboComponent.getComponentType(), combo));

        LOGGER.log(Level.INFO, "[Breach] Hit " + combo.getHitCount()
            + " intercepted — raw: " + rawDamage
            + ", multiplied: " + multiplied
            + ", total accumulated: " + combo.getAccumulatedDamage());
    }

    /**
     * Heals the boss by {@code amount} to negate the intercepted hit.
     * Uses EntityStatMap health stat directly since there is no heal() API.
     */
    private static void healEntity(@NonNullDecl Ref<EntityStore> ref,
                                   @NonNullDecl Store<EntityStore> store,
                                   @NonNullDecl CommandBuffer<EntityStore> cmd,
                                   float amount) {
        EntityStatMap stats = store.getComponent(ref, EntityStatMap.getComponentType());
        if (stats == null) return;

        int healthIndex = EntityStatType.getAssetMap().getIndex(HEALTH_STAT_ID);
        if (healthIndex == Integer.MIN_VALUE) {
            Nexus.get().getLogger().at(Level.WARNING).log("[Breach] Health stat index not found — cannot negate boss damage.");
            return;
        }

        stats.addStatValue(EntityStatMap.Predictable.ALL, healthIndex, amount);
        cmd.run(s -> s.putComponent(ref, EntityStatMap.getComponentType(), stats));
    }
}
