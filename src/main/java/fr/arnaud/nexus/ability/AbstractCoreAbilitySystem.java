package fr.arnaud.nexus.ability;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

/**
 * Base class for all Core ability systems.
 *
 * <p>Subclasses declare which {@link CoreAbility} they own via {@link #getAbility()}.
 * The base tick early-exits if the player does not have that Core equipped,
 * so subclasses only run when they are the active Core.
 *
 * <p>Subclasses must call {@code super.getQuery()} and AND it with their own
 * component requirements, or override {@link #buildQuery()} instead.
 */
public abstract class AbstractCoreAbilitySystem extends EntityTickingSystem<EntityStore> {

    /**
     * The Core ability this system drives.
     */
    @NonNullDecl
    public abstract CoreAbility getAbility();

    /**
     * Called once per tick for each player who has this Core equipped.
     * Guaranteed: {@link ActiveCoreComponent} is present and matches {@link #getAbility()}.
     */
    public abstract void tickCore(float deltaSeconds, int index,
                                  ArchetypeChunk<EntityStore> chunk,
                                  Store<EntityStore> store,
                                  CommandBuffer<EntityStore> cmd,
                                  Ref<EntityStore> ref,
                                  ActiveCoreComponent activeCore);

    /**
     * Subclasses that need additional query constraints should override this
     * and AND their requirements onto the result.
     */
    @NonNullDecl
    protected Query<EntityStore> buildQuery() {
        return Query.and(
            PlayerRef.getComponentType(),
            ActiveCoreComponent.getComponentType()
        );
    }

    @NonNullDecl
    @Override
    public final Query<EntityStore> getQuery() {
        return buildQuery();
    }

    @Override
    public final void tick(float deltaSeconds, int index, ArchetypeChunk<EntityStore> chunk,
                           @NonNullDecl Store<EntityStore> store,
                           @NonNullDecl CommandBuffer<EntityStore> cmd) {
        ActiveCoreComponent activeCore =
            chunk.getComponent(index, ActiveCoreComponent.getComponentType());

        if (activeCore == null || !activeCore.hasEquipped(getAbility())) return;

        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        tickCore(deltaSeconds, index, chunk, store, cmd, ref, activeCore);
    }
}
