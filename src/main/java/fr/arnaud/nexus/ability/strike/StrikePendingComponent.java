package fr.arnaud.nexus.ability.strike;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;

/**
 * Marker added by the interceptor on the world thread to signal StrikeSystem to open the window.
 */
public final class StrikePendingComponent implements Component<EntityStore> {

    @Nullable
    private static ComponentType<EntityStore, StrikePendingComponent> componentType;

    public StrikePendingComponent() {
    }

    @NonNullDecl
    public static ComponentType<EntityStore, StrikePendingComponent> getComponentType() {
        if (componentType == null) throw new IllegalStateException("StrikePendingComponent not registered.");
        return componentType;
    }

    public static void setComponentType(@NonNullDecl ComponentType<EntityStore, StrikePendingComponent> type) {
        componentType = type;
    }

    @Override
    @NonNullDecl
    public StrikePendingComponent clone() {
        return new StrikePendingComponent();
    }
}
