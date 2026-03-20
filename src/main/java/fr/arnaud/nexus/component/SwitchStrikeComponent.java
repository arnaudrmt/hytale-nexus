package fr.arnaud.nexus.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;

/**
 * Tracks the per-player FSM state for the Switch Strike mechanic.
 * Ephemeral — no codec needed, state is meaningless across sessions.
 */
public final class SwitchStrikeComponent implements Component<EntityStore> {

    public enum State {IDLE, WINDOW_OPEN}

    public static final float WINDOW_DURATION_SECONDS = 1.0f;

    @Nullable
    private static ComponentType<EntityStore, SwitchStrikeComponent> componentType;

    private State state = State.IDLE;
    private float windowTimer = 0f;
    private boolean pendingSwap = false;
    private boolean swapWasMeleeToRanged = false;

    /**
     * Written by {@link fr.arnaud.nexus.system.SwitchStrikeDamageInterceptor} the moment
     * Ability1 connects. Read and consumed by {@link fr.arnaud.nexus.system.SwitchStrikeTriggerSystem}
     * when it opens the window, so the execution phase always has a valid target ref.
     */
    @Nullable
    private Ref<EntityStore> pendingTriggerTarget;

    /**
     * The entity hit by Ability1. Carried into the execution phase so we can
     * branch on whether the target is a mini-boss.
     */
    @Nullable
    private Ref<EntityStore> abilityTarget;

    public SwitchStrikeComponent() {
    }

    public void openWindow(@Nullable Ref<EntityStore> target) {
        state = State.WINDOW_OPEN;
        windowTimer = WINDOW_DURATION_SECONDS;
        pendingSwap = false;
        swapWasMeleeToRanged = false;
        abilityTarget = target;
    }

    public void closeWindow() {
        state = State.IDLE;
        windowTimer = 0f;
        pendingSwap = false;
        swapWasMeleeToRanged = false;
        abilityTarget = null;
    }

    /**
     * Called by the packet interceptor on the Netty thread.
     * {@code meleeToRanged} is true when the player was on slot 0 (melee) and
     * swapped to slot 1 (ranged). The ticking system reads both flags next tick.
     */
    public void signalSwap(boolean meleeToRanged) {
        pendingSwap = true;
        swapWasMeleeToRanged = meleeToRanged;
    }

    /**
     * Advances the window timer. Returns {@code true} while the window is still alive.
     */
    public boolean tickWindow(float deltaSeconds) {
        if (state != State.WINDOW_OPEN) return false;
        windowTimer -= deltaSeconds;
        return windowTimer > 0f;
    }

    public void setPendingTriggerTarget(@Nullable Ref<EntityStore> target) {
        pendingTriggerTarget = target;
    }

    /**
     * Returns the staged hit target and clears it so it cannot be consumed twice.
     */
    @Nullable
    public Ref<EntityStore> consumePendingTriggerTarget() {
        Ref<EntityStore> t = pendingTriggerTarget;
        pendingTriggerTarget = null;
        return t;
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
    public Ref<EntityStore> getAbilityTarget() {
        return abilityTarget;
    }

    @NonNullDecl
    public static ComponentType<EntityStore, SwitchStrikeComponent> getComponentType() {
        if (componentType == null) throw new IllegalStateException("SwitchStrikeComponent not registered.");
        return componentType;
    }

    public static void setComponentType(
        @NonNullDecl ComponentType<EntityStore, SwitchStrikeComponent> type) {
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
        c.pendingTriggerTarget = this.pendingTriggerTarget;
        c.abilityTarget = this.abilityTarget;
        return c;
    }
}
