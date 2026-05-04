package fr.arnaud.nexus.spawner.mob;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.ChunkFlag;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.level.LevelConfig;
import fr.arnaud.nexus.spawner.SpawnerState;
import fr.arnaud.nexus.spawner.SpawnerTagComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.logging.Level;

public final class MobSpawnExecutor {

    private static final int MAX_SPAWN_ATTEMPTS = 40;

    private final Supplier<World> activeWorldSupplier;
    private final List<PendingSpawn> retryQueue = new ArrayList<>();

    private record PendingSpawn(SpawnerState state, LevelConfig.MobEntry entry,
                                Vector3d position, int waveIndex, int attemptsLeft) {
    }

    public MobSpawnExecutor(Supplier<World> activeWorldSupplier) {
        this.activeWorldSupplier = activeWorldSupplier;
    }

    public void spawnMobBatch(SpawnerState state, LevelConfig.MobEntry entry, int count) {
        World world = activeWorldSupplier.get();
        if (world == null) return;

        int waveIndex = state.getActiveWave();

        world.execute(() -> {
            Store<EntityStore> store = world.getEntityStore().getStore();
            for (int i = 0; i < count; i++) {
                Vector3d position = randomPositionAround(state.getConfig());
                trySpawnOrEnqueue(state, entry, position, waveIndex, store, MAX_SPAWN_ATTEMPTS);
            }
        });
    }

    public void drainRetryQueue() {
        if (retryQueue.isEmpty()) return;

        World world = activeWorldSupplier.get();
        if (world == null) return;

        List<PendingSpawn> pending = new ArrayList<>(retryQueue);
        retryQueue.clear();

        world.execute(() -> {
            Store<EntityStore> store = world.getEntityStore().getStore();
            for (PendingSpawn p : pending) {
                trySpawnOrEnqueue(p.state(), p.entry(), p.position(), p.waveIndex(), store, p.attemptsLeft());
            }
        });
    }

    public void reset() {
        retryQueue.clear();
    }

    private void trySpawnOrEnqueue(SpawnerState state, LevelConfig.MobEntry entry,
                                   Vector3d position, int waveIndex,
                                   Store<EntityStore> store, int attemptsLeft) {
        if (!isChunkTicking(position)) {
            enqueueRetryOrAbandon(state, entry, position, waveIndex, attemptsLeft);
            return;
        }

        var spawnedPair = NPCPlugin.get().spawnNPC(store, entry.mobId(), null, position, Vector3f.ZERO);
        if (spawnedPair == null || spawnedPair.first() == null || !spawnedPair.first().isValid()) {
            enqueueRetryOrAbandon(state, entry, position, waveIndex, attemptsLeft);
            return;
        }

        store.addComponent(spawnedPair.first(), SpawnerTagComponent.getComponentType(),
            new SpawnerTagComponent(state.getId(), waveIndex, entry.minEssence(), entry.maxEssence()));
    }

    private void enqueueRetryOrAbandon(SpawnerState state, LevelConfig.MobEntry entry,
                                       Vector3d position, int waveIndex, int attemptsLeft) {
        if (attemptsLeft > 0) {
            retryQueue.add(new PendingSpawn(state, entry, position, waveIndex, attemptsLeft - 1));
        } else {
            Nexus.getInstance().getLogger().at(Level.WARNING)
                 .log("Abandoned spawn for " + entry.mobId() + " at " + position + " after max retries.");
        }
    }

    private boolean isChunkTicking(Vector3d position) {
        World world = activeWorldSupplier.get();
        if (world == null) return false;

        ChunkStore chunkStore = world.getChunkStore();
        long index = ChunkUtil.indexChunkFromBlock((int) position.getX(), (int) position.getZ());
        Ref<ChunkStore> ref = chunkStore.getChunkReference(index);
        if (ref == null || !ref.isValid()) return false;

        WorldChunk chunk = chunkStore.getStore().getComponent(ref, WorldChunk.getComponentType());
        return chunk != null && chunk.is(ChunkFlag.TICKING);
    }

    private static Vector3d randomPositionAround(LevelConfig.Spawner config) {
        Random rng = ThreadLocalRandom.current();
        float radius = config.spawnRadius();
        double angle = rng.nextDouble() * 2.0 * Math.PI;
        double distance = rng.nextDouble() * radius;

        return new Vector3d(
            config.position().x() + Math.cos(angle) * distance,
            config.position().y(),
            config.position().z() + Math.sin(angle) * distance
        );
    }
}
