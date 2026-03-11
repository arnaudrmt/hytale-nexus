package fr.arnaud.nexus.event;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Fired when a player successfully confirms a Switch Strike by swapping weapons
 * within the activation window after their Razorstrike ability lands.
 * <p>
 * Downstream handlers subscribe independently via the EventBus — no caller
 * needs to know which systems care.
 */
public record SwitchStrikeActivatedEvent(Ref<EntityStore> playerRef) implements IEvent<Void> {

    public static void dispatch(Ref<EntityStore> playerRef) {
        IEventDispatcher<SwitchStrikeActivatedEvent, SwitchStrikeActivatedEvent> dispatcher =
            HytaleServer.get().getEventBus().dispatchFor(SwitchStrikeActivatedEvent.class, null);
        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new SwitchStrikeActivatedEvent(playerRef));
        }
    }
}
