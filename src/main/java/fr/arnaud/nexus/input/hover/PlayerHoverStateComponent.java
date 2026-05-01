package fr.arnaud.nexus.input.hover;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;

public final class PlayerHoverStateComponent implements Component<EntityStore> {

    @Nullable
    private Vector3i highlightedBlock;
    private int highlightedEntityId = -1;

    @Nullable
    private static ComponentType<EntityStore, PlayerHoverStateComponent> componentType;

    public PlayerHoverStateComponent() {
    }

    public void recordBlock(@NonNullDecl Vector3i block) {
        this.highlightedBlock = block;
        this.highlightedEntityId = -1;
    }

    public void recordEntity(int entityId) {
        this.highlightedBlock = null;
        this.highlightedEntityId = entityId;
    }

    public void clear() {
        this.highlightedBlock = null;
        this.highlightedEntityId = -1;
    }

    public boolean isHighlightingBlock(@NonNullDecl Vector3i block) {
        return block.equals(this.highlightedBlock);
    }

    public boolean isHighlightingEntity(int entityId) {
        return this.highlightedEntityId == entityId;
    }

    public boolean isEmpty() {
        return highlightedBlock == null && highlightedEntityId == -1;
    }

    @NonNullDecl
    public static ComponentType<EntityStore, PlayerHoverStateComponent> getComponentType() {
        if (componentType == null) throw new IllegalStateException("PlayerHoverStateComponent not registered.");
        return componentType;
    }

    public static void setComponentType(@Nullable ComponentType<EntityStore, PlayerHoverStateComponent> type) {
        componentType = type;
    }

    @Override
    @NonNullDecl
    public PlayerHoverStateComponent clone() {
        PlayerHoverStateComponent c = new PlayerHoverStateComponent();
        c.highlightedBlock = this.highlightedBlock;
        c.highlightedEntityId = this.highlightedEntityId;
        return c;
    }
}
