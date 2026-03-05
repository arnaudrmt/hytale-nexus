package fr.arnaud.nexus.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;

/**
 * Zero-data marker component — signals that an entity has triggered "The Awakening"
 * (run-end condition: lucidity reached zero).
 *
 * Usage pattern:
 *   commandBuffer.addComponent(ref, AwakeningMarkerComponent.getComponentType(),
 *                              new AwakeningMarkerComponent());
 *
 * A dedicated AwakeningSystem (future) processes all entities carrying this marker
 * each tick: fires the end-of-run sequence and removes the marker.
 *
 * GDD ref (§ Fiche Joueur / La Mort — Le Réveil):
 *   Run ends, loot lost, Lucidity score displayed.
 */
public final class AwakeningMarkerComponent implements Component<EntityStore> {

    @Nullable
    private static ComponentType<EntityStore, AwakeningMarkerComponent> componentType;

    public static @NonNullDecl ComponentType<EntityStore, AwakeningMarkerComponent> getComponentType() {
        if (componentType == null) {
            throw new IllegalStateException("AwakeningMarkerComponent not yet registered.");
        }
        return componentType;
    }

    public static void setComponentType(
            @Nullable ComponentType<EntityStore, AwakeningMarkerComponent> type) {
        componentType = type;
    }

    public AwakeningMarkerComponent() {}

    @Override
    public @Nullable AwakeningMarkerComponent clone() {
        return new AwakeningMarkerComponent();
    }
}
