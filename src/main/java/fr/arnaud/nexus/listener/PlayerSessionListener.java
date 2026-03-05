package fr.arnaud.nexus.listener;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.component.FlowComponent;
import fr.arnaud.nexus.component.LucidityComponent;
import fr.arnaud.nexus.component.WeaponSlotComponent;
import fr.arnaud.nexus.i18n.I18n;

/**
 * Bootstraps all Nexus ECS components onto a player entity when they enter a world.
 *
 * API notes:
 *   - event.getPlayer()              → Player component (display name, messaging)
 *   - event.getPlayerRef()           → PlayerRef component
 *   - playerRef.getReference()       → Ref<EntityStore>
 *   - store.hasComponent(ref, type)  → guards against double-init on world switch
 *   - store.putComponent(ref, type, instance) → persistent component add
 */
public final class PlayerSessionListener {

    private PlayerSessionListener() {}

    public static void onPlayerReady(PlayerReadyEvent event) {
        Player    player    = event.getPlayer();
        Ref<EntityStore> ref = player.getReference();
        World     world     = player.getWorld();

        world.execute(() -> {
            Store<EntityStore> store = world.getEntityStore().getStore();

            if (!ref.isValid()) return;

            // Guard: skip if already initialised (e.g. world switch).
            if (store.getComponent(ref, FlowComponent.getComponentType()) != null) return;

            // putComponent → adds persistently; component survives across sessions.
            store.putComponent(ref, FlowComponent.getComponentType(),     new FlowComponent());
            store.putComponent(ref, LucidityComponent.getComponentType(), new LucidityComponent());
            store.putComponent(ref, WeaponSlotComponent.getComponentType(), new WeaponSlotComponent());

            player.sendMessage(Message.raw(I18n.t("session.save.resumed", player.getDisplayName())));
        });
    }
}
