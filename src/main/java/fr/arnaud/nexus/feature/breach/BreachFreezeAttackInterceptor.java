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
 * Cancels any outgoing damage from a frozen entity.
 * <p>
 * The boss AI may complete an attack animation and fire a damage event even
 * while {@link BreachFreezeSystem} is zeroing its velocity. This interceptor
 * runs in the Filter Damage Group (before health is modified) and cancels
 * those events when the <em>attacker</em> carries {@link FrozenTargetComponent}.
 * <p>
 * Note: the chunk entity here is the <em>target</em> (standard DamageEventSystem
 * convention). We resolve the attacker from the source and check it for
 * {@link FrozenTargetComponent}, not the target.
 */
public final class BreachFreezeAttackInterceptor extends DamageEventSystem {

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

        FrozenTargetComponent frozen = store.getComponent(attackerRef, FrozenTargetComponent.getComponentType());
        if (frozen == null) return;

        damage.setCancelled(true);
    }
}
