package fr.arnaud.nexus.input.hover;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
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
import fr.arnaud.nexus.camera.PlayerOcclusionComponent;
import fr.arnaud.nexus.input.VoxelTargetResolver;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;

/**
 * Handles {@link PlayerMouseMotionEvent} to display cursor hover highlights.
 * <p>
 * On each motion event:
 * 1. Resolves the true target block via {@link VoxelTargetResolver} (same recast
 * logic used by {@code PlayerInputListener}).
 * 2. Checks whether the resolved target is on the {@link CursorHoverAllowlist}.
 * 3. Diffs against {@link PlayerHoverStateComponent} — only sends a highlight
 * packet when the active target has changed, avoiding per-frame spam.
 * <p>
 * Particle lifetime is intentionally short (defined in the Asset Editor).
 * No explicit "clear" packet exists for particles; the highlight simply expires
 * when the cursor moves away and no new packet is sent.
 * <p>
 * Entity targets take priority over block targets, matching the engine's own
 * targeting behaviour.
 */
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
        Vector3i rawTargetBlock = event.getTargetBlock();

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
            } else if (rawTargetBlock != null) {
                Vector3i resolvedBlock = resolveBlock(ref, store, rawTargetBlock, world);
                if (resolvedBlock != null) {
                    handleBlockTarget(resolvedBlock, hoverState, playerRef);
                } else {
                    hoverState.clear();
                }
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
    private Vector3i resolveBlock(
        @NonNullDecl Ref<EntityStore> ref,
        @NonNullDecl Store<EntityStore> store,
        @NonNullDecl Vector3i rawTarget,
        @NonNullDecl World world
    ) {
        TransformComponent transform = store.getComponent(
            ref, EntityModule.get().getTransformComponentType()
        );
        if (transform == null) return null;
        Vector3d playerFeet = transform.getPosition();
        if (playerFeet == null) return null;

        PlayerCameraComponent cam = store.getComponent(ref, PlayerCameraComponent.getComponentType());
        float camDistance = cam != null ? cam.getEffectiveIsoDistance() : PlayerCameraComponent.ISO_DISTANCE;
        PlayerOcclusionComponent occlusion = store.getComponent(ref, PlayerOcclusionComponent.getComponentType());

        return targetResolver.resolve(playerFeet, rawTarget, camDistance, occlusion, world);
    }

    /**
     * Resolves the block's asset ID by querying the world's block data at the
     * given position. Returns null if the block cannot be identified.
     * <p>
     * TODO: replace with the correct BlockType lookup API once confirmed.
     */
    @Nullable
    private String resolveBlockAssetId(@NonNullDecl Vector3i block) {
        // Placeholder — wire up the engine's block asset lookup here.
        // Example shape: world.getBlockType(block.getX(), block.getY(), block.getZ()).getAssetId()
        return null;
    }

    /**
     * Resolves the entity's type ID from its ECS components.
     * Returns null if the type cannot be determined.
     * <p>
     * TODO: replace with the correct NPC role/entity type lookup once confirmed.
     */
    @Nullable
    private String resolveEntityTypeId(
        @NonNullDecl Entity targetEntity,
        @NonNullDecl Store<EntityStore> store
    ) {
        // Placeholder — wire up the engine's entity type lookup here.
        // Example shape: store.getComponent(targetEntity.getRef(), NpcRoleComponent.getComponentType()).getRoleId()
        return null;
    }
}
