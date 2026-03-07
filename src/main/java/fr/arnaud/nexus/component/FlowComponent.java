package fr.arnaud.nexus.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Tracks the Flow bar state for an entity (player).
 * <p>
 * Flow is used as a resource for weapon abilities and dashes. It accumulates
 * via combat actions and is depleted by ability usage or damage received.
 */
public final class FlowComponent implements Component<EntityStore> {


    public static final int BASE_MAX_SEGMENTS = 5;
    public static final int ABSOLUTE_MAX_SEGMENTS = 8;
    public static final float BASE_GENERATION_RATE = 1.0f;
    public static final float BASE_RETENTION_CHANCE = 0.0f;
    public static final float DASH_FLOW_COST = 0.05f;

    private float current;
    private int maxSegments;
    private float generationRate;
    private float retentionChance;

    public FlowComponent() {
        this.current = 0.0f;
        this.maxSegments = BASE_MAX_SEGMENTS;
        this.generationRate = BASE_GENERATION_RATE;
        this.retentionChance = BASE_RETENTION_CHANCE;
    }

    private FlowComponent(float current, int maxSegments,
                          float generationRate, float retentionChance) {
        this.current = current;
        this.maxSegments = maxSegments;
        this.generationRate = generationRate;
        this.retentionChance = retentionChance;
    }

    // --- Mutations ---

    /**
     * Adds flow, scaled by {@code generationRate}.
     *
     * @return true if the flow bar reached maximum capacity.
     */
    public boolean addFlow(float amount) {
        boolean wasFull = isFull();
        current = Math.min(current + amount * generationRate, maxSegments);
        return !wasFull && isFull();
    }

    /**
     * Removes full segments from current flow, respecting retention chance.
     *
     * @return The number of segments actually lost.
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

    public void drain() {
        current = 0.0f;
    }

    // --- Queries & Accessors ---

    public boolean isFull() {
        return current >= maxSegments;
    }

    public boolean isEmpty() {
        return current <= 0f;
    }

    public int getFilledSegments() {
        return (int) current;
    }


    public float getCurrent() {
        return current;
    }

    public int getMaxSegments() {
        return maxSegments;
    }

    public float getGenerationRate() {
        return generationRate;
    }

    public float getRetentionChance() {
        return retentionChance;
    }

    // --- Setters ---

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

    // --- ECS Boilerplate ---

    @Nullable
    private static ComponentType<EntityStore, FlowComponent> componentType;

    @NonNullDecl
    public static ComponentType<EntityStore, FlowComponent> getComponentType() {
        if (componentType == null) {
            throw new IllegalStateException("FlowComponent not yet registered.");
        }
        return componentType;
    }

    public static void setComponentType(@Nonnull ComponentType<EntityStore, FlowComponent> type) {
        componentType = type;
    }

    @Override
    @NonNullDecl
    public FlowComponent clone() {
        return new FlowComponent(current, maxSegments, generationRate, retentionChance);
    }
}
