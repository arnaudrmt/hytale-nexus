package fr.arnaud.nexus.feature.breach;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

/**
 * Intercepts damage dealt to a frozen boss during the Breach sequence.
 * Runs in the Filter Damage Group (before health is modified).
 * Cancels the event and folds the raw damage into the attacker's combo
 * accumulator using the formula: hit N contributes N × rawDamage.
 */
public final class BreachDamageInterceptor extends DamageEventSystem {

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
        Ref<EntityStore> targetRef = chunk.getReferenceTo(index);
        if (store.getComponent(targetRef, FrozenTargetComponent.getComponentType()) == null) return;

        if (!(damage.getSource() instanceof Damage.EntitySource entitySource)) return;

        Ref<EntityStore> attackerRef = entitySource.getRef();
        if (attackerRef == null || !attackerRef.isValid()) return;

        BreachSequenceComponent sequence =
            store.getComponent(attackerRef, BreachSequenceComponent.getComponentType());
        if (sequence == null || sequence.getPhase() != BreachSequenceComponent.Phase.ACTIVE) return;

        sequence.registerHit(damage.getInitialAmount());
        damage.setCancelled(true);
        cmd.run(s -> s.putComponent(attackerRef, BreachSequenceComponent.getComponentType(), sequence));
    }
}
