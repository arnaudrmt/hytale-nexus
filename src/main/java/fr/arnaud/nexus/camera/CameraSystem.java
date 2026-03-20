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
import fr.arnaud.nexus.component.PlayerBodyComponent;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

/**
 * Drives per-player camera state and synchronizes it with the client each tick.
 */
public final class CameraSystem extends EntityTickingSystem<EntityStore> {

    @NonNullDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(CameraComponent.getComponentType(), PlayerRef.getComponentType());
    }

    @Override
    public void tick(float deltaSeconds, int index, ArchetypeChunk<EntityStore> chunk,
                     @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> commandBuffer) {
        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        CameraComponent cam = chunk.getComponent(index, CameraComponent.getComponentType());
        PlayerRef pr = chunk.getComponent(index, PlayerRef.getComponentType());

        if (cam == null || pr == null) return;

        switch (cam.getMode()) {
            case ISO_RUN -> handleIsoMode(pr, cam, ref, commandBuffer, chunk, index);
            case GLIMPSE_TRANSITION -> handleEntryTransition(deltaSeconds, pr, cam, ref, commandBuffer);
            case GLIMPSE_ACTIVE -> handleGlimpseActive(pr, cam, ref, commandBuffer);
            case GLIMPSE_EXIT_TRANSITION -> handleExitTransition(deltaSeconds, pr, cam, ref, commandBuffer);
        }
    }

    private void handleIsoMode(PlayerRef pr, CameraComponent cam, Ref<EntityStore> ref,
                               CommandBuffer<EntityStore> cmd,
                               ArchetypeChunk<EntityStore> chunk, int index) {
        if (cam.isPacketDirty() || isPlayerMoving(chunk, index)) {
            sendPacket(pr, CameraPacketBuilder.buildIso(cam));
            cam.clearPacketDirty();
            persistCam(cmd, ref, cam);
        }
    }

    private void handleEntryTransition(float delta, PlayerRef pr, CameraComponent cam,
                                       Ref<EntityStore> ref, CommandBuffer<EntityStore> cmd) {
        float elapsed = cam.advanceEntry(delta);
        if (elapsed >= CameraComponent.ENTRY_TRANSITION_DURATION_SEC) {
            cam.completeGlimpseEntry();
            sendPacket(pr, CameraPacketBuilder.buildGlimpseActive());
        } else {
            sendPacket(pr, CameraPacketBuilder.buildEntryTransition(
                cam.getEntryProgress(), cam.getEffectiveIsoDistance()));
        }
        cam.clearPacketDirty();
        persistCam(cmd, ref, cam);
    }

    private void handleGlimpseActive(PlayerRef pr, CameraComponent cam,
                                     Ref<EntityStore> ref, CommandBuffer<EntityStore> cmd) {
        if (cam.isPacketDirty()) {
            sendPacket(pr, CameraPacketBuilder.buildGlimpseActive());
            cam.clearPacketDirty();
            persistCam(cmd, ref, cam);
        }
    }

    private void handleExitTransition(float delta, PlayerRef pr, CameraComponent cam,
                                      Ref<EntityStore> ref, CommandBuffer<EntityStore> cmd) {
        float elapsed = cam.advanceExit(delta);
        if (elapsed >= CameraComponent.EXIT_TRANSITION_DURATION_SEC) {
            cam.completeGlimpseExit();
            sendPacket(pr, CameraPacketBuilder.buildIso(cam));
        } else {
            sendPacket(pr, CameraPacketBuilder.buildExitTransition(
                cam.getExitProgress(), cam.getEffectiveIsoDistance()));
        }
        cam.clearPacketDirty();
        persistCam(cmd, ref, cam);
    }

    // --- Static API ---

    /**
     * Begins the ISO → Glimpse entry transition and sends the t=0 packet immediately
     * so the client starts interpolating on the same frame, not the next tick.
     */
    public static boolean requestGlimpseEntry(Ref<EntityStore> ref, Store<EntityStore> store,
                                              CommandBuffer<EntityStore> cmd) {
        CameraComponent cam = store.getComponent(ref, CameraComponent.getComponentType());
        PlayerRef pr = store.getComponent(ref, PlayerRef.getComponentType());
        if (cam == null || pr == null || !cam.beginGlimpseEntry()) return false;

        sendPacket(pr, CameraPacketBuilder.buildEntryTransition(0f, cam.getEffectiveIsoDistance()));
        cam.clearPacketDirty();
        cmd.run(s -> s.putComponent(ref, CameraComponent.getComponentType(), cam));
        return true;
    }

    /**
     * Begins the Glimpse → ISO exit transition and sends the t=0 packet immediately.
     */
    public static boolean requestGlimpseExit(Ref<EntityStore> ref, Store<EntityStore> store,
                                             CommandBuffer<EntityStore> cmd) {
        CameraComponent cam = store.getComponent(ref, CameraComponent.getComponentType());
        PlayerRef pr = store.getComponent(ref, PlayerRef.getComponentType());
        if (cam == null || pr == null || !cam.beginGlimpseExit()) return false;

        sendPacket(pr, CameraPacketBuilder.buildExitTransition(0f, cam.getEffectiveIsoDistance()));
        cam.clearPacketDirty();
        cmd.run(s -> s.putComponent(ref, CameraComponent.getComponentType(), cam));
        return true;
    }

    public static void forceIso(Ref<EntityStore> ref, Store<EntityStore> store,
                                CommandBuffer<EntityStore> cmd) {
        CameraComponent cam = store.getComponent(ref, CameraComponent.getComponentType());
        PlayerRef pr = store.getComponent(ref, PlayerRef.getComponentType());
        if (cam != null && pr != null) {
            cam.completeGlimpseExit();
            sendPacket(pr, CameraPacketBuilder.buildIso(cam));
            cam.clearPacketDirty();
            cmd.run(s -> s.putComponent(ref, CameraComponent.getComponentType(), cam));
        }
    }

    public static void updateEncounterZoom(Ref<EntityStore> ref, Store<EntityStore> store,
                                           CommandBuffer<EntityStore> cmd, boolean active) {
        CameraComponent cam = store.getComponent(ref, CameraComponent.getComponentType());
        if (cam != null) {
            cam.setEncounterZoomOut(active);
            if (cam.isPacketDirty()) cmd.run(s -> s.putComponent(ref, CameraComponent.getComponentType(), cam));
        }
    }

    public static void updateSpeedFov(Ref<EntityStore> ref, Store<EntityStore> store,
                                      CommandBuffer<EntityStore> cmd, float normalised) {
        CameraComponent cam = store.getComponent(ref, CameraComponent.getComponentType());
        if (cam != null) {
            cam.setSpeedFovBonus(normalised);
            if (cam.isPacketDirty()) cmd.run(s -> s.putComponent(ref, CameraComponent.getComponentType(), cam));
        }
    }

    private static void persistCam(CommandBuffer<EntityStore> cmd, Ref<EntityStore> ref,
                                   CameraComponent cam) {
        cmd.run(s -> s.putComponent(ref, CameraComponent.getComponentType(), cam));
    }

    private static boolean isPlayerMoving(ArchetypeChunk<EntityStore> chunk, int index) {
        PlayerBodyComponent body = chunk.getComponent(index, PlayerBodyComponent.getComponentType());
        return body != null && body.getLocomotionState() != PlayerBodyComponent.LocomotionState.IDLE;
    }

    private static void sendPacket(PlayerRef pr, SetServerCamera packet) {
        try {
            pr.getPacketHandler().writeNoCache(packet);
        } catch (Exception e) {
            Nexus.get().getLogger().at(Level.WARNING).log("CameraSystem: packet send failed — " + e.getMessage());
        }
    }
}
