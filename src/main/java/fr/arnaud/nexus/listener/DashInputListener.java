package fr.arnaud.nexus.listener;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.MouseButtonState;
import com.hypixel.hytale.protocol.MouseButtonType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.component.FlowComponent;
import fr.arnaud.nexus.component.PlayerBodyComponent;
import fr.arnaud.nexus.component.PlayerBodyComponent.DashState;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

/**
 * Listener that triggers a player dash on Sneak + Left Click.
 */
public final class DashInputListener {

    private DashInputListener() {
    }

    /**
     * Triggered on mouse button events to detect dash input.
     */
    public static void onMouseButton(@NonNullDecl PlayerMouseButtonEvent event) {
        var btn = event.getMouseButton();
        if (btn.mouseButtonType != MouseButtonType.Left) return;
        if (btn.state != MouseButtonState.Pressed) return;

        Player player = event.getPlayer();
        World world = player.getWorld();
        if (world == null) return;

        world.execute(() -> {
            Ref<EntityStore> ref = player.getReference();
            Store<EntityStore> store = world.getEntityStore().getStore();
            if (ref == null || !ref.isValid()) return;

            // Only dash if sneaking (crouching)
            MovementStatesComponent movStates =
                store.getComponent(ref, MovementStatesComponent.getComponentType());
            if (movStates == null || !movStates.getMovementStates().crouching) return;

            requestDashDirect(ref, store);
        });
    }

    /**
     * Executes dash logic outside of a ticking system using direct storage access.
     */
    private static void requestDashDirect(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store) {

        PlayerBodyComponent body = store.getComponent(ref, PlayerBodyComponent.getComponentType());
        FlowComponent flow = store.getComponent(ref, FlowComponent.getComponentType());

        if (body == null || flow == null || body.getDashState() != DashState.IDLE) return;

        flow.drainFractional(FlowComponent.DASH_FLOW_COST);
        if (!body.beginDash()) return;

        // Reward successful perfect-dodge
        if (body.consumePerfectDodge()) {
            flow.addFlow(1.0f);
        }

        store.putComponent(ref, PlayerBodyComponent.getComponentType(), body);
        store.putComponent(ref, FlowComponent.getComponentType(), flow);
    }
}
