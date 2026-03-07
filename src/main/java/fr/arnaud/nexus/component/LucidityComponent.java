package fr.arnaud.nexus.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;

/**
 * Tracks a player's Lucidity, a survival metric that determines run status.
 * <p>
 * When Lucidity reaches zero, the player triggers "The Awakening."
 * Lucidity also acts as a multiplier for the final run score.
 */
public final class LucidityComponent implements Component<EntityStore> {

    public static final float DEFAULT_MAX = 100f;

    /**
     * Drain rate per second when a co-op partner is in Unstable Sleep.
     */
    public static final float COOP_DRAIN_RATE_PER_SECOND = 2.5f;

    private float current;
    private float max;
    private boolean unstableSleep;

    public LucidityComponent() {
        this.current = DEFAULT_MAX;
        this.max = DEFAULT_MAX;
        this.unstableSleep = false;
    }

    private LucidityComponent(float current, float max, boolean unstableSleep) {
        this.current = current;
        this.max = max;
        this.unstableSleep = unstableSleep;
    }

    // --- Mutations ---

    /**
     * @return amount actually gained, clamped to max.
     */
    public float gain(float amount) {
        float before = current;
        current = Math.min(current + amount, max);
        return current - before;
    }

    /**
     * Reduces lucidity.
     *
     * @return true if lucidity reached zero.
     */
    public boolean drain(float amount) {
        current = Math.max(0f, current - amount);
        return current <= 0f;
    }

    public void setUnstableSleep(boolean state) {
        this.unstableSleep = state;
    }

    // --- Queries & Accessors ---

    public boolean isDepleted() {
        return current <= 0f;
    }

    public boolean isUnstableSleep() {
        return unstableSleep;
    }

    public float getCurrent() {
        return current;
    }

    public float getMax() {
        return max;
    }

    /**
     * @return normalized value [0.0, 1.0] for HUD rendering.
     */
    public float getNormalized() {
        return max > 0 ? current / max : 0f;
    }

    /**
     * Calculates the score multiplier: linearly scales 1.0x to 3.0x
     * based on current lucidity.
     */
    public float getScoreMultiplier() {
        return 1.0f + (getNormalized() * 2.0f);
    }

    // --- ECS Boilerplate ---

    @Nullable
    private static ComponentType<EntityStore, LucidityComponent> componentType;

    @NonNullDecl
    public static ComponentType<EntityStore, LucidityComponent> getComponentType() {
        if (componentType == null) {
            throw new IllegalStateException("LucidityComponent not yet registered.");
        }
        return componentType;
    }

    public static void setComponentType(@Nullable ComponentType<EntityStore, LucidityComponent> type) {
        componentType = type;
    }

    @Override
    @NonNullDecl
    public LucidityComponent clone() {
        return new LucidityComponent(current, max, unstableSleep);
    }
}
