package fr.arnaud.nexus.session;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.component.RunSessionComponent;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public final class PlayerSessionTracker extends RefSystem<EntityStore> {

    @NonNullDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(PlayerRef.getComponentType(), RunSessionComponent.getComponentType());
    }

    @Override
    public void onEntityAdded(@NonNullDecl Ref<EntityStore> ref,
                              @NonNullDecl AddReason reason,
                              @NonNullDecl com.hypixel.hytale.component.Store<EntityStore> store,
                              @NonNullDecl CommandBuffer<EntityStore> cmd) {
        if (reason != AddReason.LOAD) return;

        RunSessionComponent session = store.getComponent(ref, RunSessionComponent.getComponentType());
        if (session == null) return;

        session.resumeSession();
        cmd.run(s -> s.putComponent(ref, RunSessionComponent.getComponentType(), session));
    }

    @Override
    public void onEntityRemove(@NonNullDecl Ref<EntityStore> ref,
                               @NonNullDecl RemoveReason reason,
                               @NonNullDecl Store<EntityStore> store,
                               @NonNullDecl CommandBuffer<EntityStore> cmd) {
        RunSessionComponent session = store.getComponent(ref, RunSessionComponent.getComponentType());
        if (session == null) return;

        session.pauseSession();
    }
}
