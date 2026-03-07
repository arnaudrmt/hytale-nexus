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
import fr.arnaud.nexus.Nexus;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.logging.Level;

/**
 * System that drives per-player camera state and synchronizes it with the client.
 * <p>
 * This system polls the {@link CameraComponent} each tick. It handles
 * interpolation for state transitions and dispatches {@link SetServerCamera}
 * packets to the player's packet handler.
 */
public final class CameraSystem extends EntityTickingSystem<EntityStore> {

    @NonNullDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(CameraComponent.getComponentType(), PlayerRef.getComponentType());
    }

    @Override
    public void tick(float deltaSeconds, int index, ArchetypeChunk<EntityStore> chunk,
                     Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {

        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        CameraComponent cam = chunk.getComponent(index, CameraComponent.getComponentType());
        PlayerRef pr = chunk.getComponent(index, PlayerRef.getComponentType());

        if (cam == null || pr == null) return;

        switch (cam.getMode()) {
            case ISO_RUN -> handleIsoMode(pr, cam, ref, commandBuffer);
            case GLIMPSE_TRANSITION -> handleEntryTransition(deltaSeconds, pr, cam, ref, commandBuffer);
            case GLIMPSE_ACTIVE -> handleGlimpseActive(pr, cam, ref, commandBuffer);
            case GLIMPSE_EXIT_TRANSITION -> handleExitTransition(deltaSeconds, pr, cam, ref, commandBuffer);
        }
    }

    // --- Private Tick Handlers ---

    private void handleIsoMode(PlayerRef pr, CameraComponent cam, Ref<EntityStore> ref, CommandBuffer<EntityStore> cmd) {
        if (cam.isPacketDirty()) {
            sendPacket(pr, CameraPacketBuilder.buildIso(cam));
            cam.clearPacketDirty();
            persistCam(cmd, ref, cam);
        }
    }

    private void handleEntryTransition(float delta, PlayerRef pr, CameraComponent cam, Ref<EntityStore> ref, CommandBuffer<EntityStore> cmd) {
        float elapsed = cam.advanceEntry(delta);
        if (elapsed >= CameraComponent.ENTRY_TRANSITION_DURATION_SEC) {
            cam.completeGlimpseEntry();
            sendPacket(pr, CameraPacketBuilder.buildGlimpseActive());
        } else {
            sendPacket(pr, CameraPacketBuilder.buildEntryTransition(cam.getEntryProgress(), cam.getEffectiveIsoDistance()));
        }
        cam.clearPacketDirty();
        persistCam(cmd, ref, cam);
    }

    private void handleGlimpseActive(PlayerRef pr, CameraComponent cam, Ref<EntityStore> ref, CommandBuffer<EntityStore> cmd) {
        if (cam.isPacketDirty()) {
            sendPacket(pr, CameraPacketBuilder.buildGlimpseActive());
            cam.clearPacketDirty();
            persistCam(cmd, ref, cam);
        }
    }

    private void handleExitTransition(float delta, PlayerRef pr, CameraComponent cam, Ref<EntityStore> ref, CommandBuffer<EntityStore> cmd) {
        float elapsed = cam.advanceExit(delta);
        if (elapsed >= CameraComponent.EXIT_TRANSITION_DURATION_SEC) {
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
        CameraComponent cam = store.getComponent(ref, CameraComponent.getComponentType());
        if (cam != null && cam.beginGlimpseEntry()) {
            cmd.run(s -> s.putComponent(ref, CameraComponent.getComponentType(), cam));
            return true;
        }
        return false;
    }

    public static boolean requestGlimpseExit(Ref<EntityStore> ref, Store<EntityStore> store, CommandBuffer<EntityStore> cmd) {
        CameraComponent cam = store.getComponent(ref, CameraComponent.getComponentType());
        if (cam != null && cam.beginGlimpseExit()) {
            cmd.run(s -> s.putComponent(ref, CameraComponent.getComponentType(), cam));
            return true;
        }
        return false;
    }

    public static void forceIso(Ref<EntityStore> ref, Store<EntityStore> store, CommandBuffer<EntityStore> cmd) {
        CameraComponent cam = store.getComponent(ref, CameraComponent.getComponentType());
        PlayerRef pr = store.getComponent(ref, PlayerRef.getComponentType());
        if (cam != null && pr != null) {
            cam.completeGlimpseExit();
            sendPacket(pr, CameraPacketBuilder.buildIso(cam));
            cam.clearPacketDirty();
            cmd.run(s -> s.putComponent(ref, CameraComponent.getComponentType(), cam));
        }
    }

    public static void updateEncounterZoom(Ref<EntityStore> ref, Store<EntityStore> store, CommandBuffer<EntityStore> cmd, boolean active) {
        CameraComponent cam = store.getComponent(ref, CameraComponent.getComponentType());
        if (cam != null) {
            cam.setEncounterZoomOut(active);
            if (cam.isPacketDirty()) cmd.run(s -> s.putComponent(ref, CameraComponent.getComponentType(), cam));
        }
    }

    public static void updateSpeedFov(Ref<EntityStore> ref, Store<EntityStore> store, CommandBuffer<EntityStore> cmd, float normalised) {
        CameraComponent cam = store.getComponent(ref, CameraComponent.getComponentType());
        if (cam != null) {
            cam.setSpeedFovBonus(normalised);
            if (cam.isPacketDirty()) cmd.run(s -> s.putComponent(ref, CameraComponent.getComponentType(), cam));
        }
    }

    // --- Internal Helpers ---

    private static void persistCam(CommandBuffer<EntityStore> cmd, Ref<EntityStore> ref, CameraComponent cam) {
        cmd.run(s -> s.putComponent(ref, CameraComponent.getComponentType(), cam));
    }

    private static void sendPacket(PlayerRef pr, SetServerCamera packet) {
        try {
            pr.getPacketHandler().writeNoCache(packet);
        } catch (Exception e) {
            Nexus.get().getLogger().at(Level.WARNING).log("CameraSystem: packet send failed — " + e.getMessage());
        }
    }
}
