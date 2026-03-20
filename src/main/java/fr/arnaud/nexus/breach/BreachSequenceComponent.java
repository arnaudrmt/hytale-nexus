package fr.arnaud.nexus.breach;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Per-player FSM state for the Breach Sequence (mini-boss Switch Strike).
 * Ephemeral — state is meaningless across sessions, no codec needed.
 * <p>
 * State machine:
 * <pre>
 *   IDLE ──begin()──► ENTERING ──camera ready──► ACTIVE ──hit/timeout──► EXITING ──done──► IDLE
 * </pre>
 */
public final class BreachSequenceComponent implements Component<EntityStore> {

    public enum State {
        IDLE,
        ENTERING,
        ACTIVE,
        EXITING
    }

    public static final float AIM_WINDOW_SECONDS = 7.0f;
    public static final float TIME_DILATION = 0.3f;

    @Nullable
    private static ComponentType<EntityStore, BreachSequenceComponent> componentType;

    private State state = State.IDLE;
    private float aimTimer = 0f;
    private boolean breachWasHit = false;

    /**
     * The mini-boss entity this sequence is targeting.
     * Held so breach entities can be positioned around it on entry, and so
     * the execution system can apply damage on a successful breach hit.
     */
    @Nullable
    private Ref<EntityStore> bossRef;

    /**
     * Live refs to the spawned breach NPC entities.
     * Populated by {@link BreachSpawner} on ENTERING → ACTIVE.
     * Cleared on exit so surviving breaches can be despawned.
     */
    private final List<Ref<EntityStore>> activeBreachRefs = new ArrayList<>();

    public BreachSequenceComponent() {
    }

    /**
     * Begins the sequence targeting the given mini-boss. Only valid from IDLE.
     */
    public boolean begin(@NonNullDecl Ref<EntityStore> boss) {
        if (state != State.IDLE) return false;
        state = State.ENTERING;
        bossRef = boss;
        aimTimer = AIM_WINDOW_SECONDS;
        breachWasHit = false;
        activeBreachRefs.clear();
        return true;
    }

    /**
     * Called by {@link BreachSequenceSystem} once the camera entry transition
     * completes. Advances state to ACTIVE immediately; breach refs arrive
     * asynchronously via {@link #registerSpawnedRefs} once the deferred spawn runs.
     */
    public void onEnteringComplete() {
        state = State.ACTIVE;
        activeBreachRefs.clear();
    }

    /**
     * Called by {@link BreachSpawner} on the world thread
     * after the deferred spawn completes, registering the live breach refs.
     */
    public void registerSpawnedRefs(@NonNullDecl List<Ref<EntityStore>> refs) {
        activeBreachRefs.addAll(refs);
    }

    /**
     * Called when the player hits a breach or the aim window expires.
     */
    public void beginExit(boolean breachHit) {
        if (state != State.ACTIVE) return;
        state = State.EXITING;
        breachWasHit = breachHit;
    }

    /**
     * Called by {@link BreachSequenceSystem} once the camera exit transition
     * completes and the world speed is restored.
     */
    public void onExitComplete() {
        state = State.IDLE;
        bossRef = null;
        aimTimer = 0f;
        breachWasHit = false;
        activeBreachRefs.clear();
    }

    /**
     * Removes a breach ref once its entity has been destroyed (hit or despawned).
     */
    public void removeBreachRef(@NonNullDecl Ref<EntityStore> ref) {
        activeBreachRefs.remove(ref);
    }

    /**
     * Advances the aim window timer. Returns true while the window is still alive.
     */
    public boolean tickAimWindow(float deltaSeconds) {
        aimTimer -= deltaSeconds;
        return aimTimer > 0f;
    }

    public State getState() {
        return state;
    }

    @Nullable
    public Ref<EntityStore> getBossRef() {
        return bossRef;
    }

    public boolean wasBreachHit() {
        return breachWasHit;
    }

    public float getAimTimer() {
        return aimTimer;
    }

    public List<Ref<EntityStore>> getActiveBreachRefs() {
        return Collections.unmodifiableList(activeBreachRefs);
    }

    @NonNullDecl
    public static ComponentType<EntityStore, BreachSequenceComponent> getComponentType() {
        if (componentType == null) throw new IllegalStateException("BreachSequenceComponent not registered.");
        return componentType;
    }

    public static void setComponentType(
        @NonNullDecl ComponentType<EntityStore, BreachSequenceComponent> type) {
        componentType = type;
    }

    @Override
    @NonNullDecl
    public BreachSequenceComponent clone() {
        BreachSequenceComponent c = new BreachSequenceComponent();
        c.state = this.state;
        c.aimTimer = this.aimTimer;
        c.bossRef = this.bossRef;
        c.breachWasHit = this.breachWasHit;
        c.activeBreachRefs.addAll(this.activeBreachRefs);
        return c;
    }
}
