package fr.arnaud.nexus.input.hover;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;

/**
 * Tracks the last target for which a hover highlight packet was sent.
 * <p>
 * Used to diff against the current frame's resolved target so that packets
 * are only dispatched when the highlighted target actually changes — preventing
 * per-frame packet spam on {@code PlayerMouseMotionEvent}.
 * <p>
 * Exactly one of {@code highlightedBlock} or {@code highlightedEntityId} is set
 * at any time; never both. A fully cleared state means no highlight is active.
 */
public final class PlayerHoverStateComponent implements Component<EntityStore> {

    @Nullable
    private Vector3i highlightedBlock;
    private int highlightedEntityId = -1;

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

    // ── ECS boilerplate ──────────────────────────────────────────────────────

    @Nullable
    private static ComponentType<EntityStore, PlayerHoverStateComponent> componentType;

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
