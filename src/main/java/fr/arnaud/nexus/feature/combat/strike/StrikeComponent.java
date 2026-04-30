package fr.arnaud.nexus.feature.combat.strike;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.util.MessageUtil;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public final class StrikeComponent implements Component<EntityStore> {

    public enum State {IDLE, HIT_WINDOW, SWITCH_WINDOW, COMBO}

    public static final float HIT_COLLECTION_SECONDS = 5.0f;
    public static final float SWITCH_WINDOW_SECONDS = 5.0f;
    public static final float COMBO_WINDOW_REAL_SECONDS = 5.0f;
    public static final float COMBO_WINDOW_SECONDS = COMBO_WINDOW_REAL_SECONDS * 0.3f;
    public static final float STAMINA_COST_RATIO = 0.50f;

    @Nullable
    private static ComponentType<EntityStore, StrikeComponent> componentType;

    private State state = State.IDLE;
    private float stateTimer = 0f;

    private final Map<Ref<EntityStore>, TargetCombo> targetCombos = new HashMap<>();

    /**
     * Camera focus: last mob hit during HIT_WINDOW.
     */
    @Nullable
    private Ref<EntityStore> cameraFocusRef;

    public StrikeComponent() {
    }

    public void openHitWindow() {
        state = State.HIT_WINDOW;
        stateTimer = HIT_COLLECTION_SECONDS;
        targetCombos.clear();
        cameraFocusRef = null;
    }

    public void openSwitchWindow() {
        state = State.SWITCH_WINDOW;
        stateTimer = SWITCH_WINDOW_SECONDS;
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

    public boolean tickTimer(float deltaSeconds) {
        stateTimer -= deltaSeconds;
        return stateTimer > 0f;
    }

    public void registerHitTarget(@NonNullDecl Ref<EntityStore> targetRef) {
        targetCombos.putIfAbsent(targetRef, new TargetCombo());
        cameraFocusRef = targetRef;
    }

    public boolean hasHitTargets() {
        return !targetCombos.isEmpty();
    }

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

    public float getStateTimer() {
        return stateTimer;
    }

    @NonNullDecl
    public static ComponentType<EntityStore, StrikeComponent> getComponentType() {
        if (componentType == null)
            throw new IllegalStateException(MessageUtil.componentNotRegistered(StrikeComponent.class.getSimpleName()));
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
