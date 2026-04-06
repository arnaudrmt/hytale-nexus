package fr.arnaud.nexus.spawning;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import fr.arnaud.nexus.level.LevelConfig;
import fr.arnaud.nexus.level.LevelProgressComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public final class MobSpawnManager {

    private final List<SpawnerState> spawnerStates = new ArrayList<>();
    private World activeWorld;

    public MobSpawnManager() {
    }

    public void onLevelLoaded(World world, LevelConfig config) {
        this.activeWorld = world;
        spawnerStates.clear();
        int idSequence = 0;
        for (LevelConfig.SpawnerConfig spawnerConfig : config.getSpawners()) {
            spawnerStates.add(new SpawnerState(idSequence++, spawnerConfig));
        }
    }

    public void reset() {
        spawnerStates.clear();
        activeWorld = null;
    }

    public List<SpawnerState> getSpawnerStates() {
        return Collections.unmodifiableList(spawnerStates);
    }

    public void tick(float dt, Vector3d position, LevelProgressComponent progress, CommandBuffer<EntityStore> commandBuffer, Ref<EntityStore> playerRef) {
        if (activeWorld == null) return;

        for (SpawnerState state : spawnerStates) {
            // 1. RESTORE STATE
            if (!state.isTriggered() && progress != null && progress.triggeredSpawners.contains(state.getId())) {
                state.markTriggered();
            }

            if (!state.isTriggered()) {
                checkProximityTrigger(state, position, progress, commandBuffer, playerRef);
            } else {
                tickActiveSpawner(state, dt);
            }
        }
    }

    private void checkProximityTrigger(SpawnerState state, Vector3d position, LevelProgressComponent progress, CommandBuffer<EntityStore> commandBuffer, Ref<EntityStore> playerRef) {
        LevelConfig.Position pos = state.getConfig().getPosition();
        double dx = position.getX() - pos.getX();
        double dy = position.getY() - pos.getY();
        double dz = position.getZ() - pos.getZ();
        double distanceSq = dx * dx + dy * dy + dz * dz;
        float triggerRadius = state.getConfig().getTriggerRadius();

        if (distanceSq <= triggerRadius * triggerRadius) {
            state.markTriggered();

            // 2. SAVE STATE using the CommandBuffer (thread-safe during ticking system)
            if (progress != null) {
                progress.triggeredSpawners.add(state.getId());
                commandBuffer.putComponent(playerRef, LevelProgressComponent.getComponentType(), progress);
            }

            activateSpawner(state);
        }
    }

    private void activateSpawner(SpawnerState state) {
        if (state.getConfig().hasWaves()) {
            advanceToWave(state, 1);
        } else {
            spawnAllMobsForWave(state, 0);
        }
    }

    private void tickActiveSpawner(SpawnerState state, float dt) {
        tickSpawnRateDrip(state, dt);

        if (!state.getConfig().hasWaves()) return;

        int currentWave = state.getActiveWave();
        LevelConfig.WaveConfig nextWaveConfig = findWaveConfig(state, currentWave + 1);
        if (nextWaveConfig == null) return;

        switch (nextWaveConfig.getType()) {
            case TIME -> tickTimeWaveTransition(state, nextWaveConfig, dt);
            case KILL -> tickKillWaveTransition(state, nextWaveConfig, dt);
        }
    }

    private void tickTimeWaveTransition(SpawnerState state, LevelConfig.WaveConfig nextWave, float dt) {
        state.addWaveTimer(dt);
        if (state.getWaveTimer() >= nextWave.getValue()) {
            advanceToWave(state, nextWave.getWave());
        }
    }

    private void tickKillWaveTransition(SpawnerState state, LevelConfig.WaveConfig nextWave, float dt) {
        int total = state.getTotalMobsInCurrentWave();
        if (total == 0) return;

        int alive = state.getAliveMobsInCurrentWave();
        float killedRatio = (total - alive) / (float) total;

        boolean thresholdReached = killedRatio >= nextWave.getValue();
        boolean timedOut = nextWave.getTimeout() > 0f
            && state.getKillWaveTimeoutTimer() >= nextWave.getTimeout();

        if (thresholdReached || timedOut) {
            advanceToWave(state, nextWave.getWave());
            return;
        }

        state.addKillWaveTimeoutTimer(dt);
    }

    private void advanceToWave(SpawnerState state, int waveIndex) {
        state.resetForNewWave();
        state.setActiveWave(waveIndex);
        spawnAllMobsForWave(state, waveIndex);
    }

    private void spawnAllMobsForWave(SpawnerState state, int waveIndex) {
        List<LevelConfig.MobEntry> entries = state.getConfig().getMobs();
        int totalForWave = 0;

        for (int i = 0; i < entries.size(); i++) {
            LevelConfig.MobEntry entry = entries.get(i);
            if (entry.getWave() != waveIndex) continue;

            int count = rollCount(entry);
            totalForWave += count;

            if (entry.getSpawnRate() == 0f) {
                spawnMobBatch(state, entry, count);
            } else {
                state.setPendingSpawns(i, count);
            }
        }

        state.setTotalMobsInCurrentWave(state.getTotalMobsInCurrentWave() + totalForWave);
        state.setAliveMobsInCurrentWave(state.getAliveMobsInCurrentWave() + totalForWave);
    }

    private void tickSpawnRateDrip(SpawnerState state, float dt) {
        List<LevelConfig.MobEntry> entries = state.getConfig().getMobs();

        for (int i = 0; i < entries.size(); i++) {
            LevelConfig.MobEntry entry = entries.get(i);
            int pending = state.getPendingSpawns(i);
            if (pending <= 0) continue;

            state.addSpawnRateAccumulator(i, dt);

            if (state.getSpawnRateAccumulator(i) >= entry.getSpawnRate()) {
                state.resetSpawnRateAccumulator(i);
                spawnMobBatch(state, entry, 1);
                state.decrementPendingSpawns(i);
            }
        }
    }

    private void spawnMobBatch(SpawnerState state, LevelConfig.MobEntry entry, int count) {
        // This is safely deferred to the world tick, outside of the ECS lock.
        activeWorld.execute(() -> {
            // Retrieve the Store dynamically here instead of passing the CommandBuffer!
            Store<EntityStore> store = activeWorld.getEntityStore().getStore();

            for (int i = 0; i < count; i++) {
                Vector3d position = randomPositionAround(state.getConfig());

                var spawnedPair = NPCPlugin.get().spawnNPC(store, entry.getMobId(), null, position, Vector3f.ZERO);

                if (spawnedPair != null) {
                    var npcRef = spawnedPair.first();

                    if (npcRef != null && npcRef.isValid()) {
                        store.addComponent(
                            npcRef,
                            SpawnerTagComponent.getComponentType(),
                            new SpawnerTagComponent(state.getId())
                        );
                    }
                }
            }
        });
    }

    private Vector3d randomPositionAround(LevelConfig.SpawnerConfig config) {
        Random rng = ThreadLocalRandom.current();
        float radius = config.getSpawnRadius();
        double angle = rng.nextDouble() * 2.0 * Math.PI;
        double distance = rng.nextDouble() * radius;

        return new Vector3d(
            config.getPosition().getX() + Math.cos(angle) * distance,
            config.getPosition().getY(),
            config.getPosition().getZ() + Math.sin(angle) * distance
        );
    }

    public void onMobDied(int spawnerId) {
        for (SpawnerState state : spawnerStates) {
            if (state.getId() == spawnerId) {
                state.decrementAliveMobs();
                break;
            }
        }
    }

    private LevelConfig.WaveConfig findWaveConfig(SpawnerState state, int waveIndex) {
        for (LevelConfig.WaveConfig wc : state.getConfig().getWaves()) {
            if (wc.getWave() == waveIndex) return wc;
        }
        return null;
    }

    private int rollCount(LevelConfig.MobEntry entry) {
        if (entry.getMinCount() == entry.getMaxCount()) return entry.getMinCount();
        return entry.getMinCount()
            + ThreadLocalRandom.current().nextInt(entry.getMaxCount() - entry.getMinCount() + 1);
    }
}
