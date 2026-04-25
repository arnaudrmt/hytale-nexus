package fr.arnaud.nexus.event;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.component.RunSessionComponent;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public record RunCompletedEvent(
    @NonNullDecl Ref<EntityStore> playerRef,
    @NonNullDecl RunSessionComponent snapshot
) implements IEvent<Void> {

    public static void dispatch(@NonNullDecl Ref<EntityStore> playerRef,
                                @NonNullDecl RunSessionComponent snapshot) {
        IEventDispatcher<RunCompletedEvent, RunCompletedEvent> dispatcher =
            HytaleServer.get().getEventBus().dispatchFor(RunCompletedEvent.class, null);
        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new RunCompletedEvent(playerRef, snapshot));
        }
    }
}
