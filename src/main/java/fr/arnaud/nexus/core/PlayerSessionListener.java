package fr.arnaud.nexus.core;

import com.hypixel.hytale.builtin.instances.InstancesPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.camera.PlayerCameraComponent;
import fr.arnaud.nexus.camera.PlayerOcclusionComponent;
import fr.arnaud.nexus.feature.combat.PlayerBodyStateComponent;
import fr.arnaud.nexus.feature.combat.switchstrike.SwitchStrikeComponent;
import fr.arnaud.nexus.feature.movement.PlayerDashComponent;
import fr.arnaud.nexus.input.PlayerCursorTargetComponent;
import fr.arnaud.nexus.input.hover.PlayerHoverStateComponent;
import fr.arnaud.nexus.level.NexusWorldLoadSystem;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class PlayerSessionListener {

    private PlayerSessionListener() {
    }

    public static void onPlayerReady(@NonNullDecl PlayerReadyEvent event) {
        var ref = event.getPlayerRef();
        var store = ref.getStore();
        World currentWorld = store.getExternalData().getWorld();

        // 1. If the player is ALREADY in ANY Nexus world, do nothing.
        if (currentWorld.getName().contains("Nexus")) {
            if (store.getComponent(ref, PlayerCameraComponent.getComponentType()) == null) {
                bootstrapComponents(ref, store);
            }
            return;
        }

        NexusWorldLoadSystem levelSystem = Nexus.get().getNexusWorldLoadSystem();
        CompletableFuture<World> pending = levelSystem.getPendingNexusWorld();

        if (pending == null) return;

        UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
        if (uuidComponent == null) return;
        UUID playerUuid = uuidComponent.getUuid();

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        Transform returnLocation = transform != null
            ? new Transform(transform.getPosition().clone(), transform.getRotation().clone())
            : new Transform(new Vector3d(0.5, 80.0, 0.5), Vector3f.FORWARD);

        // Prep the bootstrap instructions for when they arrive at the targeted world.
        pending.thenAccept(nexusWorld -> {
            nexusWorld.execute(() -> {
                EntityStore entityStore = nexusWorld.getEntityStore();
                Ref<EntityStore> newRef = entityStore.getRefFromUUID(playerUuid);
                if (newRef == null || !newRef.isValid()) return;

                Store<EntityStore> newStore = entityStore.getStore();
                if (newStore.getComponent(newRef, PlayerCameraComponent.getComponentType()) == null) {
                    bootstrapComponents(newRef, newStore);
                }
            });
        });

        // 2. Safely defer the teleport to the world's tick thread.
        // This prevents the "getPositionComponent called async" stack trace from WorldMapTracker.
        currentWorld.execute(() -> {
            InstancesPlugin.teleportPlayerToLoadingInstance(ref, store, pending, returnLocation);
        });
    }

    private static void bootstrapComponents(@NonNullDecl Ref<EntityStore> ref,
                                            @NonNullDecl Store<EntityStore> store) {
        store.putComponent(ref, PlayerCameraComponent.getComponentType(), new PlayerCameraComponent());
        store.putComponent(ref, PlayerBodyStateComponent.getComponentType(), new PlayerBodyStateComponent());
        store.putComponent(ref, PlayerDashComponent.getComponentType(), new PlayerDashComponent());
        store.putComponent(ref, PlayerCursorTargetComponent.getComponentType(), new PlayerCursorTargetComponent());
        store.putComponent(ref, SwitchStrikeComponent.getComponentType(), new SwitchStrikeComponent());
        store.putComponent(ref, PlayerOcclusionComponent.getComponentType(), new PlayerOcclusionComponent());
        store.putComponent(ref, PlayerHoverStateComponent.getComponentType(), new PlayerHoverStateComponent());
    }
}
