package fr.arnaud.nexus.feature.combat;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.modules.splitvelocity.VelocityConfig;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.camera.PlayerCameraSystem;
import fr.arnaud.nexus.feature.movement.PlayerDashComponent;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.jetbrains.annotations.NotNull;

public final class PlayerLocomotionSystem extends EntityTickingSystem<EntityStore> {

    private static final float DASH_SPEED = 20.0f;

    private static final VelocityConfig DASH_VELOCITY_CONFIG = new VelocityConfig();

    @NonNullDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
            PlayerBodyStateComponent.getComponentType(),
            PlayerDashComponent.getComponentType(),
            PlayerRef.getComponentType()
        );
    }

    @Override
    public void tick(float deltaSeconds, int index, ArchetypeChunk<EntityStore> chunk,
                     @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> commandBuffer) {

        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        PlayerBodyStateComponent body = chunk.getComponent(index, PlayerBodyStateComponent.getComponentType());
        PlayerDashComponent dash = chunk.getComponent(index, PlayerDashComponent.getComponentType());
        PlayerRef pr = chunk.getComponent(index, PlayerRef.getComponentType());

        if (body == null || dash == null || pr == null) return;

        var headRot = store.getComponent(ref, HeadRotation.getComponentType());
        var velocity = store.getComponent(ref, Velocity.getComponentType());

        float aimYaw = (headRot != null) ? headRot.getRotation().getYaw() : body.getAimYaw();
        float vx = (velocity != null) ? (float) velocity.getX() : 0f;
        float vz = (velocity != null) ? (float) velocity.getZ() : 0f;
        body.updateOrientation(aimYaw, vx, vz);

        if (dash.consumeDashImpulse() && velocity != null) {
            applyDashVelocity(dash, velocity);
            commandBuffer.run(s -> s.putComponent(ref, Velocity.getComponentType(), velocity));
        }

        dash.tick(deltaSeconds);

        var transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform != null) {
            feedSpeedFov(body, transform, ref, store, commandBuffer, deltaSeconds);
        }

        commandBuffer.run(s -> {
            s.putComponent(ref, PlayerBodyStateComponent.getComponentType(), body);
            s.putComponent(ref, PlayerDashComponent.getComponentType(), dash);
        });
    }

    // --- Static API used by other systems ---

    public static boolean isIFrameActive(@NonNullDecl Ref<EntityStore> ref,
                                         @NonNullDecl Store<EntityStore> store) {
        PlayerDashComponent dash = store.getComponent(ref, PlayerDashComponent.getComponentType());
        return dash != null && dash.isIFrameActive();
    }

    public static void openPerfectDodgeWindow(@NonNullDecl Ref<EntityStore> ref,
                                              @NonNullDecl Store<EntityStore> store,
                                              @NonNullDecl CommandBuffer<EntityStore> commandBuffer,
                                              float durationSec) {
        PlayerDashComponent dash = store.getComponent(ref, PlayerDashComponent.getComponentType());
        if (dash != null) {
            dash.openPerfectDodgeWindow(durationSec);
            commandBuffer.run(s -> s.putComponent(ref, PlayerDashComponent.getComponentType(), dash));
        }
    }

    // --- Helpers ---

    private static void applyDashVelocity(@NonNullDecl PlayerDashComponent dash,
                                          @NonNullDecl Velocity velocity) {
        Vector3d dashVector = new Vector3d(
            dash.getDashDirX() * DASH_SPEED,
            10.0,
            dash.getDashDirZ() * DASH_SPEED
        );
        velocity.addInstruction(dashVector, DASH_VELOCITY_CONFIG, ChangeVelocityType.Set);
    }

    private static void feedSpeedFov(@NonNullDecl PlayerBodyStateComponent body,
                                     @NonNullDecl TransformComponent transform,
                                     @NonNullDecl Ref<EntityStore> ref,
                                     @NonNullDecl Store<EntityStore> store,
                                     @NonNullDecl CommandBuffer<EntityStore> commandBuffer,
                                     float deltaSec) {
        var pos = transform.getPosition();
        float normalised = body.sampleSpeed((float) pos.getX(), (float) pos.getZ(), deltaSec);
        PlayerCameraSystem.updateSpeedFov(ref, store, commandBuffer, normalised);
    }
}
