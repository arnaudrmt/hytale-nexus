package fr.arnaud.nexus.input;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;

public final class PlayerCursorTargetComponent implements Component<EntityStore> {

    @Nullable
    private Entity targetEntity;
    @Nullable
    private Vector3i targetBlock;

    @Nullable
    private static ComponentType<EntityStore, PlayerCursorTargetComponent> componentType;

    public PlayerCursorTargetComponent() {
    }

    public void resolveEntity(@NonNullDecl Entity entity) {
        this.targetEntity = entity;
        this.targetBlock = null;
    }

    public void resolveBlock(@NonNullDecl Vector3i block) {
        this.targetEntity = null;
        this.targetBlock = block;
    }

    public void clear() {
        this.targetEntity = null;
        this.targetBlock = null;
    }

    public boolean hasEntityTarget() {
        return targetEntity != null;
    }

    public boolean hasBlockTarget() {
        return targetBlock != null;
    }

    public boolean isEmpty() {
        return targetEntity == null && targetBlock == null;
    }

    @Nullable
    public Entity getTargetEntity() {
        return targetEntity;
    }

    @Nullable
    public Vector3i getTargetBlock() {
        return targetBlock;
    }

    @NonNullDecl
    public static ComponentType<EntityStore, PlayerCursorTargetComponent> getComponentType() {
        if (componentType == null) throw new IllegalStateException("CursorTargetComponent not registered.");
        return componentType;
    }

    public static void setComponentType(@Nullable ComponentType<EntityStore, PlayerCursorTargetComponent> type) {
        componentType = type;
    }

    @Override
    @NonNullDecl
    public PlayerCursorTargetComponent clone() {
        PlayerCursorTargetComponent c = new PlayerCursorTargetComponent();
        c.targetEntity = this.targetEntity;
        c.targetBlock = this.targetBlock;
        return c;
    }
}
