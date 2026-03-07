package fr.arnaud.nexus.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.camera.CameraSystem;
import fr.arnaud.nexus.component.FlowComponent;
import fr.arnaud.nexus.component.PlayerBodyComponent;
import fr.arnaud.nexus.component.PlayerBodyComponent.DashState;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

/**
 * System that manages player movement, dash locomotion, and orientation.
 */
public final class PlayerMovementSystem extends EntityTickingSystem<EntityStore> {

    private static final float DASH_SPEED = 20.0f;

    @NonNullDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
            PlayerBodyComponent.getComponentType(),
            PlayerRef.getComponentType(),
            FlowComponent.getComponentType()
        );
    }

    @Override
    public void tick(float deltaSeconds, int index, ArchetypeChunk<EntityStore> chunk,
                     Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {

        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        PlayerBodyComponent body = chunk.getComponent(index, PlayerBodyComponent.getComponentType());
        PlayerRef pr = chunk.getComponent(index, PlayerRef.getComponentType());
        if (body == null || pr == null) return;

        // 1. Update orientation from live engine components
        var headRot = store.getComponent(ref, HeadRotation.getComponentType());
        var velocity = store.getComponent(ref, Velocity.getComponentType());

        float aimYaw = (headRot != null) ? (float) headRot.getRotation().getYaw() : body.getAimYaw();
        float vx = (velocity != null) ? (float) velocity.getX() : 0f;
        float vz = (velocity != null) ? (float) velocity.getZ() : 0f;
        body.updateOrientation(aimYaw, vx, vz);

        // 2. Apply dash impulse on the first tick of the dash
        if (body.getDashState() == DashState.DASHING && body.getDashElapsedSec() == 0f && velocity != null) {
            applyDashImpulse(body, velocity);
            commandBuffer.run(s -> s.putComponent(ref, Velocity.getComponentType(), velocity));
        }

        // 3. Advance state machines
        body.tickTimers(deltaSeconds);

        // 4. Update Camera FOV based on speed
        var transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform != null) {
            feedSpeedFov(body, transform, ref, store, commandBuffer, deltaSeconds);
        }

        // 5. Persist changes
        commandBuffer.run(s -> s.putComponent(ref, PlayerBodyComponent.getComponentType(), body));
    }

    // --- Static API ---

    /**
     * Requests a dash for the player. Guards state and flow requirements.
     */
    public static boolean requestDash(Ref<EntityStore> ref, Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {
        PlayerBodyComponent body = store.getComponent(ref, PlayerBodyComponent.getComponentType());
        FlowComponent flow = store.getComponent(ref, FlowComponent.getComponentType());

        if (body == null || flow == null || body.getDashState() != DashState.IDLE) return false;

        flow.drainFractional(FlowComponent.DASH_FLOW_COST);
        if (!body.beginDash()) return false;

        // Perfect-dodge reward
        if (body.consumePerfectDodge()) {
            commandBuffer.run(s -> {
                FlowComponent f = s.getComponent(ref, FlowComponent.getComponentType());
                if (f != null) {
                    f.addFlow(1.0f);
                    s.putComponent(ref, FlowComponent.getComponentType(), f);
                }
            });
        }

        commandBuffer.run(s -> {
            s.putComponent(ref, PlayerBodyComponent.getComponentType(), body);
            s.putComponent(ref, FlowComponent.getComponentType(), flow);
        });
        return true;
    }

    /**
     * @return true if player is currently in I-frames.
     */
    public static boolean isIFrameActive(Ref<EntityStore> ref, Store<EntityStore> store) {
        PlayerBodyComponent body = store.getComponent(ref, PlayerBodyComponent.getComponentType());
        return body != null && body.isIFrameActive();
    }

    /**
     * Opens a perfect-dodge window for the specified duration.
     */
    public static void openPerfectDodgeWindow(Ref<EntityStore> ref, Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer, float durationSec) {
        PlayerBodyComponent body = store.getComponent(ref, PlayerBodyComponent.getComponentType());
        if (body != null) {
            body.openPerfectDodgeWindow(durationSec);
            commandBuffer.run(s -> s.putComponent(ref, PlayerBodyComponent.getComponentType(), body));
        }
    }

    // --- Helpers ---

    private static void applyDashImpulse(PlayerBodyComponent body, Velocity velocity) {
        velocity.setX(body.getDashDirX() * DASH_SPEED);
        velocity.setZ(body.getDashDirZ() * DASH_SPEED);
    }

    private static void feedSpeedFov(PlayerBodyComponent body, TransformComponent transform, Ref<EntityStore> ref,
                                     Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer, float deltaSec) {
        var pos = transform.getPosition();
        float normalised = body.sampleSpeed((float) pos.getX(), (float) pos.getZ(), deltaSec);
        CameraSystem.updateSpeedFov(ref, store, commandBuffer, normalised);
    }
}
