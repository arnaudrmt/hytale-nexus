package fr.arnaud.nexus.feature.combat.switchstrike;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;

/**
 * Per-player FSM state for the Switch Strike mechanic. Ephemeral — no codec needed.
 */
public final class SwitchStrikeComponent implements Component<EntityStore> {

    public enum State {IDLE, PENDING_OPEN, WINDOW_OPEN}

    public static final float WINDOW_DURATION_SECONDS = 1.0f;

    @Nullable
    private static ComponentType<EntityStore, SwitchStrikeComponent> componentType;

    private State state = State.IDLE;
    private float windowTimer = 0f;
    private boolean pendingSwap = false;
    private boolean swapWasMeleeToRanged = false;
    private float bossHitTimer = 0f;

    @Nullable
    private Ref<EntityStore> lastBossRef;

    public SwitchStrikeComponent() {
    }

    public void requestOpenWindow() {
        state = State.PENDING_OPEN;
        pendingSwap = false;
        swapWasMeleeToRanged = false;
    }

    public void commitOpenWindow() {
        state = State.WINDOW_OPEN;
        windowTimer = WINDOW_DURATION_SECONDS;
    }

    public void closeWindow() {
        state = State.IDLE;
        windowTimer = 0f;
        pendingSwap = false;
        swapWasMeleeToRanged = false;
    }

    public void markBossHit(@NonNullDecl Ref<EntityStore> bossRef) {
        bossHitTimer = WINDOW_DURATION_SECONDS;
        lastBossRef = bossRef;
    }

    public boolean hasBossHitInWindow() {
        return bossHitTimer > 0f;
    }

    public void tickBossTimer(float deltaSeconds) {
        if (bossHitTimer > 0f) {
            bossHitTimer = Math.max(0f, bossHitTimer - deltaSeconds);
            if (bossHitTimer == 0f) lastBossRef = null;
        }
    }

    public void signalSwap(boolean meleeToRanged) {
        pendingSwap = true;
        swapWasMeleeToRanged = meleeToRanged;
    }

    public boolean tickWindow(float deltaSeconds) {
        if (state != State.WINDOW_OPEN) return false;
        windowTimer -= deltaSeconds;
        return windowTimer > 0f;
    }

    public State getState() {
        return state;
    }

    public float getWindowTimer() {
        return windowTimer;
    }

    public boolean hasPendingSwap() {
        return pendingSwap;
    }

    public boolean wasSwapMeleeToRanged() {
        return swapWasMeleeToRanged;
    }

    @Nullable
    public Ref<EntityStore> getLastBossRef() {
        return lastBossRef;
    }

    @NonNullDecl
    public static ComponentType<EntityStore, SwitchStrikeComponent> getComponentType() {
        if (componentType == null) throw new IllegalStateException("SwitchStrikeComponent not registered.");
        return componentType;
    }

    public static void setComponentType(@NonNullDecl ComponentType<EntityStore, SwitchStrikeComponent> type) {
        componentType = type;
    }

    @Override
    @NonNullDecl
    public SwitchStrikeComponent clone() {
        SwitchStrikeComponent c = new SwitchStrikeComponent();
        c.state = this.state;
        c.windowTimer = this.windowTimer;
        c.pendingSwap = this.pendingSwap;
        c.swapWasMeleeToRanged = this.swapWasMeleeToRanged;
        c.bossHitTimer = this.bossHitTimer;
        c.lastBossRef = this.lastBossRef;
        return c;
    }
}
