package fr.arnaud.nexus.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.text.html.parser.Entity;

/**
 * ECS component representing the Flow bar state for an entity (player).
 *
 * GDD refs:
 *  - Base 5 segments, expandable via weapon bonuses (§ Flow Bar / Capacity).
 *  - Accumulation: fills on damage dealt; loss: 1 segment per hit received.
 *  - generationRate: base 1.0x, improved via Refinement / Resonance enchants.
 *  - retentionChance: hidden stat tied to Resilience (chance to NOT lose segment).
 *  - dashCost: fractional drain per dash to prevent spam.
 *
 * Registration:
 *  Nexus#setup() calls getEntityStoreRegistry().registerComponent(FlowComponent.class,
 *  FlowComponent::new) and passes the returned type to setComponentType().
 *  Any class then calls FlowComponent.getComponentType() without a plugin singleton.
 */
public final class FlowComponent implements Component<EntityStore> {

    // -------------------------------------------------------------------------
    // ComponentType — injected once at plugin setup, never null after that
    // -------------------------------------------------------------------------

    @Nullable
    private static ComponentType<EntityStore, FlowComponent> componentType;

    public static @Nonnull ComponentType<EntityStore, FlowComponent> getComponentType() {
        if (componentType == null) {
            throw new IllegalStateException("FlowComponent not yet registered.");
        }
        return componentType;
    }

    public static void setComponentType(@Nonnull ComponentType<EntityStore, FlowComponent> type) {
        componentType = type;
    }

    // -------------------------------------------------------------------------
    // Constants (GDD-derived)
    // -------------------------------------------------------------------------

    public static final int BASE_MAX_SEGMENTS = 5;
    public static final int ABSOLUTE_MAX_SEGMENTS = 8;     // HUD cap: 5-8 bars
    public static final float BASE_GENERATION_RATE = 1.0f;
    public static final float BASE_RETENTION_CHANCE = 0.0f;
    public static final float DASH_FLOW_COST = 0.05f;     // 5% of one segment per dash

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    /** Current fill value in segment units (0.0 → maxSegments). */
    private float current;

    /** Maximum segments: base + weapon bonuses. Clamped to ABSOLUTE_MAX_SEGMENTS. */
    private int maxSegments;

    /**
     * Multiplier applied to all Flow generation events.
     * 1.0 = normal; 1.5 = 50% faster fill.
     */
    private float generationRate;

    /**
     * Probability [0.0, 1.0] to negate a segment loss when hit.
     * Derived from the sum of equipped weapons' Resilience stats.
     */
    private float retentionChance;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public FlowComponent() {
        this.current         = 0.0f;
        this.maxSegments     = BASE_MAX_SEGMENTS;
        this.generationRate  = BASE_GENERATION_RATE;
        this.retentionChance = BASE_RETENTION_CHANCE;
    }

    private FlowComponent(float current, int maxSegments,
                          float generationRate, float retentionChance) {
        this.current         = current;
        this.maxSegments     = maxSegments;
        this.generationRate  = generationRate;
        this.retentionChance = retentionChance;
    }

    // -------------------------------------------------------------------------
    // Mutations
    // -------------------------------------------------------------------------

    /**
     * Adds {@code amount} (scaled by generationRate) to the current Flow.
     * Clamps to maxSegments.
     *
     * @return true if Flow just reached maximum (Switch Strike trigger).
     */
    public boolean addFlow(float amount) {
        boolean wasFull = isFull();
        current = Math.min(current + amount * generationRate, maxSegments);
        return !wasFull && isFull();
    }

    /**
     * Removes {@code segments} full segments from current Flow.
     * Applies retention chance roll: if roll < retentionChance, loss is negated.
     *
     * @param segments number of segments to remove.
     * @param applyRetention whether to allow retention roll (false for ability costs).
     * @return actual segments lost after retention check.
     */
    public int removeSegments(int segments, boolean applyRetention) {
        if (applyRetention && retentionChance > 0f && Math.random() < retentionChance) {
            return 0;
        }
        int lost = Math.min(segments, getFilledSegments());
        current = Math.max(0f, current - lost);
        return lost;
    }

    /**
     * Drains a fractional portion of one segment (e.g. dash cost).
     */
    public void drainFractional(float amount) {
        current = Math.max(0f, current - amount);
    }

    /** Fully empties the Flow bar (e.g. Switch Strike execution). */
    public void drain() {
        current = 0.0f;
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    public boolean isFull() {
        return current >= maxSegments;
    }

    public boolean isEmpty() {
        return current <= 0f;
    }

    /** Returns the number of fully-filled segments (for enchantment threshold checks). */
    public int getFilledSegments() {
        return (int) current;
    }

    public float getCurrent() { return current; }
    public int getMaxSegments() { return maxSegments; }
    public float getGenerationRate() { return generationRate; }
    public float getRetentionChance() { return retentionChance; }

    // -------------------------------------------------------------------------
    // Setters (called by WeaponSlotComponent recalculation)
    // -------------------------------------------------------------------------

    public void setMaxSegments(int max) {
        this.maxSegments = Math.min(max, ABSOLUTE_MAX_SEGMENTS);
        this.current = Math.min(current, this.maxSegments);
    }

    public void setGenerationRate(float rate) {
        this.generationRate = Math.max(0.1f, rate);
    }

    public void setRetentionChance(float chance) {
        this.retentionChance = Math.max(0f, Math.min(1f, chance));
    }

    // -------------------------------------------------------------------------
    // ECS contract
    // -------------------------------------------------------------------------

    @Override
    public @Nullable FlowComponent clone() {
        return new FlowComponent(current, maxSegments, generationRate, retentionChance);
    }
}
