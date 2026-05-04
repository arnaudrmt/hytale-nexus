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
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.ability.core.CoreAbilityRouter;
import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.level.LevelProgressComponent;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public final class PlayerInputListener {

    private final CoreAbilityRouter coreAbilityRouter;

    public PlayerInputListener(@NonNullDecl CoreAbilityRouter coreAbilityRouter) {
        this.coreAbilityRouter = coreAbilityRouter;
    }

    public void onMouseButton(@NonNullDecl PlayerMouseButtonEvent event) {
        MouseButtonType button = event.getMouseButton().mouseButtonType;
        if (button != MouseButtonType.Left && button != MouseButtonType.Right) return;

        Player player = event.getPlayer();
        World world = player.getWorld();
        if (world == null) return;

        Entity targetEntity = event.getTargetEntity();
        Vector3i targetBlock = event.getTargetBlock();

        world.execute(() -> {
            Ref<EntityStore> ref = player.getReference();
            if (ref == null || !ref.isValid()) return;

            Store<EntityStore> store = world.getEntityStore().getStore();

            resolveTarget(ref, store, targetEntity, targetBlock);
            routeInput(button, ref, store, targetBlock);
        });
    }

    private void routeInput(MouseButtonType button,
                            Ref<EntityStore> ref, Store<EntityStore> store,
                            Vector3i targetBlock) {
        if (button == MouseButtonType.Left) {
            if (targetBlock != null && tryChestInteraction(targetBlock, ref, store)) return;

            MovementStatesComponent movStates =
                store.getComponent(ref, MovementStatesComponent.getComponentType());
            if (movStates != null && movStates.getMovementStates().crouching) {
                coreAbilityRouter.tryActivate(ref, store);
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
        LevelProgressComponent progress = store.getComponent(ref, LevelProgressComponent.getComponentType());

        if (progress == null) return false;
        return Nexus.getInstance().getSpawnerManager().tryOpenChest(blockCenter, ref, store, progress);
    }

    private static void resolveTarget(Ref<EntityStore> ref, Store<EntityStore> store,
                                      Entity targetEntity, Vector3i targetBlock) {
        PlayerCursorTargetComponent cursor =
            store.getComponent(ref, PlayerCursorTargetComponent.getComponentType());
        if (cursor == null) return;

        if (targetEntity != null) {
            cursor.resolveEntity(targetEntity);
        } else if (targetBlock != null) {
            cursor.resolveBlock(targetBlock);
        } else {
            cursor.clear();
        }

        store.putComponent(ref, PlayerCursorTargetComponent.getComponentType(), cursor);
    }
}
