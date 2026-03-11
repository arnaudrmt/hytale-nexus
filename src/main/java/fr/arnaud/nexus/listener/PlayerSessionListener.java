package fr.arnaud.nexus.listener;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.camera.CameraComponent;
import fr.arnaud.nexus.component.CursorTargetComponent;
import fr.arnaud.nexus.component.DashComponent;
import fr.arnaud.nexus.component.PlayerBodyComponent;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

/**
 * Handles player session lifecycle events.
 * <p>
 * Bootstraps ephemeral runtime components on join. Persistent components
 * ({@link fr.arnaud.nexus.component.RunSessionComponent},
 * {@link EquippedWeaponsComponent}, and all EntityStatMap stats) are restored
 * automatically by the engine from the world's save data. The idempotency
 * guard on {@link CameraComponent} prevents double-bootstrapping on reconnect.
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
            if (store.getComponent(ref, CameraComponent.getComponentType()) != null) return;

            bootstrapComponents(ref, store);
        });
    }

    private static void bootstrapComponents(@NonNullDecl Ref<EntityStore> ref,
                                            @NonNullDecl Store<EntityStore> store) {
        store.putComponent(ref, CameraComponent.getComponentType(), new CameraComponent());
        store.putComponent(ref, PlayerBodyComponent.getComponentType(), new PlayerBodyComponent());
        store.putComponent(ref, DashComponent.getComponentType(), new DashComponent());
        store.putComponent(ref, CursorTargetComponent.getComponentType(), new CursorTargetComponent());
    }
}
