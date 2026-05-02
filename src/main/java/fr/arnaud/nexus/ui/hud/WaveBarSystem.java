package fr.arnaud.nexus.ui.hud;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.math.WorldPosition;
import fr.arnaud.nexus.spawner.SpawnerState;
import fr.arnaud.nexus.spawner.WaveBarStateProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class WaveBarSystem extends EntityTickingSystem<EntityStore> {

    private static final float TICK_INTERVAL = 0.25f;
    private static final float TRIGGER_RADIUS_SQ = 20f * 20f;
    private static final float COMPLETE_DURATION = 3f;

    private record PlayerEntry(
        World world,
        float[] pollTimer,
        float[] completeTimer,
        int[] lastSpawnerId,
        int[] lastKilled,
        int[] lastTotal,
        boolean[] lastCompleted
    ) {
    }

    private final Map<Ref<EntityStore>, PlayerEntry> entries = new ConcurrentHashMap<>();

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(PlayerRef.getComponentType(), TransformComponent.getComponentType());
    }

    @Override
    public void tick(float dt, int index, ArchetypeChunk<EntityStore> chunk,
                     @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> cmd) {

        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        PlayerEntry entry = entries.get(ref);
        if (entry == null) return;

        if (entry.completeTimer()[0] > 0f) {
            entry.completeTimer()[0] -= dt;
            if (entry.completeTimer()[0] <= 0f) {
                entry.completeTimer()[0] = 0f;
                entry.lastSpawnerId()[0] = -1;
                entry.world().execute(() -> Nexus.getInstance().getNexusHudSystem().hideWave(ref));
            }
            return;
        }

        entry.pollTimer()[0] += dt;
        if (entry.pollTimer()[0] < TICK_INTERVAL) return;
        entry.pollTimer()[0] = 0f;

        TransformComponent transform = chunk.getComponent(index, TransformComponent.getComponentType());
        if (transform == null) return;

        SpawnerState nearest = findNearestActiveSpawner(transform.getPosition(), entry.lastSpawnerId()[0]);

        if (nearest == null) {
            if (entry.lastSpawnerId()[0] != -1) {
                entry.lastSpawnerId()[0] = -1;
                entry.lastCompleted()[0] = false;
                entry.world().execute(() -> Nexus.getInstance().getNexusHudSystem().hideWave(ref));
            }
            return;
        }

        WaveBarStateProvider.WaveSnapshot snap =
            Nexus.getInstance().getWaveBarStateProvider().getSnapshot(nearest.getId());
        if (snap == null) return;

        if (snap.completed() && !entry.lastCompleted()[0]) {
            entry.lastCompleted()[0] = true;
            entry.lastSpawnerId()[0] = snap.spawnerId();
            entry.completeTimer()[0] = COMPLETE_DURATION;
            entry.world().execute(() -> Nexus.getInstance().getNexusHudSystem().showWaveComplete(ref));
            return;
        }

        if (snap.completed()) return;

        if (entry.lastSpawnerId()[0] == snap.spawnerId()
            && entry.lastKilled()[0] == snap.killed()
            && entry.lastTotal()[0] == snap.total()) return;

        entry.lastSpawnerId()[0] = snap.spawnerId();
        entry.lastKilled()[0] = snap.killed();
        entry.lastTotal()[0] = snap.total();
        entry.lastCompleted()[0] = false;

        int cw = snap.currentWave(), tw = snap.totalWaves();
        int k = snap.killed(), t = snap.total();
        entry.world().execute(() -> Nexus.getInstance().getNexusHudSystem().showWave(ref, cw, tw, k, t));
    }

    private static SpawnerState findNearestActiveSpawner(Vector3d pos, int completingSpawnerId) {
        SpawnerState nearest = null;
        double bestDistSq = TRIGGER_RADIUS_SQ;

        for (SpawnerState state : Nexus.getInstance().getSpawnerRegistry().getSpawnerStates()) {
            if (!state.isTriggered()) continue;
            if (state.isComplete() && state.getId() != completingSpawnerId) continue;

            WorldPosition p = state.getConfig().position();
            double dx = pos.getX() - p.x();
            double dy = pos.getY() - p.y();
            double dz = pos.getZ() - p.z();
            double distSq = dx * dx + dy * dy + dz * dz;

            if (distSq <= bestDistSq) {
                bestDistSq = distSq;
                nearest = state;
            }
        }
        return nearest;
    }

    public void onPlayerReady(Player player) {
        World world = player.getWorld();
        if (world == null) return;

        world.execute(() -> {
            Ref<EntityStore> ref = player.getReference();
            if (ref == null || !ref.isValid()) return;

            entries.put(ref, new PlayerEntry(
                world,
                new float[]{0f}, new float[]{0f},
                new int[]{-1}, new int[]{0}, new int[]{0},
                new boolean[]{false}
            ));
        });
    }

    public void removePlayer(Player player) {
        Ref<EntityStore> ref = player.getReference();
        if (ref != null) entries.remove(ref);
    }
}
