package fr.arnaud.nexus.breach;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;

/**
 * Per-player state for an active Breach sequence.
 *
 * <p>Combo damage formula (GDD §IV.2):
 * {@code total = Σ (hitIndex × baseDamage)} for hitIndex = 1..n
 *
 * <p>Timer note: {@link #tickWindow} receives raw server delta-seconds. During
 * the ACTIVE phase, time dilation compresses the delta. Dividing by
 * {@link #TIME_DILATION_SLOW} converts back to real-wall-clock seconds so the
 * 5-second window is always 5 real seconds regardless of dilation.
 */
public final class BreachSequenceComponent implements Component<EntityStore> {

    public enum Phase {ENTRY, ACTIVE, EXIT}

    public static final float COMBO_WINDOW_SECONDS = 5.0f;
    public static final float TIME_DILATION_SLOW = 0.3f;
    public static final float TIME_DILATION_NORMAL = 1.0f;
    public static final float EXIT_KNOCKBACK_FORCE = 5.0f;

    @Nullable
    private static ComponentType<EntityStore, BreachSequenceComponent> componentType;

    private Phase phase = Phase.ENTRY;
    private float windowTimer = COMBO_WINDOW_SECONDS;

    @Nullable
    private Ref<EntityStore> bossRef;

    private float pendingDamage = 0f;
    private int comboHitCount = 0;
    private float savedDayTime = 0f;

    public BreachSequenceComponent() {
    }

    public static BreachSequenceComponent forBoss(
        @NonNullDecl Ref<EntityStore> bossRef, float currentDayTime) {
        BreachSequenceComponent c = new BreachSequenceComponent();
        c.bossRef = bossRef;
        c.savedDayTime = currentDayTime;
        return c;
    }

    public void advanceToActive() {
        phase = Phase.ACTIVE;
        windowTimer = COMBO_WINDOW_SECONDS;
    }

    public void advanceToExit() {
        phase = Phase.EXIT;
    }

    public void registerHit(float rawDamage) {
        comboHitCount++;
        pendingDamage += comboHitCount * rawDamage;
    }

    public boolean tickWindow(float deltaSeconds) {
        windowTimer -= deltaSeconds / TIME_DILATION_SLOW;
        return windowTimer > 0f;
    }

    public Phase getPhase() {
        return phase;
    }

    public float getWindowTimer() {
        return windowTimer;
    }

    @Nullable
    public Ref<EntityStore> getBossRef() {
        return bossRef;
    }

    public float getPendingDamage() {
        return pendingDamage;
    }

    public int getComboHitCount() {
        return comboHitCount;
    }

    public float getSavedDayTime() {
        return savedDayTime;
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
        c.phase = this.phase;
        c.windowTimer = this.windowTimer;
        c.bossRef = this.bossRef;
        c.pendingDamage = this.pendingDamage;
        c.comboHitCount = this.comboHitCount;
        c.savedDayTime = this.savedDayTime;
        return c;
    }
}
