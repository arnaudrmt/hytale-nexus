package fr.arnaud.nexus.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;

/**
 * Tracks the player's persistent "Dream Dust" currency balance.
 * <p>
 * This currency persists across play sessions. Death penalties
 * are applied via {@link #applyDeathPenalty()} by the game logic systems.
 */
public final class DreamDustComponent implements Component<EntityStore> {

    /**
     * Fraction of balance lost upon death.
     */
    public static final float DEATH_PENALTY_FRACTION = 0.25f;

    private float amount;

    public DreamDustComponent() {
        this(0f);
    }

    public DreamDustComponent(float initialAmount) {
        this.amount = Math.max(0f, initialAmount);
    }

    // --- Mutations ---

    /**
     * Adds or removes dust from the balance. Clamped to 0.
     */
    public void add(float delta) {
        amount = Math.max(0f, amount + delta);
    }

    /**
     * Applies the death penalty deduction.
     *
     * @return The amount deducted.
     */
    public float applyDeathPenalty() {
        float lost = amount * DEATH_PENALTY_FRACTION;
        amount -= lost;
        return lost;
    }

    // --- Getters ---

    public float getAmount() {
        return amount;
    }

    // --- ECS Boilerplate ---

    @Nullable
    private static ComponentType<EntityStore, DreamDustComponent> componentType;

    @NonNullDecl
    public static ComponentType<EntityStore, DreamDustComponent> getComponentType() {
        if (componentType == null) {
            throw new IllegalStateException("DreamDustComponent not yet registered.");
        }
        return componentType;
    }

    public static void setComponentType(
        @Nullable ComponentType<EntityStore, DreamDustComponent> type) {
        componentType = type;
    }

    @Override
    public DreamDustComponent clone() {
        return new DreamDustComponent(amount);
    }
}
