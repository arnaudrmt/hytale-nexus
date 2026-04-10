package fr.arnaud.nexus.feature.breach;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;

/**
 * Ephemeral marker placed on a mini-boss when it enters the Breach sequence.
 * <p>
 * Presence signals {@link BreachDamageInterceptor} to cancel incoming damage
 * and fold it into the attacker's combo accumulator instead.
 * Removed by {@link BreachSequenceSystem} when the sequence exits.
 */
public final class FrozenTargetComponent implements Component<EntityStore> {

    @Nullable
    private static ComponentType<EntityStore, FrozenTargetComponent> componentType;

    public FrozenTargetComponent() {
    }

    @NonNullDecl
    public static ComponentType<EntityStore, FrozenTargetComponent> getComponentType() {
        if (componentType == null) throw new IllegalStateException("FrozenComponent not registered.");
        return componentType;
    }

    public static void setComponentType(@NonNullDecl ComponentType<EntityStore, FrozenTargetComponent> type) {
        componentType = type;
    }

    @Override
    @NonNullDecl
    public FrozenTargetComponent clone() {
        return new FrozenTargetComponent();
    }
}
