package fr.arnaud.nexus.input.hover;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3f;
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
import fr.arnaud.nexus.feature.combat.HeadLockComponent;
import fr.arnaud.nexus.input.VoxelTargetResolver;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

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
            if (store.isProcessing()) {
                // Re-queue for next task queue drain, after tick completes
                world.execute(() -> applyHoverUpdate(player, world, ref, store, targetEntity, targetBlock));
                return;
            }

            applyHoverUpdate(player, world, ref, store, targetEntity, targetBlock);
        });
    }

    private void applyHoverUpdate(
        Player player, World world,
        Ref<EntityStore> ref, Store<EntityStore> store,
        Entity targetEntity, Vector3i targetBlock
    ) {
        if (!ref.isValid()) return;

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
            unlockHead(ref, store);
        }

        store.putComponent(ref, PlayerHoverStateComponent.getComponentType(), hoverState);
    }

    private void handleEntityTarget(
        Entity targetEntity,
        PlayerHoverStateComponent hoverState,
        PlayerRef playerRef,
        Ref<EntityStore> ref,
        Store<EntityStore> store
    ) {

        int entityId = targetEntity.getNetworkId();

        HeadLockComponent lock = store.getComponent(ref, HeadLockComponent.getComponentType());
        if (lock != null && lock.getLockedEntityNetworkId() != entityId) {
            lockHeadOnEntity(targetEntity, ref, store, lock);
        } else if (lock != null && !lock.isActive()) {
            lockHeadOnEntity(targetEntity, ref, store, lock);
        }

        if (hoverState.isHighlightingEntity(entityId)) return;
        HoverHighlightDispatcher.highlightEntity(targetEntity, playerRef, ref, store);
        hoverState.recordEntity(entityId);
    }

    private static void lockHeadOnEntity(Entity target, Ref<EntityStore> ref,
                                         Store<EntityStore> store, HeadLockComponent lock) {
        lock.lockOnEntity(new Vector3f(Float.NaN, Float.NaN, Float.NaN), 3f, target.getNetworkId());
        store.putComponent(ref, HeadLockComponent.getComponentType(), lock);
    }

    private static void unlockHead(Ref<EntityStore> ref, Store<EntityStore> store) {
        HeadLockComponent lock = store.getComponent(ref, HeadLockComponent.getComponentType());
        if (lock != null && lock.isActive()) {
            lock.unlock();
            store.putComponent(ref, HeadLockComponent.getComponentType(), lock);
        }
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
}
