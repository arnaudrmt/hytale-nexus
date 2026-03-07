package fr.arnaud.nexus.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;

/**
 * Tracks player orientation, locomotion states, and dash mechanics.
 */
public final class PlayerBodyComponent implements Component<EntityStore> {

    public enum LocomotionState {IDLE, FORWARD, STRAFE_RIGHT, STRAFE_LEFT, BACKPEDAL}

    public enum DashState {IDLE, DASHING, COOLDOWN}

    public static final float DASH_DURATION_SEC = 0.25f;
    public static final float IFRAME_DURATION_SEC = 0.15f;
    public static final float DASH_COOLDOWN_SEC = 0.50f;
    public static final float MOVE_THRESHOLD = 0.5f;
    public static final float MAX_SPEED_FOR_FOV = 12.0f;

    private static final float FORWARD_HALF_ARC = (float) Math.toRadians(67.5);
    private static final float BACKPEDAL_HALF_ARC = (float) Math.toRadians(112.5);

    @Nullable
    private static ComponentType<EntityStore, PlayerBodyComponent> componentType;

    private float aimYaw, bodyYaw;
    private LocomotionState locomotionState = LocomotionState.IDLE;

    private DashState dashState = DashState.IDLE;
    private float dashElapsedSec, cooldownRemaining, iFrameElapsedSec;
    private float dashDirX, dashDirZ;

    private boolean perfectDodgeWindowOpen, perfectDodgeConsumed;
    private float perfectDodgeWindowRemaining;

    private float lastX, lastZ;
    private boolean positionSeeded;

    public PlayerBodyComponent() {
    }

    // --- Orientation ---

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

    // --- Dash State ---

    public boolean beginDash() {
        if (dashState != DashState.IDLE) return false;

        this.dashState = DashState.DASHING;
        this.dashElapsedSec = 0f;
        this.iFrameElapsedSec = 0f;

        float dirYaw = (locomotionState != LocomotionState.IDLE) ? bodyYaw : aimYaw;
        this.dashDirX = -(float) Math.sin(dirYaw);
        this.dashDirZ = (float) Math.cos(dirYaw);
        return true;
    }

    public void tickTimers(float deltaSec) {
        if (dashState == DashState.DASHING) {
            dashElapsedSec += deltaSec;
            iFrameElapsedSec += deltaSec;
            if (dashElapsedSec >= DASH_DURATION_SEC) {
                dashState = DashState.COOLDOWN;
                cooldownRemaining = DASH_COOLDOWN_SEC;
            }
        } else if (dashState == DashState.COOLDOWN) {
            cooldownRemaining -= deltaSec;
            if (cooldownRemaining <= 0f) dashState = DashState.IDLE;
        }

        if (perfectDodgeWindowOpen) {
            perfectDodgeWindowRemaining -= deltaSec;
            if (perfectDodgeWindowRemaining <= 0f) {
                perfectDodgeWindowOpen = false;
                perfectDodgeConsumed = false;
            }
        }
    }

    public void openPerfectDodgeWindow(float durationSec) {
        this.perfectDodgeWindowOpen = true;
        this.perfectDodgeWindowRemaining = durationSec;
        this.perfectDodgeConsumed = false;
    }

    public boolean consumePerfectDodge() {
        if (!perfectDodgeWindowOpen || perfectDodgeConsumed) return false;
        return perfectDodgeConsumed = true;
    }

    public DashState getDashState() {
        return dashState;
    }

    public float getAimYaw() {
        return aimYaw;
    }

    public float getDashDirX() {
        return dashDirX;
    }

    public float getDashDirZ() {
        return dashDirZ;
    }

    public float getDashElapsedSec() {
        return dashElapsedSec;
    }

    public boolean isIFrameActive() {
        return dashState == DashState.DASHING
            && iFrameElapsedSec < IFRAME_DURATION_SEC;
    }

    // --- Speed Sampling ---

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

    // --- ECS Boilerplate ---

    @NonNullDecl
    public static ComponentType<EntityStore, PlayerBodyComponent> getComponentType() {
        if (componentType == null) throw new IllegalStateException("PlayerBodyComponent not registered.");
        return componentType;
    }

    public static void setComponentType(@NonNullDecl ComponentType<EntityStore, PlayerBodyComponent> type) {
        componentType = type;
    }

    @Override
    public PlayerBodyComponent clone() {
        PlayerBodyComponent c = new PlayerBodyComponent();
        /**
         * (Full field copy here)
         */
        return c;
    }
}
