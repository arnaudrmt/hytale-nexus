package fr.arnaud.nexus.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.tutorial.TutorialStepConfig;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class NexusHudSystem {

    private final Map<Ref<EntityStore>, NexusHud> huds = new ConcurrentHashMap<>();

    public void onPlayerReady(Player player, Runnable onReady) {
        World world = player.getWorld();
        if (world == null) return;
        world.execute(() -> {
            Ref<EntityStore> ref = player.getReference();
            if (ref == null || !ref.isValid()) return;
            Store<EntityStore> store = world.getEntityStore().getStore();
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef == null) return;

            huds.entrySet().removeIf(e -> !e.getKey().isValid());

            if (huds.containsKey(ref)) {
                onReady.run();
                return;
            }

            NexusHud hud = new NexusHud(playerRef);
            huds.put(ref, hud);
            player.getHudManager().setCustomHud(playerRef, hud);
            onReady.run();
        });
    }

    @Nullable
    public NexusHud getHud(Ref<EntityStore> ref) {
        return huds.get(ref);
    }

    public void removePlayer(Player player) {
        Ref<EntityStore> ref = player.getReference();
        if (ref != null) huds.remove(ref);
    }

    public void showWave(Ref<EntityStore> ref, int cw, int tw, int k, int t) {
        if (!ref.isValid()) return;
        NexusHud hud = huds.get(ref);
        if (hud == null) return;
        hud.showWave(cw, tw, k, t);
    }

    public void showWaveComplete(Ref<EntityStore> ref) {
        if (!ref.isValid()) return;
        NexusHud hud = huds.get(ref);
        if (hud == null) return;
        hud.showWaveComplete();
    }

    public void hideWave(Ref<EntityStore> ref) {
        if (!ref.isValid()) return;
        NexusHud hud = huds.get(ref);
        if (hud == null) return;
        hud.hideWave();
    }

    public void showTutorialStep(Player player, TutorialStepConfig step) {
        Ref<EntityStore> ref = player.getReference();
        if (ref == null || !ref.isValid()) return;
        NexusHud hud = huds.get(ref);
        if (hud == null) return;
        hud.showTutorialStep(step);
    }

    public void hideTutorial(Player player) {
        Ref<EntityStore> ref = player.getReference();
        if (ref == null || !ref.isValid()) return;
        NexusHud hud = huds.get(ref);
        if (hud == null) return;
        hud.hideTutorial();
    }
}
