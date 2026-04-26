package fr.arnaud.nexus.feature.combat.strike;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import javax.annotation.Nullable;

/** Marker written by StrikeWeaponSwapInterceptor to signal a confirmed swap to StrikeSystem. */
public final class StrikeSwapConfirmedComponent implements Component<EntityStore> {

    @Nullable
    private static ComponentType<EntityStore, StrikeSwapConfirmedComponent> componentType;

    public StrikeSwapConfirmedComponent() {}

    @NonNullDecl
    public static ComponentType<EntityStore, StrikeSwapConfirmedComponent> getComponentType() {
        if (componentType == null) throw new IllegalStateException("StrikeSwapConfirmedComponent not registered.");
        return componentType;
    }

    public static void setComponentType(@NonNullDecl ComponentType<EntityStore, StrikeSwapConfirmedComponent> type) {
        componentType = type;
    }

    @Override @NonNullDecl
    public StrikeSwapConfirmedComponent clone() { return new StrikeSwapConfirmedComponent(); }
}
