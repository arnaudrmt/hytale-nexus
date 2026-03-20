package fr.arnaud.nexus.breach;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;

/**
 * Tracks the damage combo accumulation during an active Breach sequence.
 * <p>
 * Each hit on the frozen boss is intercepted — the raw damage is negated
 * immediately and stored here with a multiplier that grows with each hit.
 * On sequence exit the total accumulated damage is applied to the boss in
 * a single burst.
 * <p>
 * Ephemeral — meaningless across sessions.
 */
public final class BreachComboComponent implements Component<EntityStore> {

    @Nullable
    private static ComponentType<EntityStore, BreachComboComponent> componentType;

    private int hitCount = 0;
    private float accumulatedDamage = 0f;
    private boolean active = false;

    public BreachComboComponent() {
    }

    public void beginCombo() {
        hitCount = 0;
        accumulatedDamage = 0f;
        active = true;
    }

    /**
     * Records one intercepted hit. The damage contributed is {@code rawDamage * (hitCount + 1)}
     * so each successive hit multiplies by its ordinal position in the combo.
     * Returns the multiplied value actually added.
     */
    public float recordHit(float rawDamage) {
        if (!active) return 0f;
        hitCount++;
        float multiplied = rawDamage * hitCount;
        accumulatedDamage += multiplied;
        return multiplied;
    }

    /**
     * Finalises the combo and returns the total damage to apply to the boss.
     * Resets state so the component is clean for the next sequence.
     */
    public float consumeAccumulatedDamage() {
        float total = accumulatedDamage;
        hitCount = 0;
        accumulatedDamage = 0f;
        active = false;
        return total;
    }

    public boolean isActive() {
        return active;
    }

    public int getHitCount() {
        return hitCount;
    }

    public float getAccumulatedDamage() {
        return accumulatedDamage;
    }

    @NonNullDecl
    public static ComponentType<EntityStore, BreachComboComponent> getComponentType() {
        if (componentType == null) throw new IllegalStateException("BreachComboComponent not registered.");
        return componentType;
    }

    public static void setComponentType(
        @NonNullDecl ComponentType<EntityStore, BreachComboComponent> type) {
        componentType = type;
    }

    @Override
    @NonNullDecl
    public BreachComboComponent clone() {
        BreachComboComponent c = new BreachComboComponent();
        c.hitCount = this.hitCount;
        c.accumulatedDamage = this.accumulatedDamage;
        c.active = this.active;
        return c;
    }
}
