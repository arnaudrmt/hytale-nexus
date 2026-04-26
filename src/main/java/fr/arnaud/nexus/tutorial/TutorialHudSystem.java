package fr.arnaud.nexus.tutorial;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TutorialHudSystem {

    private final Map<Ref<EntityStore>, TutorialHud> huds = new ConcurrentHashMap<>();

    public void onPlayerReady(Player player, Runnable onReady) {
        World world = player.getWorld();
        if (world == null) return;

        world.execute(() -> {
            Ref<EntityStore> ref = player.getReference();
            if (ref == null || !ref.isValid()) return;

            Store<EntityStore> store = world.getEntityStore().getStore();
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef == null) return;

            TutorialHud hud = new TutorialHud(playerRef);
            huds.put(ref, hud);
            player.getHudManager().setCustomHud(playerRef, hud);

            onReady.run();
        });
    }

    public void onPlayerReady(Player player) {
        onPlayerReady(player, () -> {
        });
    }

    public void showStep(Player player, TutorialStepConfig step) {
        Ref<EntityStore> ref = player.getReference();
        if (ref == null) return;
        TutorialHud hud = huds.get(ref);
        if (hud == null) return;
        hud.showStep(step);
    }

    public void hide(Player player) {
        Ref<EntityStore> ref = player.getReference();
        if (ref == null) return;
        TutorialHud hud = huds.get(ref);
        if (hud == null) return;
        hud.hide();
    }

    public void removePlayer(Player player) {
        Ref<EntityStore> ref = player.getReference();
        if (ref != null) huds.remove(ref);
    }
}
