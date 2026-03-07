package fr.arnaud.nexus.listener;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.camera.CameraComponent;
import fr.arnaud.nexus.component.FlowComponent;
import fr.arnaud.nexus.component.LucidityComponent;
import fr.arnaud.nexus.component.PlayerBodyComponent;
import fr.arnaud.nexus.component.RunSessionComponent;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

/**
 * Bootstraps all required Nexus ECS components onto a player entity
 * when they enter the world.
 */
public final class PlayerSessionListener {

    private PlayerSessionListener() {
    }

    public static void onPlayerReady(@NonNullDecl PlayerReadyEvent event) {
        Player player = event.getPlayer();
        if (player.getWorld() == null) return;

        player.getWorld().execute(() -> {
            Ref<EntityStore> ref = player.getReference();
            if (ref == null || !ref.isValid()) return;

            Store<EntityStore> store = player.getWorld().getEntityStore().getStore();

            // Guard against duplicate initialization (e.g., reconnects or world switches)
            if (store.getComponent(ref, FlowComponent.getComponentType()) != null) return;

            bootstrapComponents(ref, store);
        });
    }

    private static void bootstrapComponents(Ref<EntityStore> ref, Store<EntityStore> store) {
        store.putComponent(ref, FlowComponent.getComponentType(), new FlowComponent());
        store.putComponent(ref, LucidityComponent.getComponentType(), new LucidityComponent());
        store.putComponent(ref, RunSessionComponent.getComponentType(), new RunSessionComponent());
        store.putComponent(ref, CameraComponent.getComponentType(), new CameraComponent());
        store.putComponent(ref, PlayerBodyComponent.getComponentType(), new PlayerBodyComponent());
    }
}
