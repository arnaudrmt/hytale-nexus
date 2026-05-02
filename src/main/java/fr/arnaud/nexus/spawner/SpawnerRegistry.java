package fr.arnaud.nexus.spawner;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.level.LevelConfig;
import fr.arnaud.nexus.level.LevelProgressComponent;
import fr.arnaud.nexus.level.LevelTransitionService;
import fr.arnaud.nexus.math.WorldPosition;
import fr.arnaud.nexus.spawner.chest.ChestManager;
import fr.arnaud.nexus.spawner.mob.MobSpawnExecutor;
import fr.arnaud.nexus.spawner.portal.FinishPortalController;
import fr.arnaud.nexus.spawner.wave.WaveController;
import fr.arnaud.nexus.spawner.wave.WaveEventSink;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SpawnerRegistry {

    private final ChestManager chestManager;
    private final MobSpawnExecutor mobSpawnExecutor;
    private final FinishPortalController portalController;
    private final WaveController waveController;
    private final WaveBarStateProvider waveBarStateProvider;

    private final List<SpawnerState> spawnerStates = new ArrayList<>();
    private World activeWorld;

    public SpawnerRegistry(LevelTransitionService levelTransitionService,
                           WaveBarStateProvider waveBarStateProvider) {
        this.waveBarStateProvider = waveBarStateProvider;
        this.mobSpawnExecutor = new MobSpawnExecutor(() -> activeWorld);
        this.chestManager = new ChestManager(() -> activeWorld, p -> {
        });
        this.portalController = new FinishPortalController(levelTransitionService, () -> activeWorld);
        this.waveController = new WaveController(buildWaveEventSink(), waveBarStateProvider);
        chestManager.setStandaloneChestLootPersister((id, items) -> {
        });
    }

    private WaveEventSink buildWaveEventSink() {
        return new WaveEventSink() {
            @Override
            public void onMobsRequested(SpawnerState state, LevelConfig.MobEntry entry, int count) {
                mobSpawnExecutor.spawnMobBatch(state, entry, count);
            }

            @Override
            public void onChestRequested(SpawnerState state) {
                chestManager.spawnLootChest(state, new LevelProgressComponent());
            }
        };
    }

    public void onLevelLoaded(World world, LevelConfig config) {
        this.activeWorld = world;
        spawnerStates.clear();

        int idSequence = 0;
        for (LevelConfig.Spawner spawner : config.spawners()) {
            spawnerStates.add(new SpawnerState(idSequence++, spawner));
        }

        chestManager.onLevelLoaded(config, new LevelProgressComponent());
        portalController.onLevelLoaded(config);
        mobSpawnExecutor.reset();
    }

    public void reset() {
        spawnerStates.clear();
        activeWorld = null;
        chestManager.reset();
        mobSpawnExecutor.reset();
        waveBarStateProvider.onLevelReset();
    }

    public void tick(float dt, Vector3d playerPosition,
                     LevelProgressComponent progress,
                     CommandBuffer<EntityStore> commandBuffer,
                     Ref<EntityStore> playerRef) {
        if (activeWorld == null) return;

        WaveController.ProgressWriter progressWriter =
            p -> commandBuffer.putComponent(playerRef, LevelProgressComponent.getComponentType(), p);

        mobSpawnExecutor.drainRetryQueue();

        chestManager.tick(playerPosition);

        if (!portalController.isPortalSpawned() && areAllSpawnersComplete()) {
            portalController.spawnPortal();
        }

        if (portalController.isPortalSpawned()) {
            portalController.checkProximityTrigger(playerPosition, playerRef, commandBuffer);
        }

        for (SpawnerState state : spawnerStates) {
            if (state.isComplete()) continue;

            if (!state.isTriggered()) {
                checkProximityTrigger(state, playerPosition);
            } else {
                waveController.tick(state, dt, progress, progressWriter);
            }
        }
    }

    public void onMobDied(int spawnerId, int waveIndex) {
        for (SpawnerState state : spawnerStates) {
            if (state.getId() != spawnerId) continue;
            if (state.getActiveWave() != waveIndex) return;
            state.decrementAliveMobs();
            waveBarStateProvider.onMobKilled(state);
            return;
        }
    }

    public boolean tryOpenChest(Vector3d clickedBlockCenter, Ref<EntityStore> playerRef,
                                Store<EntityStore> store, LevelProgressComponent progress) {
        return chestManager.tryOpenChest(clickedBlockCenter, playerRef, store,
            Collections.unmodifiableList(spawnerStates), progress);
    }

    public List<SpawnerState> getSpawnerStates() {
        return Collections.unmodifiableList(spawnerStates);
    }

    private void checkProximityTrigger(SpawnerState state, Vector3d playerPosition) {
        WorldPosition pos = state.getConfig().position();
        double dx = playerPosition.getX() - pos.x();
        double dy = playerPosition.getY() - pos.y();
        double dz = playerPosition.getZ() - pos.z();
        float radius = state.getConfig().activationRadius();

        if (dx * dx + dy * dy + dz * dz > radius * radius) return;

        state.markTriggered();
        waveController.activateSpawner(state);
    }

    private boolean areAllSpawnersComplete() {
        if (spawnerStates.isEmpty()) return false;
        SpawnerState last = spawnerStates.getLast();
        if (last.getConfig().hasLootChest()) {
            return last.isChestSpawned();
        }
        return last.isTriggered()
            && last.getAliveMobsInCurrentWave() == 0
            && last.getTotalMobsInCurrentWave() > 0;
    }
}
