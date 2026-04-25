package fr.arnaud.nexus.feature.combat.strike;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Per-player FSM state for the Strike mechanic. Ephemeral.
 *
 * <p>States:
 * <pre>
 *   IDLE        → no active strike
 *   HIT_WINDOW  → Ability2 fired, collecting hits for up to {@link #HIT_COLLECTION_SECONDS}
 *   COMBO       → at least one mob hit; frozen targets absorbing combo hits for {@link #COMBO_WINDOW_SECONDS}
 * </pre>
 */
public final class StrikeComponent implements Component<EntityStore> {

    public enum State {IDLE, HIT_WINDOW, COMBO}

    public static final float HIT_COLLECTION_SECONDS = 2.0f;
    public static final float COMBO_WINDOW_REAL_SECONDS = 5.0f;
    public static final float COMBO_WINDOW_SECONDS = COMBO_WINDOW_REAL_SECONDS * 0.3f;
    
    public static final float STAMINA_COST_RATIO = 0.75f;

    @Nullable
    private static ComponentType<EntityStore, StrikeComponent> componentType;

    private State state = State.IDLE;
    private float stateTimer = 0f;

    /**
     * Per-target combo state. Key = frozen mob ref, value = accumulated combo damage for that mob.
     * hitCount is stored per-target so the formula hitN × rawDamage is independent per mob.
     */
    private final Map<Ref<EntityStore>, TargetCombo> targetCombos = new HashMap<>();

    /**
     * Camera focus: last mob hit during HIT_WINDOW.
     */
    @Nullable
    private Ref<EntityStore> cameraFocusRef;

    public StrikeComponent() {
    }

    // ── State transitions ─────────────────────────────────────────────────────

    public void openHitWindow() {
        state = State.HIT_WINDOW;
        stateTimer = HIT_COLLECTION_SECONDS;
        targetCombos.clear();
        cameraFocusRef = null;
    }

    public void openComboWindow() {
        state = State.COMBO;
        stateTimer = COMBO_WINDOW_SECONDS;
    }

    public void reset() {
        state = State.IDLE;
        stateTimer = 0f;
        targetCombos.clear();
        cameraFocusRef = null;
    }

    // ── Timer ─────────────────────────────────────────────────────────────────

    /**
     * @return true while the timer is still running
     */
    public boolean tickTimer(float deltaSeconds) {
        stateTimer -= deltaSeconds;
        return stateTimer > 0f;
    }

    // ── Hit registration (HIT_WINDOW phase) ──────────────────────────────────

    public void registerHitTarget(@NonNullDecl Ref<EntityStore> targetRef) {
        targetCombos.putIfAbsent(targetRef, new TargetCombo());
        cameraFocusRef = targetRef;
    }

    public boolean hasHitTargets() {
        return !targetCombos.isEmpty();
    }

    // ── Combo accumulation (COMBO phase) ─────────────────────────────────────

    /**
     * Registers a combo hit on a specific frozen target.
     * Formula: hit N on this target contributes N × rawDamage.
     *
     * @return true if this target is tracked (was hit during the window)
     */
    public boolean registerComboHit(@NonNullDecl Ref<EntityStore> targetRef, float rawDamage) {
        TargetCombo combo = targetCombos.get(targetRef);
        if (combo == null) return false;
        combo.hitCount++;
        combo.accumulatedDamage += combo.hitCount * rawDamage;
        return true;
    }

    public Map<Ref<EntityStore>, TargetCombo> getTargetCombos() {
        return targetCombos;
    }

    @Nullable
    public Ref<EntityStore> getCameraFocusRef() {
        return cameraFocusRef;
    }

    public State getState() {
        return state;
    }

    // ── Component boilerplate ─────────────────────────────────────────────────

    @NonNullDecl
    public static ComponentType<EntityStore, StrikeComponent> getComponentType() {
        if (componentType == null) throw new IllegalStateException("StrikeComponent not registered.");
        return componentType;
    }

    public static void setComponentType(@NonNullDecl ComponentType<EntityStore, StrikeComponent> type) {
        componentType = type;
    }

    @Override
    @NonNullDecl
    public StrikeComponent clone() {
        StrikeComponent c = new StrikeComponent();
        c.state = this.state;
        c.stateTimer = this.stateTimer;
        c.cameraFocusRef = this.cameraFocusRef;
        this.targetCombos.forEach((ref, combo) -> c.targetCombos.put(ref, combo.copy()));
        return c;
    }

    // ── Inner data class ──────────────────────────────────────────────────────

    public static final class TargetCombo {
        public int hitCount = 0;
        public float accumulatedDamage = 0f;

        TargetCombo copy() {
            TargetCombo t = new TargetCombo();
            t.hitCount = this.hitCount;
            t.accumulatedDamage = this.accumulatedDamage;
            return t;
        }
    }
}
