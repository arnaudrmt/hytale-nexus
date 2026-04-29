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

public abstract class AbstractCoreAbilitySystem extends EntityTickingSystem<EntityStore> {

    @NonNullDecl
    public abstract CoreAbility getAbility();

    public abstract void tickCore(float deltaSeconds, int index,
                                  ArchetypeChunk<EntityStore> chunk,
                                  Store<EntityStore> store,
                                  CommandBuffer<EntityStore> cmd,
                                  Ref<EntityStore> ref,
                                  ActiveCoreComponent activeCore);

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
