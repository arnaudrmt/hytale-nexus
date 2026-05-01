package fr.arnaud.nexus.ability.dash;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;

public final class DashComponent implements Component<EntityStore> {

    public enum DashState {IDLE, DASHING}

    public static final float DASH_DURATION_SEC = 0.25f;
    public static final float IFRAME_DURATION_SEC = 0.15f;

    @Nullable
    private static ComponentType<EntityStore, DashComponent> componentType;

    private DashState dashState = DashState.IDLE;
    private float dashElapsedSec;
    private float iFrameElapsedSec;
    private float dashDirX, dashDirZ;
    private boolean pendingDashImpulse;

    private boolean perfectDodgeWindowOpen;
    private boolean perfectDodgeConsumed;
    private float perfectDodgeWindowRemaining;

    public DashComponent() {
    }

    public void beginDash(float dirX, float dirZ) {
        if (dashState != DashState.IDLE) return;
        dashState = DashState.DASHING;
        dashElapsedSec = 0f;
        iFrameElapsedSec = 0f;
        pendingDashImpulse = true;
        dashDirX = dirX;
        dashDirZ = dirZ;
    }

    public boolean consumeDashImpulse() {
        if (!pendingDashImpulse) return false;
        pendingDashImpulse = false;
        return true;
    }

    public void tick(float deltaSec) {
        if (dashState == DashState.DASHING) {
            dashElapsedSec += deltaSec;
            iFrameElapsedSec += deltaSec;
            if (dashElapsedSec >= DASH_DURATION_SEC) {
                dashState = DashState.IDLE;
            }
        }

        if (perfectDodgeWindowOpen) {
            perfectDodgeWindowRemaining -= deltaSec;
            if (perfectDodgeWindowRemaining <= 0f) {
                perfectDodgeWindowOpen = false;
                perfectDodgeConsumed = false;
            }
        }
    }

    public DashState getDashState() {
        return dashState;
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

    public boolean isIdle() {
        return dashState == DashState.IDLE;
    }

    public boolean isIFrameActive() {
        return dashState == DashState.DASHING && iFrameElapsedSec < IFRAME_DURATION_SEC;
    }

    @NonNullDecl
    public static ComponentType<EntityStore, DashComponent> getComponentType() {
        if (componentType == null) throw new IllegalStateException("DashComponent not registered.");
        return componentType;
    }

    public static void setComponentType(@Nullable ComponentType<EntityStore, DashComponent> type) {
        componentType = type;
    }

    @Override
    @NonNullDecl
    public DashComponent clone() {
        DashComponent c = new DashComponent();
        c.dashState = this.dashState;
        c.dashElapsedSec = this.dashElapsedSec;
        c.iFrameElapsedSec = this.iFrameElapsedSec;
        c.dashDirX = this.dashDirX;
        c.dashDirZ = this.dashDirZ;
        c.pendingDashImpulse = this.pendingDashImpulse;
        c.perfectDodgeWindowOpen = this.perfectDodgeWindowOpen;
        c.perfectDodgeConsumed = this.perfectDodgeConsumed;
        c.perfectDodgeWindowRemaining = this.perfectDodgeWindowRemaining;
        return c;
    }
}
