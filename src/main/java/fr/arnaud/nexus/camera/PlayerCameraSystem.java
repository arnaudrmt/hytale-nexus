package fr.arnaud.nexus.camera;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.protocol.packets.camera.SetServerCamera;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.feature.combat.PlayerBodyStateComponent;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

/**
 * System that drives per-player camera state and synchronizes it with the client.
 * <p>
 * This system polls the {@link PlayerCameraComponent} each tick. It handles
 * interpolation for state transitions and dispatches {@link SetServerCamera}
 * packets to the player's packet handler.
 */
public final class PlayerCameraSystem extends EntityTickingSystem<EntityStore> {

    @NonNullDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(PlayerCameraComponent.getComponentType(), PlayerRef.getComponentType());
    }

    @Override
    public void tick(float deltaSeconds, int index, ArchetypeChunk<EntityStore> chunk,
                     @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> commandBuffer) {

        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        PlayerCameraComponent cam = chunk.getComponent(index, PlayerCameraComponent.getComponentType());
        PlayerRef pr = chunk.getComponent(index, PlayerRef.getComponentType());


        if (cam == null || pr == null) return;

        switch (cam.getMode()) {
            case ISO_RUN -> handleIsoMode(pr, cam, ref, commandBuffer, chunk, index);
            case GLIMPSE_TRANSITION -> handleEntryTransition(deltaSeconds, pr, cam, ref, commandBuffer);
            case GLIMPSE_ACTIVE -> handleGlimpseActive(pr, cam, ref, commandBuffer);
            case GLIMPSE_EXIT_TRANSITION -> handleExitTransition(deltaSeconds, pr, cam, ref, commandBuffer);
        }
    }

    // --- Private Tick Handlers ---

    private void handleIsoMode(PlayerRef pr, PlayerCameraComponent cam, Ref<EntityStore> ref,
                               CommandBuffer<EntityStore> cmd,
                               ArchetypeChunk<EntityStore> chunk, int index) {
        if (cam.isPacketDirty() || isPlayerMoving(chunk, index)) {
            sendPacket(pr, CameraPacketBuilder.buildIso(cam));
            cam.clearPacketDirty();
            persistCam(cmd, ref, cam);
        }
    }

    private void handleEntryTransition(float delta, PlayerRef pr, PlayerCameraComponent cam, Ref<EntityStore> ref, CommandBuffer<EntityStore> cmd) {
        float elapsed = cam.advanceEntry(delta);
        if (elapsed >= PlayerCameraComponent.ENTRY_TRANSITION_DURATION_SEC) {
            cam.completeGlimpseEntry();
            sendPacket(pr, CameraPacketBuilder.buildGlimpseActive());
        } else {
            sendPacket(pr, CameraPacketBuilder.buildEntryTransition(cam.getEntryProgress(), cam.getEffectiveIsoDistance()));
        }
        cam.clearPacketDirty();
        persistCam(cmd, ref, cam);
    }

    private void handleGlimpseActive(PlayerRef pr, PlayerCameraComponent cam, Ref<EntityStore> ref, CommandBuffer<EntityStore> cmd) {
        if (cam.isPacketDirty()) {
            sendPacket(pr, CameraPacketBuilder.buildGlimpseActive());
            cam.clearPacketDirty();
            persistCam(cmd, ref, cam);
        }
    }

    private void handleExitTransition(float delta, PlayerRef pr, PlayerCameraComponent cam, Ref<EntityStore> ref, CommandBuffer<EntityStore> cmd) {
        float elapsed = cam.advanceExit(delta);
        if (elapsed >= PlayerCameraComponent.EXIT_TRANSITION_DURATION_SEC) {
            cam.completeGlimpseExit();
            sendPacket(pr, CameraPacketBuilder.buildIso(cam));
        } else {
            sendPacket(pr, CameraPacketBuilder.buildExitTransition(cam.getExitProgress(), cam.getEffectiveIsoDistance()));
        }
        cam.clearPacketDirty();
        persistCam(cmd, ref, cam);
    }

    // --- Static API for External Systems ---

    public static boolean requestGlimpseEntry(Ref<EntityStore> ref, Store<EntityStore> store, CommandBuffer<EntityStore> cmd) {
        PlayerCameraComponent cam = store.getComponent(ref, PlayerCameraComponent.getComponentType());
        if (cam != null && cam.beginGlimpseEntry()) {
            cmd.run(s -> s.putComponent(ref, PlayerCameraComponent.getComponentType(), cam));
            return true;
        }
        return false;
    }

    public static boolean requestGlimpseExit(Ref<EntityStore> ref, Store<EntityStore> store, CommandBuffer<EntityStore> cmd) {
        PlayerCameraComponent cam = store.getComponent(ref, PlayerCameraComponent.getComponentType());
        if (cam != null && cam.beginGlimpseExit()) {
            cmd.run(s -> s.putComponent(ref, PlayerCameraComponent.getComponentType(), cam));
            return true;
        }
        return false;
    }

    public static void forceIso(Ref<EntityStore> ref, Store<EntityStore> store, CommandBuffer<EntityStore> cmd) {
        PlayerCameraComponent cam = store.getComponent(ref, PlayerCameraComponent.getComponentType());
        PlayerRef pr = store.getComponent(ref, PlayerRef.getComponentType());
        if (cam != null && pr != null) {
            cam.completeGlimpseExit();
            sendPacket(pr, CameraPacketBuilder.buildIso(cam));
            cam.clearPacketDirty();
            cmd.run(s -> s.putComponent(ref, PlayerCameraComponent.getComponentType(), cam));
        }
    }

    public static void updateEncounterZoom(Ref<EntityStore> ref, Store<EntityStore> store, CommandBuffer<EntityStore> cmd, boolean active) {
        PlayerCameraComponent cam = store.getComponent(ref, PlayerCameraComponent.getComponentType());
        if (cam != null) {
            cam.setEncounterZoomOut(active);
            if (cam.isPacketDirty()) cmd.run(s -> s.putComponent(ref, PlayerCameraComponent.getComponentType(), cam));
        }
    }

    public static void updateSpeedFov(Ref<EntityStore> ref, Store<EntityStore> store, CommandBuffer<EntityStore> cmd, float normalised) {
        PlayerCameraComponent cam = store.getComponent(ref, PlayerCameraComponent.getComponentType());
        if (cam != null) {
            cam.setSpeedFovBonus(normalised);
            if (cam.isPacketDirty()) cmd.run(s -> s.putComponent(ref, PlayerCameraComponent.getComponentType(), cam));
        }
    }

    // --- Internal Helpers ---

    private static void persistCam(CommandBuffer<EntityStore> cmd, Ref<EntityStore> ref, PlayerCameraComponent cam) {
        cmd.run(s -> s.putComponent(ref, PlayerCameraComponent.getComponentType(), cam));
    }

    private static boolean isPlayerMoving(ArchetypeChunk<EntityStore> chunk, int index) {
        PlayerBodyStateComponent body = chunk.getComponent(index, PlayerBodyStateComponent.getComponentType());
        return body != null && body.getLocomotionState() != PlayerBodyStateComponent.LocomotionState.IDLE;
    }

    private static void sendPacket(PlayerRef pr, SetServerCamera packet) {
        try {
            pr.getPacketHandler().writeNoCache(packet);
        } catch (Exception e) {
            Nexus.get().getLogger().at(Level.WARNING).log("CameraSystem: packet send failed — " + e.getMessage());
        }
    }
}
