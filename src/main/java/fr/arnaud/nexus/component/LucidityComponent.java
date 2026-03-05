package fr.arnaud.nexus.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;

/**
 * ECS component representing a player's Lucidity (Oniric Stability / survival bar).
 *
 * GDD refs:
 *  - Replaces standard health portrait (§ Fiche Joueur / Condition de Survie).
 *  - Zero → "The Awakening" (run ends, loot lost, score displayed).
 *  - Gained by killing mobs and purifying Gaia Totems.
 *  - Coop: shared collective bar is managed externally in CoopSessionData.
 *  - Higher lucidity = higher final score multiplier.
 *
 * NOTE: The native Hytale EntityStatMap / DefaultEntityStatTypes#getHealth() still
 * drives actual damage calculation; lucidity is an additional mod layer on top.
 */
public final class LucidityComponent implements Component<EntityStore> {

    // -------------------------------------------------------------------------
    // ComponentType — injected once at plugin setup
    // -------------------------------------------------------------------------

    @Nullable
    private static ComponentType<EntityStore, LucidityComponent> componentType;

    public static @NonNullDecl ComponentType<EntityStore, LucidityComponent> getComponentType() {
        if (componentType == null) {
            throw new IllegalStateException("LucidityComponent not yet registered.");
        }
        return componentType;
    }

    public static void setComponentType(@Nullable ComponentType<EntityStore, LucidityComponent> type) {
        componentType = type;
    }

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    public static final float DEFAULT_MAX = 100f;

    /** Drain rate per second when a co-op partner is in Unstable Sleep. */
    public static final float COOP_DRAIN_RATE_PER_SECOND = 2.5f;

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private float current;
    private float max;

    /** Whether this player is currently in Unstable Sleep (co-op only). */
    private boolean unstableSleep;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public LucidityComponent() {
        this.current      = DEFAULT_MAX;
        this.max          = DEFAULT_MAX;
        this.unstableSleep = false;
    }

    private LucidityComponent(float current, float max, boolean unstableSleep) {
        this.current      = current;
        this.max          = max;
        this.unstableSleep = unstableSleep;
    }

    // -------------------------------------------------------------------------
    // Mutations
    // -------------------------------------------------------------------------

    /**
     * Adds lucidity. Clamps to max.
     *
     * @return amount actually gained.
     */
    public float gain(float amount) {
        float before = current;
        current = Math.min(current + amount, max);
        return current - before;
    }

    /**
     * Reduces lucidity by {@code amount}.
     *
     * @return true if lucidity just reached zero ("The Awakening").
     */
    public boolean drain(float amount) {
        current = Math.max(0f, current - amount);
        return current <= 0f;
    }

    public void setUnstableSleep(boolean state) {
        this.unstableSleep = state;
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    public boolean isDepleted() { return current <= 0f; }
    public boolean isUnstableSleep() { return unstableSleep; }
    public float getCurrent() { return current; }
    public float getMax() { return max; }

    /** Returns lucidity as a [0.0, 1.0] fraction for HUD rendering. */
    public float getNormalized() { return max > 0 ? current / max : 0f; }

    /**
     * Score multiplier: linearly scales from 1.0x at 0 lucidity to 3.0x at max.
     * Higher lucidity → better final score (GDD: "Plus la Lucidité est haute...").
     */
    public float getScoreMultiplier() {
        return 1.0f + (getNormalized() * 2.0f);
    }

    // -------------------------------------------------------------------------
    // ECS contract
    // -------------------------------------------------------------------------


    @NonNullDecl
    @Override
    public LucidityComponent clone() {
        return new LucidityComponent(current, max, unstableSleep);
    }
}
