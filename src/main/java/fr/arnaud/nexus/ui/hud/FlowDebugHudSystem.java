package fr.arnaud.nexus.ui.hud;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class FlowDebugHudSystem extends EntityTickingSystem<EntityStore> {

    private static final float REFRESH_INTERVAL = 1.0f;

    private record HudEntry(FlowDebugHud hud, World world, float[] accumulator) {
    }

    private static final Map<Ref<EntityStore>, HudEntry> entriesByRef = new ConcurrentHashMap<>();

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(PlayerRef.getComponentType(), EntityStatMap.getComponentType());
    }

    @Override
    public void tick(float dt, int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store, CommandBuffer<EntityStore> cmd) {
        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        HudEntry entry = entriesByRef.get(ref);
        if (entry == null) return;

        entry.accumulator()[0] += dt;
        if (entry.accumulator()[0] < REFRESH_INTERVAL) return;
        entry.accumulator()[0] = 0f;

        cmd.run(s -> entry.world().execute(() -> entry.hud().refresh(ref, s)));
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

            FlowDebugHud hud = new FlowDebugHud(playerRef);
            entriesByRef.put(ref, new HudEntry(hud, world, new float[]{0f}));
            player.getHudManager().setCustomHud(playerRef, hud);
            hud.refresh(ref, store);
        });
    }

    public static void removePlayer(@NonNullDecl Player player) {
        Ref<EntityStore> ref = player.getReference();
        if (ref != null) entriesByRef.remove(ref);
    }
}
