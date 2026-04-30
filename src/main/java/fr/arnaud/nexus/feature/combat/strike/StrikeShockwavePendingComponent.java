package fr.arnaud.nexus.feature.combat.strike;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.util.MessageUtil;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;

public final class StrikeShockwavePendingComponent implements Component<EntityStore> {

    private static final float DELAY_SECONDS = 0.5f;
    public float remainingSeconds = DELAY_SECONDS;

    @Nullable
    private static ComponentType<EntityStore, StrikeShockwavePendingComponent> componentType;

    public StrikeShockwavePendingComponent() {
    }

    @NonNullDecl
    public static ComponentType<EntityStore, StrikeShockwavePendingComponent> getComponentType() {
        if (componentType == null)
            throw new IllegalStateException(MessageUtil.componentNotRegistered(StrikeShockwavePendingComponent.class.getSimpleName()));
        return componentType;
    }

    public static void setComponentType(@NonNullDecl ComponentType<EntityStore, StrikeShockwavePendingComponent> type) {
        componentType = type;
    }

    @Override
    @NonNullDecl
    public StrikeShockwavePendingComponent clone() {
        StrikeShockwavePendingComponent c = new StrikeShockwavePendingComponent();
        c.remainingSeconds = this.remainingSeconds;
        return c;
    }
}
