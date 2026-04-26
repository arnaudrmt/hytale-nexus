package fr.arnaud.nexus.spawner;

import com.hypixel.hytale.math.vector.Vector3d;
import fr.arnaud.nexus.level.LevelConfig;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mutable runtime state for a single spawner instance.
 *
 * <p>One {@code SpawnerState} is created per {@link LevelConfig.SpawnerConfig}
 * when a level loads. It tracks whether the spawner has been triggered, which
 * wave is currently active, timers, kill counts, and pending chest loot.
 * It holds no logic — all decisions are made by {@link MobSpawnerManager}.
 */
public final class SpawnerState {

    private final LevelConfig.SpawnerConfig config;
    private final int id;

    private boolean triggered = false;

    /**
     * Prevents the loot chest from being placed more than once per spawner lifetime.
     */
    private boolean chestSpawned = false;

    /**
     * Items rolled for the loot chest. Populated when the final wave ends.
     * Cleared once the player clicks the chest and items are ejected.
     */
    private List<String> pendingChestLoot = Collections.emptyList();

    /**
     * World position of the placed chest block. Set alongside {@code pendingChestLoot}.
     * Null until the chest has been spawned.
     */
    private Vector3d chestPosition = null;

    /**
     * The wave index currently being executed. {@code 0} means no wave system or pre-wave.
     */
    private int activeWave = 0;

    /**
     * Elapsed seconds since the current wave was spawned. Used for TIME wave transitions.
     */
    private float waveTimer = 0f;

    /**
     * Elapsed seconds since the current KILL wave started.
     * Used to enforce timeout. Reset to 0 when a new wave begins.
     */
    private float killWaveTimeoutTimer = 0f;

    private final Map<Integer, Integer> spawnedCountPerEntry = new HashMap<>();
    private int aliveMobsInCurrentWave = 0;
    private int totalMobsInCurrentWave = 0;
    private final Map<Integer, Float> spawnRateAccumulatorPerEntry = new HashMap<>();
    private final Map<Integer, Integer> pendingSpawnsPerEntry = new HashMap<>();

    public SpawnerState(int id, LevelConfig.SpawnerConfig config) {
        this.id = id;
        this.config = config;
    }

    public LevelConfig.SpawnerConfig getConfig() {
        return config;
    }

    public int getId() {
        return id;
    }

    public boolean isTriggered() {
        return triggered;
    }

    public void markTriggered() {
        this.triggered = true;
    }

    public boolean isChestSpawned() {
        return chestSpawned;
    }

    public void markChestSpawned() {
        this.chestSpawned = true;
    }

    public List<String> getPendingChestLoot() {
        return pendingChestLoot;
    }

    public void setPendingChestLoot(List<String> loot) {
        this.pendingChestLoot = loot;
    }

    public void clearPendingChestLoot() {
        this.pendingChestLoot = Collections.emptyList();
    }

    public boolean hasPendingChestLoot() {
        return !pendingChestLoot.isEmpty();
    }

    public Vector3d getChestPosition() {
        return chestPosition;
    }

    public void setChestPosition(Vector3d position) {
        this.chestPosition = position;
    }

    public int getActiveWave() {
        return activeWave;
    }

    public void setActiveWave(int wave) {
        this.activeWave = wave;
    }

    public int getTotalWaves() {
        return config.getWaves().size();
    }

    public boolean isComplete() {
        if (config.hasLootChest()) return chestSpawned;
        if (!triggered || totalMobsInCurrentWave == 0) return false;
        if (aliveMobsInCurrentWave > 0) return false;
        for (LevelConfig.WaveConfig wc : config.getWaves()) {
            if (wc.getWave() == activeWave + 1) return false;
        }
        return true;
    }

    public float getWaveTimer() {
        return waveTimer;
    }

    public void addWaveTimer(float dt) {
        this.waveTimer += dt;
    }

    public void resetWaveTimer() {
        this.waveTimer = 0f;
    }

    public float getKillWaveTimeoutTimer() {
        return killWaveTimeoutTimer;
    }

    public void addKillWaveTimeoutTimer(float dt) {
        this.killWaveTimeoutTimer += dt;
    }

    public void resetKillWaveTimeoutTimer() {
        this.killWaveTimeoutTimer = 0f;
    }

    public int getAliveMobsInCurrentWave() {
        return aliveMobsInCurrentWave;
    }

    public void setAliveMobsInCurrentWave(int n) {
        this.aliveMobsInCurrentWave = n;
    }

    public void decrementAliveMobs() {
        if (aliveMobsInCurrentWave > 0) aliveMobsInCurrentWave--;
    }

    public int getTotalMobsInCurrentWave() {
        return totalMobsInCurrentWave;
    }

    public void setTotalMobsInCurrentWave(int n) {
        this.totalMobsInCurrentWave = n;
    }

    public int getSpawnedCount(int entryIndex) {
        return spawnedCountPerEntry.getOrDefault(entryIndex, 0);
    }

    public void incrementSpawnedCount(int entryIndex) {
        spawnedCountPerEntry.merge(entryIndex, 1, Integer::sum);
    }

    public void clearSpawnedCounts() {
        spawnedCountPerEntry.clear();
    }

    public float getSpawnRateAccumulator(int entryIndex) {
        return spawnRateAccumulatorPerEntry.getOrDefault(entryIndex, 0f);
    }

    public void addSpawnRateAccumulator(int entryIndex, float dt) {
        spawnRateAccumulatorPerEntry.merge(entryIndex, dt, Float::sum);
    }

    public void resetSpawnRateAccumulator(int entryIndex) {
        spawnRateAccumulatorPerEntry.put(entryIndex, 0f);
    }

    public int getPendingSpawns(int entryIndex) {
        return pendingSpawnsPerEntry.getOrDefault(entryIndex, 0);
    }

    public void setPendingSpawns(int entryIndex, int count) {
        pendingSpawnsPerEntry.put(entryIndex, count);
    }

    public void decrementPendingSpawns(int entryIndex) {
        int current = getPendingSpawns(entryIndex);
        if (current > 0) pendingSpawnsPerEntry.put(entryIndex, current - 1);
    }

    public void resetForNewWave() {
        clearSpawnedCounts();
        spawnRateAccumulatorPerEntry.clear();
        pendingSpawnsPerEntry.clear();
        resetWaveTimer();
        resetKillWaveTimeoutTimer();
        aliveMobsInCurrentWave = 0;
        totalMobsInCurrentWave = 0;
    }
}
