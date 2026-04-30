package fr.arnaud.nexus.spawner;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.ChunkFlag;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.level.LevelConfig;
import fr.arnaud.nexus.level.LevelProgressComponent;
import fr.arnaud.nexus.level.LevelTransitionService;
import fr.arnaud.nexus.spawner.loot.LootRoller;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public final class MobSpawnerManager {

    private final ChestManager chestManager;
    private LevelTransitionService levelTransitionService;
    private final List<PendingSpawn> retryQueue = new ArrayList<>();

    private static final String CHEST_BLOCK_KEY = "Furniture_Dungeon_Chest_Epic";
    private static final float PORTAL_TRIGGER_RADIUS = 2.0f;

    private final List<SpawnerState> spawnerStates = new ArrayList<>();
    private World activeWorld;

    private boolean portalSpawned = false;
    private boolean levelTransitionTriggered = false;

    private record PendingSpawn(SpawnerState state, LevelConfig.MobEntry entry, Vector3d position, int attemptsLeft) {
    }

    public MobSpawnerManager(ChestManager chestManager) {
        this.chestManager = chestManager;
    }

    public void onLevelLoaded(World world, LevelConfig config) {
        this.activeWorld = world;
        spawnerStates.clear();
        portalSpawned = false;
        levelTransitionTriggered = false;
        retryQueue.clear();
        int idSequence = 0;
        stateRestored = false;
        for (LevelConfig.SpawnerConfig spawnerConfig : config.getSpawners()) {
            spawnerStates.add(new SpawnerState(idSequence++, spawnerConfig));
        }
        chestManager.onLevelLoaded(world, config);
    }

    public void reset() {
        spawnerStates.clear();
        retryQueue.clear();
        activeWorld = null;
        portalSpawned = false;
        levelTransitionTriggered = false;
        chestManager.reset();
        Nexus.get().getWaveBarStateProvider().onLevelReset();
    }

    public List<SpawnerState> getSpawnerStates() {
        return Collections.unmodifiableList(spawnerStates);
    }

    public void tick(float dt, Vector3d position, LevelProgressComponent progress,
                     CommandBuffer<EntityStore> commandBuffer, Ref<EntityStore> playerRef) {
        if (activeWorld == null) return;
        drainRetryQueue();
        chestManager.tick(position);

        restoreCompletedSpawners(progress);

        if (!portalSpawned && areAllSpawnersComplete()) {
            spawnFinishPortal();
        }

        if (portalSpawned && !levelTransitionTriggered) {
            checkPortalProximity(position, playerRef, commandBuffer);
        }

        for (SpawnerState state : spawnerStates) {
            if (state.isChestSpawned()) continue;

            if (!state.isTriggered() && progress != null
                && progress.triggeredSpawners.contains(state.getId())) {
                state.markTriggered();
            }

            if (!state.isTriggered()) {
                checkProximityTrigger(state, position, progress, commandBuffer, playerRef);
            } else {
                tickActiveSpawner(state, dt, playerRef, progress, commandBuffer);
            }
        }
    }

    private boolean stateRestored = false;

    private void restoreCompletedSpawners(LevelProgressComponent progress) {
        if (stateRestored || progress == null || progress.completedSpawners.isEmpty()) return;
        stateRestored = true;
        for (SpawnerState state : spawnerStates) {
            if (progress.completedSpawners.contains(state.getId())) {
                state.markTriggered();
                state.markChestSpawned();
            }
        }
    }

    private boolean areAllSpawnersComplete() {
        if (spawnerStates.isEmpty()) return false;
        SpawnerState last = spawnerStates.getLast();
        if (last.getConfig().hasLootChest()) {
            return last.isChestSpawned();
        }
        return last.isTriggered() && last.getAliveMobsInCurrentWave() == 0
            && last.getTotalMobsInCurrentWave() > 0;
    }

    private void spawnFinishPortal() {
        portalSpawned = true;
        LevelConfig.Position pos = Nexus.get().getLevelManager().getCurrentConfig().getFinishPoint();
        Vector3d portalPos = new Vector3d(pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5);

        activeWorld.execute(() -> activeWorld.getPlayerRefs().forEach(_ ->
            ParticleUtil.spawnParticleEffect(
                "MagicPortal_Default",
                portalPos,
                activeWorld.getEntityStore().getStore()
            )
        ));
    }

    private void checkPortalProximity(Vector3d position, Ref<EntityStore> playerRef,
                                      CommandBuffer<EntityStore> commandBuffer) {
        LevelConfig.Position pos = Nexus.get().getLevelManager().getCurrentConfig().getFinishPoint();
        double dx = position.getX() - pos.getX();
        double dy = position.getY() - pos.getY();
        double dz = position.getZ() - pos.getZ();

        if (dx * dx + dy * dy + dz * dz > PORTAL_TRIGGER_RADIUS * PORTAL_TRIGGER_RADIUS) return;

        levelTransitionTriggered = true;

        getLevelTransitionService().onPortalEntered(playerRef, commandBuffer, activeWorld);
    }

    private void checkProximityTrigger(SpawnerState state, Vector3d position,
                                       LevelProgressComponent progress,
                                       CommandBuffer<EntityStore> commandBuffer,
                                       Ref<EntityStore> playerRef) {
        LevelConfig.Position pos = state.getConfig().getPosition();
        double dx = position.getX() - pos.getX();
        double dy = position.getY() - pos.getY();
        double dz = position.getZ() - pos.getZ();
        double distanceSq = dx * dx + dy * dy + dz * dz;
        float triggerRadius = state.getConfig().getTriggerRadius();

        if (distanceSq > triggerRadius * triggerRadius) return;

        state.markTriggered();

        if (progress != null) {
            progress.triggeredSpawners.add(state.getId());
            commandBuffer.putComponent(playerRef, LevelProgressComponent.getComponentType(), progress);
        }

        activateSpawner(state);
    }

    private void activateSpawner(SpawnerState state) {
        if (state.getConfig().hasWaves()) {
            advanceToWave(state, 1);
        } else {
            spawnAllMobsForWave(state, 0);
        }
    }

    private void tickActiveSpawner(SpawnerState state, float dt,
                                   Ref<EntityStore> playerRef,
                                   LevelProgressComponent progress,
                                   CommandBuffer<EntityStore> commandBuffer) {
        tickSpawnRateDrip(state, dt);

        if (!state.getConfig().hasWaves()) return;

        int currentWave = state.getActiveWave();
        LevelConfig.WaveConfig nextWaveConfig = findWaveConfig(state, currentWave + 1);
        if (nextWaveConfig == null) {
            checkFinalWaveCompletion(state, playerRef, progress, commandBuffer);
            return;
        }

        switch (nextWaveConfig.getType()) {
            case TIME -> tickTimeWaveTransition(state, nextWaveConfig, dt);
            case KILL -> tickKillWaveTransition(state, nextWaveConfig, dt);
        }
    }

    private void checkFinalWaveCompletion(SpawnerState state,
                                          Ref<EntityStore> playerRef,
                                          LevelProgressComponent progress,
                                          CommandBuffer<EntityStore> commandBuffer) {
        if (state.isChestSpawned()) return;
        if (state.getAliveMobsInCurrentWave() > 0) return;
        if (state.getTotalMobsInCurrentWave() == 0) return;
        if (!state.getConfig().hasLootChest()) return;

        state.markChestSpawned();
        Nexus.get().getWaveBarStateProvider().onWaveStarted(state);
        spawnLootChest(state);

        if (progress != null) {
            progress.completedSpawners.add(state.getId());
            commandBuffer.putComponent(playerRef, LevelProgressComponent.getComponentType(), progress);
        }

        if (progress != null) {
            LevelConfig.Position pos = state.getConfig().getPosition();
            progress.checkpointX = (float) pos.getX();
            progress.checkpointY = (float) pos.getY();
            progress.checkpointZ = (float) pos.getZ() + 1.5f;
            commandBuffer.putComponent(playerRef, LevelProgressComponent.getComponentType(), progress);
        }
    }

    private void spawnLootChest(SpawnerState state) {
        if (state.getConfig().getLootChest() == null) return;

        List<String> rolledItems = LootRoller.roll(state.getConfig().getLootChest());
        LevelConfig.Position pos = state.getConfig().getPosition();
        Vector3d chestPos = new Vector3d(pos.getX(), pos.getY(), pos.getZ());

        state.setPendingChestLoot(rolledItems);
        state.setChestPosition(chestPos);

        activeWorld.execute(() -> {
            EntityStore store = activeWorld.getEntityStore();
            int index = SoundEvent.getAssetMap().getIndex("SFX_Portal_Neutral_Open.json");

            activeWorld.getPlayerRefs().forEach(playerRef -> {
                TransformComponent transform = store.getStore().getComponent(
                    playerRef.getReference(), EntityModule.get().getTransformComponentType());
                SoundUtil.playSoundEvent3dToPlayer(
                    playerRef.getReference(), index, SoundCategory.UI,
                    transform.getPosition(), store.getStore());
            });

            activeWorld.setBlock(
                (int) Math.floor(chestPos.getX()),
                (int) Math.floor(chestPos.getY()),
                (int) Math.floor(chestPos.getZ()),
                CHEST_BLOCK_KEY
            );
            chestManager.spawnAuraSphere(chestPos);
        });
    }

    public boolean tryOpenChest(Vector3d clickedBlockCenter, Ref<EntityStore> playerRef,
                                Store<EntityStore> store) {
        return chestManager.tryOpenChest(
            clickedBlockCenter, playerRef, store, Collections.unmodifiableList(spawnerStates));
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
        Nexus.get().getWaveBarStateProvider().onWaveStarted(state);
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
        activeWorld.execute(() -> {
            Store<EntityStore> store = activeWorld.getEntityStore().getStore();
            for (int i = 0; i < count; i++) {
                Vector3d position = randomPositionAround(state.getConfig());
                trySpawnOrEnqueue(state, entry, position, store, 40);
            }
        });
    }

    private void trySpawnOrEnqueue(SpawnerState state, LevelConfig.MobEntry entry,
                                   Vector3d position, Store<EntityStore> store, int attemptsLeft) {
        if (!isChunkTicking(position)) {
            if (attemptsLeft > 0) {
                retryQueue.add(new PendingSpawn(state, entry, position, attemptsLeft - 1));
            } else {
                Nexus.get().getLogger().at(Level.WARNING)
                     .log("[SpawnDebug] Gave up on " + entry.getMobId() + " at " + position);
            }
            return;
        }

        var spawnedPair = NPCPlugin.get().spawnNPC(store, entry.getMobId(), null, position, Vector3f.ZERO);
        if (spawnedPair == null || spawnedPair.first() == null || !spawnedPair.first().isValid()) {
            if (attemptsLeft > 0) {
                retryQueue.add(new PendingSpawn(state, entry, position, attemptsLeft - 1));
            }
            return;
        }

        store.addComponent(spawnedPair.first(), SpawnerTagComponent.getComponentType(),
            new SpawnerTagComponent(state.getId(), entry.getMinEssence(), entry.getMaxEssence()));
    }

    private boolean isChunkTicking(Vector3d position) {
        ChunkStore chunkStore = activeWorld.getChunkStore();
        long index = ChunkUtil.indexChunkFromBlock((int) position.getX(), (int) position.getZ());
        Ref<ChunkStore> ref = chunkStore.getChunkReference(index);
        if (ref == null || !ref.isValid()) return false;
        WorldChunk chunk = chunkStore.getStore().getComponent(ref, WorldChunk.getComponentType());
        return chunk != null && chunk.is(ChunkFlag.TICKING);
    }

    private void drainRetryQueue() {
        if (retryQueue.isEmpty()) return;
        List<PendingSpawn> pending = new ArrayList<>(retryQueue);
        retryQueue.clear();
        activeWorld.execute(() -> {
            Store<EntityStore> store = activeWorld.getEntityStore().getStore();
            for (PendingSpawn p : pending) {
                trySpawnOrEnqueue(p.state(), p.entry(), p.position(), store, p.attemptsLeft());
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
                Nexus.get().getWaveBarStateProvider().onMobKilled(state);
                break;
            }
        }
    }

    @Nullable
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

    private LevelTransitionService getLevelTransitionService() {
        if (levelTransitionService == null) {
            levelTransitionService = new LevelTransitionService(Nexus.get().getLevelManager());
        }
        return levelTransitionService;
    }
}
