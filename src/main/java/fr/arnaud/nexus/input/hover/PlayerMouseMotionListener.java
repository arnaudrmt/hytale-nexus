package fr.arnaud.nexus.input.hover;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseMotionEvent;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.camera.PlayerCameraComponent;
import fr.arnaud.nexus.input.VoxelTargetResolver;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;

public final class PlayerMouseMotionListener {

    private final VoxelTargetResolver targetResolver;

    public PlayerMouseMotionListener(@NonNullDecl VoxelTargetResolver targetResolver) {
        this.targetResolver = targetResolver;
    }

    public void onMouseMotion(@NonNullDecl PlayerMouseMotionEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        if (world == null) return;

        Entity targetEntity = event.getTargetEntity();
        Vector3i targetBlock = event.getTargetBlock();

        world.execute(() -> {
            Ref<EntityStore> ref = player.getReference();
            if (ref == null || !ref.isValid()) return;

            Store<EntityStore> store = world.getEntityStore().getStore();

            PlayerHoverStateComponent hoverState = store.getComponent(ref, PlayerHoverStateComponent.getComponentType());
            if (hoverState == null) return;

            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef == null) return;

            if (targetEntity != null) {
                handleEntityTarget(targetEntity, hoverState, playerRef, ref, store);
            } else if (targetBlock != null) {
                TransformComponent transform = store.getComponent(ref, EntityModule.get().getTransformComponentType());
                PlayerCameraComponent cam = store.getComponent(ref, PlayerCameraComponent.getComponentType());
                if (transform == null) return;
                float camDistance = cam != null ? cam.getEffectiveIsoDistance() : PlayerCameraComponent.ISO_DISTANCE;
                Vector3i resolved = targetResolver.resolve(transform.getPosition(), targetBlock, camDistance, world);
                if (resolved != null) handleBlockTarget(resolved, hoverState, playerRef);
            } else {
                hoverState.clear();
            }

            store.putComponent(ref, PlayerHoverStateComponent.getComponentType(), hoverState);
        });
    }

    private void handleEntityTarget(
        @NonNullDecl Entity targetEntity,
        @NonNullDecl PlayerHoverStateComponent hoverState,
        @NonNullDecl PlayerRef playerRef,
        @NonNullDecl Ref<EntityStore> ref,
        @NonNullDecl Store<EntityStore> store
    ) {
        String entityTypeId = resolveEntityTypeId(targetEntity, store);
        if (entityTypeId == null || !CursorHoverAllowlist.isInteractableEntity(entityTypeId)) {
            hoverState.clear();
            return;
        }

        int entityId = targetEntity.getNetworkId();
        if (hoverState.isHighlightingEntity(entityId)) return;

        HoverHighlightDispatcher.highlightEntity(targetEntity, playerRef, ref, store);
        hoverState.recordEntity(entityId);
    }

    private void handleBlockTarget(
        @NonNullDecl Vector3i block,
        @NonNullDecl PlayerHoverStateComponent hoverState,
        @NonNullDecl PlayerRef playerRef
    ) {
        if (hoverState.isHighlightingBlock(block)) return;

        HoverHighlightDispatcher.highlightBlock(block, playerRef);
        hoverState.recordBlock(block);
    }

    @Nullable
    private String resolveEntityTypeId(
        @NonNullDecl Entity targetEntity,
        @NonNullDecl Store<EntityStore> store
    ) {
        // TODO: replace with the correct NPC role/entity type lookup once confirmed.
        return null;
    }
}
