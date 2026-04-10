package fr.arnaud.nexus.input;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.MouseButtonType;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.camera.PlayerCameraComponent;
import fr.arnaud.nexus.camera.PlayerOcclusionComponent;
import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.feature.movement.PlayerDashSystem;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;

/**
 * Single entry point for all player mouse input.
 * Resolves the cursor target then routes to the appropriate system.
 * Uses direct store writes — no CommandBuffer — since this runs inside
 * {@code world.execute()}, not inside a ticking system.
 * <p>
 * Input routing table:
 * <ul>
 *   <li>Left + crouching      → dash</li>
 *   <li>Left + block target   → chest interaction (if applicable)</li>
 *   <li>Right                 → weapon swap / Switch Strike confirm</li>
 * </ul>
 */
public final class PlayerInputListener {

    private final PlayerDashSystem dashSystem;
    private final VoxelTargetResolver targetResolver;

    public PlayerInputListener(@NonNullDecl PlayerDashSystem dashSystem,
                               @NonNullDecl VoxelTargetResolver targetResolver) {
        this.dashSystem = dashSystem;
        this.targetResolver = targetResolver;
    }

    public void onMouseButton(@NonNullDecl PlayerMouseButtonEvent event) {
        MouseButtonType button = event.getMouseButton().mouseButtonType;
        if (button != MouseButtonType.Left && button != MouseButtonType.Right) return;

        Player player = event.getPlayer();
        World world = player.getWorld();
        if (world == null) return;

        Entity targetEntity = event.getTargetEntity();
        Vector3i rawTargetBlock = event.getTargetBlock();
        long clientTime = event.getClientUseTime();

        world.execute(() -> {
            Ref<EntityStore> ref = player.getReference();
            if (ref == null || !ref.isValid()) return;

            Store<EntityStore> store = world.getEntityStore().getStore();

            Vector3i targetBlock = resolveBlockTarget(ref, store, rawTargetBlock, world);

            resolveTarget(ref, store, targetEntity, targetBlock, clientTime);
            routeInput(button, player, ref, store, targetBlock);
        });
    }

    @Nullable
    private Vector3i resolveBlockTarget(@NonNullDecl Ref<EntityStore> ref,
                                        @NonNullDecl Store<EntityStore> store,
                                        @Nullable Vector3i rawTarget,
                                        @NonNullDecl World world) {
        if (rawTarget == null) return null;
        TransformComponent transform = store.getComponent(ref, EntityModule.get().getTransformComponentType());
        if (transform == null) return null;
        Vector3d playerFeet = transform.getPosition();
        if (playerFeet == null) return null;

        PlayerCameraComponent cam = store.getComponent(ref, PlayerCameraComponent.getComponentType());
        float camDistance = cam != null ? cam.getEffectiveIsoDistance() : PlayerCameraComponent.ISO_DISTANCE;
        PlayerOcclusionComponent occlusion = store.getComponent(ref, PlayerOcclusionComponent.getComponentType());

        return targetResolver.resolve(playerFeet, rawTarget, camDistance, occlusion, world);
    }

    private void routeInput(MouseButtonType button, Player player,
                            Ref<EntityStore> ref, Store<EntityStore> store,
                            Vector3i targetBlock) {
        if (button == MouseButtonType.Left) {
            if (targetBlock != null && tryChestInteraction(targetBlock, ref, store)) return;

            MovementStatesComponent movStates = store.getComponent(ref, MovementStatesComponent.getComponentType());
            if (movStates != null && movStates.getMovementStates().crouching) {
                dashSystem.tryDash(player, ref, store);
            }
        }
    }

    private boolean tryChestInteraction(Vector3i targetBlock, Ref<EntityStore> ref,
                                        Store<EntityStore> store) {
        Vector3d blockCenter = new Vector3d(
            targetBlock.getX() + 0.5,
            targetBlock.getY() + 0.5,
            targetBlock.getZ() + 0.5
        );
        return Nexus.get().getMobSpawnerManager().tryOpenChest(blockCenter, ref, store);
    }

    private static void resolveTarget(Ref<EntityStore> ref, Store<EntityStore> store,
                                      Entity targetEntity, Vector3i targetBlock, long clientTime) {
        PlayerCursorTargetComponent cursor = store.getComponent(ref, PlayerCursorTargetComponent.getComponentType());
        if (cursor == null) return;

        if (targetEntity != null) {
            cursor.resolveEntity(targetEntity, clientTime);
        } else if (targetBlock != null) {
            cursor.resolveBlock(targetBlock, clientTime);
        } else {
            cursor.clear();
        }

        store.putComponent(ref, PlayerCursorTargetComponent.getComponentType(), cursor);
    }
}
