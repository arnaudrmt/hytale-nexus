package fr.arnaud.nexus.event;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.ui.dashboard.RunDashboardPage;

import java.util.function.Consumer;

public final class RunCompletedHandler implements Consumer<RunCompletedEvent> {

    @Override
    public void accept(RunCompletedEvent event) {
        Ref<EntityStore> ref = event.playerRef();
        if (!ref.isValid()) return;

        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();
        if (world == null) return;

        world.execute(() -> {
            if (!ref.isValid()) return;
            Store<EntityStore> s = ref.getStore();

            Player player = s.getComponent(ref, Player.getComponentType());
            if (player == null) return;

            PlayerRef playerRef = s.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef == null) return;

            player.getPageManager().openCustomPage(ref, s,
                new RunDashboardPage(playerRef, event.snapshot()));
        });
    }
}
