package fr.arnaud.nexus.ability.dash;

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
import fr.arnaud.nexus.feature.movement.PlayerOrientationComponent;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public final class DashSystem extends EntityTickingSystem<EntityStore> {

    private static final float DASH_SPEED = 20.0f;
    private static final VelocityConfig DASH_VELOCITY_CONFIG = new VelocityConfig();

    @NonNullDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
            PlayerOrientationComponent.getComponentType(),
            DashComponent.getComponentType(),
            PlayerRef.getComponentType()
        );
    }

    @Override
    public void tick(float deltaSeconds, int index, ArchetypeChunk<EntityStore> chunk,
                     @NonNullDecl Store<EntityStore> store,
                     @NonNullDecl CommandBuffer<EntityStore> commandBuffer) {

        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        PlayerOrientationComponent orientation = chunk.getComponent(index, PlayerOrientationComponent.getComponentType());
        DashComponent dash = chunk.getComponent(index, DashComponent.getComponentType());
        PlayerRef pr = chunk.getComponent(index, PlayerRef.getComponentType());

        if (orientation == null || dash == null || pr == null) return;

        HeadRotation headRot = store.getComponent(ref, HeadRotation.getComponentType());
        Velocity velocity = store.getComponent(ref, Velocity.getComponentType());

        float aimYaw = (headRot != null) ? headRot.getRotation().getYaw() : orientation.getAimYaw();
        float vx = (velocity != null) ? (float) velocity.getX() : 0f;
        float vz = (velocity != null) ? (float) velocity.getZ() : 0f;
        orientation.updateOrientation(aimYaw, vx, vz);

        if (dash.consumeDashImpulse() && velocity != null) {
            applyDashVelocity(dash, velocity);
            commandBuffer.run(s -> s.putComponent(ref, Velocity.getComponentType(), velocity));
        }

        dash.tick(deltaSeconds);

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform != null) {
            feedSpeedFov(orientation, transform, ref, store, commandBuffer, deltaSeconds);
        }

        commandBuffer.run(s -> {
            s.putComponent(ref, PlayerOrientationComponent.getComponentType(), orientation);
            s.putComponent(ref, DashComponent.getComponentType(), dash);
        });
    }

    private static void applyDashVelocity(@NonNullDecl DashComponent dash,
                                          @NonNullDecl Velocity velocity) {
        Vector3d dashVector = new Vector3d(
            dash.getDashDirX() * DASH_SPEED,
            10.0,
            dash.getDashDirZ() * DASH_SPEED
        );
        velocity.addInstruction(dashVector, DASH_VELOCITY_CONFIG, ChangeVelocityType.Set);
    }

    private static void feedSpeedFov(@NonNullDecl PlayerOrientationComponent orientation,
                                     @NonNullDecl TransformComponent transform,
                                     @NonNullDecl Ref<EntityStore> ref,
                                     @NonNullDecl Store<EntityStore> store,
                                     @NonNullDecl CommandBuffer<EntityStore> commandBuffer,
                                     float deltaSec) {
        var pos = transform.getPosition();
        float normalised = orientation.sampleSpeed((float) pos.getX(), (float) pos.getZ(), deltaSec);
        PlayerCameraSystem.updateSpeedFov(ref, store, commandBuffer, normalised);
    }
}
