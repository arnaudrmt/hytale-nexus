package fr.arnaud.nexus.handler;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.component.RunSessionComponent;
import fr.arnaud.nexus.event.SwitchStrikeActivatedEvent;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.function.Consumer;

/**
 * Reacts to a confirmed Switch Strike by recording it in the run session
 * for end-of-run score calculation.
 */
public final class SwitchStrikeHandler implements Consumer<SwitchStrikeActivatedEvent> {

    @Override
    public void accept(@NonNullDecl SwitchStrikeActivatedEvent event) {
        Ref<EntityStore> ref = event.playerRef();
        if (!ref.isValid()) return;

        Store<EntityStore> store = ref.getStore();

        RunSessionComponent session = store.getComponent(ref, RunSessionComponent.getComponentType());
        if (session == null) return;

        session.incrementSwitchStrikes();
        store.putComponent(ref, RunSessionComponent.getComponentType(), session);
    }
}
