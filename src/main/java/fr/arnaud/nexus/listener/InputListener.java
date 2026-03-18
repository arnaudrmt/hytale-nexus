package fr.arnaud.nexus.listener;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.MouseButtonType;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.component.CursorTargetComponent;
import fr.arnaud.nexus.system.DashSystem;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

/**
 * Single entry point for all player mouse input.
 * Resolves the cursor target then routes to the appropriate system.
 * Uses direct store writes — no CommandBuffer — since this runs inside
 * {@code world.execute()}, not inside a ticking system.
 * <p>
 * Input routing table:
 * <ul>
 *   <li>Left + crouching  → dash</li>
 *   <li>Right             → weapon swap (second ability); also confirms a pending Switch Strike</li>
 * </ul>
 */
public final class InputListener {

    private final DashSystem dashSystem;

    public InputListener(@NonNullDecl DashSystem dashSystem) {
        this.dashSystem = dashSystem;
    }

    public void onMouseButton(@NonNullDecl PlayerMouseButtonEvent event) {
        MouseButtonType button = event.getMouseButton().mouseButtonType;
        if (button != MouseButtonType.Left && button != MouseButtonType.Right) return;

        Player player = event.getPlayer();
        World world = player.getWorld();
        if (world == null) return;

        Entity targetEntity = event.getTargetEntity();
        Vector3i targetBlock = event.getTargetBlock();
        long clientTime = event.getClientUseTime();

        world.execute(() -> {
            Ref<EntityStore> ref = player.getReference();
            if (ref == null || !ref.isValid()) return;

            Store<EntityStore> store = world.getEntityStore().getStore();
            resolveTarget(ref, store, targetEntity, targetBlock, clientTime);
            routeInput(button, player, ref, store);
        });
    }

    private void routeInput(MouseButtonType button, Player player,
                            Ref<EntityStore> ref, Store<EntityStore> store) {
        MovementStatesComponent movStates = store.getComponent(ref, MovementStatesComponent.getComponentType());
        if (movStates == null) return;

        boolean crouching = movStates.getMovementStates().crouching;

        if (button == MouseButtonType.Left && crouching) {
            dashSystem.tryDash(player, ref, store);
            return;
        }
    }

    private static void resolveTarget(Ref<EntityStore> ref, Store<EntityStore> store,
                                      Entity targetEntity, Vector3i targetBlock, long clientTime) {
        CursorTargetComponent cursor = store.getComponent(ref, CursorTargetComponent.getComponentType());
        if (cursor == null) return;

        if (targetEntity != null) {
            cursor.resolveEntity(targetEntity, clientTime);
        } else if (targetBlock != null) {
            cursor.resolveBlock(targetBlock, clientTime);
        } else {
            cursor.clear();
        }

        store.putComponent(ref, CursorTargetComponent.getComponentType(), cursor);
    }
}
