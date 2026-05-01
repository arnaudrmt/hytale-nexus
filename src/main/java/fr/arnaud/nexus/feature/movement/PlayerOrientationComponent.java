package fr.arnaud.nexus.feature.movement;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;

/**
 * Tracks player orientation and locomotion state.
 * Used by the camera system for speed-based FOV sampling.
 */
public final class PlayerOrientationComponent implements Component<EntityStore> {

    public enum LocomotionState {IDLE, FORWARD, STRAFE_RIGHT, STRAFE_LEFT, BACKPEDAL}

    public static final float MOVE_THRESHOLD = 0.5f;
    public static final float MAX_SPEED_FOR_FOV = 12.0f;

    private static final float FORWARD_HALF_ARC = (float) Math.toRadians(67.5);
    private static final float BACKPEDAL_HALF_ARC = (float) Math.toRadians(112.5);

    @Nullable
    private static ComponentType<EntityStore, PlayerOrientationComponent> componentType;

    private float aimYaw, bodyYaw;
    private LocomotionState locomotionState = LocomotionState.IDLE;
    private float lastX, lastZ;
    private boolean positionSeeded;

    public PlayerOrientationComponent() {
    }

    public void updateOrientation(float newAimYaw, float vx, float vz) {
        this.aimYaw = newAimYaw;
        float speed = (float) Math.sqrt(vx * vx + vz * vz);
        if (speed > MOVE_THRESHOLD) {
            this.bodyYaw = (float) Math.atan2(-vx, vz);
            this.locomotionState = classifyLocomotion(aimYaw, bodyYaw);
        } else {
            this.locomotionState = LocomotionState.IDLE;
        }
    }

    private static LocomotionState classifyLocomotion(float aimYaw, float bodyYaw) {
        float diff = normalizeAngle(aimYaw - bodyYaw);
        if (Math.abs(diff) <= FORWARD_HALF_ARC) return LocomotionState.FORWARD;
        if (Math.abs(diff) >= BACKPEDAL_HALF_ARC) return LocomotionState.BACKPEDAL;
        return diff > 0f ? LocomotionState.STRAFE_RIGHT : LocomotionState.STRAFE_LEFT;
    }

    public static float normalizeAngle(float angle) {
        angle %= (float) (2 * Math.PI);
        if (angle > Math.PI) angle -= (float) (2 * Math.PI);
        if (angle <= -Math.PI) angle += (float) (2 * Math.PI);
        return angle;
    }

    public float sampleSpeed(float x, float z, float deltaSec) {
        if (!positionSeeded) {
            lastX = x;
            lastZ = z;
            positionSeeded = true;
            return 0f;
        }
        float dx = x - lastX, dz = z - lastZ;
        lastX = x;
        lastZ = z;
        if (deltaSec <= 0f) return 0f;
        return Math.min(1f, (float) Math.sqrt(dx * dx + dz * dz) / deltaSec / MAX_SPEED_FOR_FOV);
    }

    public float getAimYaw() {
        return aimYaw;
    }

    public LocomotionState getLocomotionState() {
        return locomotionState;
    }

    @NonNullDecl
    public static ComponentType<EntityStore, PlayerOrientationComponent> getComponentType() {
        if (componentType == null) throw new IllegalStateException("PlayerOrientationComponent not registered.");
        return componentType;
    }

    public static void setComponentType(@NonNullDecl ComponentType<EntityStore, PlayerOrientationComponent> type) {
        componentType = type;
    }

    @Override
    @NonNullDecl
    public PlayerOrientationComponent clone() {
        PlayerOrientationComponent c = new PlayerOrientationComponent();
        c.aimYaw = this.aimYaw;
        c.bodyYaw = this.bodyYaw;
        c.locomotionState = this.locomotionState;
        c.lastX = this.lastX;
        c.lastZ = this.lastZ;
        c.positionSeeded = this.positionSeeded;
        return c;
    }
}
