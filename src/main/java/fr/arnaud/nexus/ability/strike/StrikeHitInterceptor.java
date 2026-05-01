package fr.arnaud.nexus.ability.strike;

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
 * During the HIT_WINDOW phase, registers any damaged entity as a Strike target.
 */
public final class StrikeHitInterceptor extends DamageEventSystem {

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
        if (!attackerRef.isValid()) return;

        StrikeComponent strike = store.getComponent(attackerRef, StrikeComponent.getComponentType());
        if (strike == null) return;

        Ref<EntityStore> targetRef = chunk.getReferenceTo(index);

        switch (strike.getState()) {
            case HIT_WINDOW -> {
                strike.registerHitTarget(targetRef);
                cmd.run(s -> s.putComponent(attackerRef, StrikeComponent.getComponentType(), strike));
            }
            case COMBO -> {
                boolean tracked = strike.registerComboHit(targetRef, damage.getInitialAmount());
                if (tracked) {
                    damage.setCancelled(true);
                    cmd.run(s -> s.putComponent(attackerRef, StrikeComponent.getComponentType(), strike));
                }
            }
            default -> { /* IDLE — nothing to do */ }
        }
    }
}
