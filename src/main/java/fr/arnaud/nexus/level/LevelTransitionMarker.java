package fr.arnaud.nexus.level;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.Nullable;

public final class LevelTransitionMarker implements Component<EntityStore> {

    private static ComponentType<EntityStore, LevelTransitionMarker> componentType;

    public static void setComponentType(ComponentType<EntityStore, LevelTransitionMarker> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, LevelTransitionMarker> getComponentType() {
        if (componentType == null) throw new IllegalStateException("LevelTransitionMarker not registered.");
        return componentType;
    }

    @Nullable
    @Override
    public Component<EntityStore> clone() {
        return new LevelTransitionMarker();
    }
}
