package fr.arnaud.nexus.ui.hud;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class NexusHudSystem {

    private record HudEntry(NexusHud hud, World world) {
    }

    private static final Map<Player, HudEntry> entriesByPlayer = new ConcurrentHashMap<>();
    private static final Map<Ref<EntityStore>, HudEntry> entriesByRef = new ConcurrentHashMap<>();

    private NexusHudSystem() {
    }

    public static void onPlayerReady(@NonNullDecl PlayerReadyEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        if (world == null) return;

        world.execute(() -> {
            Ref<EntityStore> ref = player.getReference();
            if (ref == null || !ref.isValid()) return;

            Store<EntityStore> store = world.getEntityStore().getStore();
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef == null) return;

            NexusHud hud = new NexusHud(playerRef);
            HudEntry entry = new HudEntry(hud, world);

            entriesByPlayer.put(player, entry);
            entriesByRef.put(ref, entry);
            player.getHudManager().setCustomHud(playerRef, hud);
            hud.refresh(ref, store);
        });
    }

    public static void notifyFlowChanged(@NonNullDecl Ref<EntityStore> ref,
                                         @NonNullDecl Store<EntityStore> store) {
        refreshByRef(ref, store);
    }

    public static void notifyFlowChanged(@NonNullDecl Player player,
                                         @NonNullDecl Ref<EntityStore> ref,
                                         @NonNullDecl Store<EntityStore> store) {
        refreshByPlayer(player, ref, store);
    }

    public static void notifyLucidityChanged(@NonNullDecl Player player,
                                             @NonNullDecl Ref<EntityStore> ref,
                                             @NonNullDecl Store<EntityStore> store) {
        refreshByPlayer(player, ref, store);
    }

    public static void notifyDustChanged(@NonNullDecl Player player,
                                         @NonNullDecl Ref<EntityStore> ref,
                                         @NonNullDecl Store<EntityStore> store) {
        refreshByPlayer(player, ref, store);
    }

    public static void removePlayer(@NonNullDecl Player player) {
        HudEntry entry = entriesByPlayer.remove(player);
        if (entry != null) {
            Ref<EntityStore> ref = player.getReference();
            if (ref != null) entriesByRef.remove(ref);
        }
    }

    private static void refreshByRef(@NonNullDecl Ref<EntityStore> ref,
                                     @NonNullDecl Store<EntityStore> store) {
        HudEntry entry = entriesByRef.get(ref);
        if (entry == null) return;
        entry.world().execute(() -> entry.hud().refresh(ref, store));
    }

    private static void refreshByPlayer(@NonNullDecl Player player,
                                        @NonNullDecl Ref<EntityStore> ref,
                                        @NonNullDecl Store<EntityStore> store) {
        HudEntry entry = entriesByPlayer.get(player);
        if (entry == null) return;
        entry.world().execute(() -> entry.hud().refresh(ref, store));
    }
}
