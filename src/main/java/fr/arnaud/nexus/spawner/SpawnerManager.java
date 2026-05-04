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
import java.util.function.Consumer;

public final class SpawnerManager {

    private final ChestManager chestManager;
    private final MobSpawnExecutor mobSpawnExecutor;
    private final FinishPortalController portalController;
    private final WaveController waveController;
    private final WaveBarStateProvider waveBarStateProvider;

    private final List<SpawnerState> spawnerStates = new ArrayList<>();
    private World activeWorld;

    private LevelProgressComponent activeProgress;
    private Consumer<LevelProgressComponent> progressWriter = p -> {
    };

    public SpawnerManager(LevelTransitionService levelTransitionService,
                          WaveBarStateProvider waveBarStateProvider) {
        this.waveBarStateProvider = waveBarStateProvider;
        this.mobSpawnExecutor = new MobSpawnExecutor(() -> activeWorld);
        this.chestManager = new ChestManager(() -> activeWorld, p -> progressWriter.accept(p));
        this.portalController = new FinishPortalController(levelTransitionService, () -> activeWorld);
        this.waveController = new WaveController(buildWaveEventSink(), waveBarStateProvider);
        chestManager.setStandaloneChestLootPersister((id, items) -> {
            if (activeProgress != null) {
                activeProgress.recordStandaloneChestLoot(id, items);
                progressWriter.accept(activeProgress);
            }
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
                if (activeProgress != null) {
                    chestManager.spawnLootChest(state, activeProgress);
                }
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

        chestManager.onLevelLoaded(config);
        portalController.onLevelLoaded(config);
        mobSpawnExecutor.reset();
    }

    public void onPlayerReady(Ref<EntityStore> playerRef, Store<EntityStore> store) {
        LevelProgressComponent progress =
            store.getComponent(playerRef, LevelProgressComponent.getComponentType());
        if (progress == null) return;

        this.activeProgress = progress;
        restoreSpawnerStatesFromProgress(progress);
        chestManager.restoreFromProgress(progress);
    }

    private void restoreSpawnerStatesFromProgress(LevelProgressComponent progress) {
        for (SpawnerState state : spawnerStates) {
            int id = state.getId();

            if (progress.clearedSpawnerIndices.contains(id)) {
                state.markComplete();
                continue;
            }

            if (progress.activatedSpawnerIndices.contains(id)) {
                state.markTriggered();
                waveController.activateSpawner(state);
            }

            List<String> savedLoot = progress.pendingSpawnerChestLoot.get(id);
            if (savedLoot == null || savedLoot.isEmpty()) continue;

            WorldPosition pos = state.getConfig().position();
            Vector3d chestPos = new Vector3d(pos.x(), pos.y(), pos.z());

            state.markChestSpawned();
            state.setPendingChestLoot(new ArrayList<>(savedLoot));
            state.setChestPosition(chestPos);
            chestManager.placeRestoredSpawnerChest(chestPos);
        }
    }

    public void reset() {
        spawnerStates.clear();
        activeWorld = null;
        activeProgress = null;
        progressWriter = p -> {
        };
        chestManager.reset();
        mobSpawnExecutor.reset();
        waveBarStateProvider.onLevelReset();
    }

    public void tick(float dt, Vector3d playerPosition,
                     LevelProgressComponent progress,
                     CommandBuffer<EntityStore> commandBuffer,
                     Ref<EntityStore> playerRef) {
        if (activeWorld == null) return;

        this.activeProgress = progress;
        this.progressWriter = p -> commandBuffer.putComponent(
            playerRef, LevelProgressComponent.getComponentType(), p);

        WaveController.ProgressWriter waveProgressWriter = p -> progressWriter.accept(p);

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
                checkProximityTrigger(state, playerPosition, progress);
            } else {
                waveController.tick(state, dt, progress, waveProgressWriter);
                maybeRecordSpawnerCleared(state, progress);
            }
        }
    }

    public void onChestOpened(int spawnerIndex) {
        if (activeProgress == null) return;
        activeProgress.recordSpawnerCleared(spawnerIndex);
        progressWriter.accept(activeProgress);
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

    private void checkProximityTrigger(SpawnerState state, Vector3d playerPosition,
                                       LevelProgressComponent progress) {
        WorldPosition pos = state.getConfig().position();
        double dx = playerPosition.getX() - pos.x();
        double dy = playerPosition.getY() - pos.y();
        double dz = playerPosition.getZ() - pos.z();
        float radius = state.getConfig().activationRadius();

        if (dx * dx + dy * dy + dz * dz > radius * radius) return;

        state.markTriggered();
        progress.recordSpawnerActivated(state.getId());
        progressWriter.accept(progress);
        waveController.activateSpawner(state);
    }

    private void maybeRecordSpawnerCleared(SpawnerState state, LevelProgressComponent progress) {
        if (state.getConfig().hasLootChest()) return;
        if (!state.isComplete()) return;
        if (progress.clearedSpawnerIndices.contains(state.getId())) return;

        progress.recordSpawnerCleared(state.getId());
        progressWriter.accept(progress);
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
