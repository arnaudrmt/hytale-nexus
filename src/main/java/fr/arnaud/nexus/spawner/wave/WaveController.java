package fr.arnaud.nexus.spawner.wave;

import fr.arnaud.nexus.level.LevelConfig;
import fr.arnaud.nexus.level.LevelProgressComponent;
import fr.arnaud.nexus.math.WorldPosition;
import fr.arnaud.nexus.spawner.SpawnerState;
import fr.arnaud.nexus.spawner.WaveBarStateProvider;

import javax.annotation.Nullable;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Drives the wave finite-state machine for a single spawner.
 */
public final class WaveController {

    private final WaveEventSink eventSink;
    private final WaveBarStateProvider waveBarStateProvider;

    public WaveController(WaveEventSink eventSink, WaveBarStateProvider waveBarStateProvider) {
        this.eventSink = eventSink;
        this.waveBarStateProvider = waveBarStateProvider;
    }

    public void activateSpawner(SpawnerState state) {
        if (state.getConfig().hasWaves()) {
            advanceToWave(state, 1);
        } else {
            spawnAllMobsForWave(state, 0);
        }
    }

    public void tick(SpawnerState state, float dt,
                     @Nullable LevelProgressComponent progress,
                     ProgressWriter progressWriter) {
        tickSpawnStagger(state, dt);

        if (!state.getConfig().hasWaves()) return;

        if (hasPendingSpawns(state)) return;

        int currentWave = state.getActiveWave();
        LevelConfig.Wave nextWave = findWaveConfig(state, currentWave + 1);

        if (nextWave == null) {
            checkFinalWaveCompletion(state, progress, progressWriter);
            return;
        }

        switch (nextWave.type()) {
            case TIME -> tickTimeWaveTransition(state, nextWave, dt);
            case KILL -> tickKillWaveTransition(state, nextWave, dt);
        }
    }

    private void checkFinalWaveCompletion(SpawnerState state,
                                          @Nullable LevelProgressComponent progress,
                                          ProgressWriter progressWriter) {
        if (state.isChestSpawned()) return;
        if (state.getAliveMobsInCurrentWave() > 0) return;
        if (state.getTotalMobsInCurrentWave() == 0) return;
        if (!state.getConfig().hasLootChest()) return;

        state.markChestSpawned();
        waveBarStateProvider.onWaveStarted(state);
        eventSink.onChestRequested(state);

        if (progress != null) {
            WorldPosition pos = state.getConfig().position();
            progress.lastCheckpointPosition = new WorldPosition(pos.x(), pos.y(), pos.z() + 1.5f);
            progressWriter.write(progress);
        }
    }

    private void tickTimeWaveTransition(SpawnerState state, LevelConfig.Wave nextWave, float dt) {
        state.addWaveTimer(dt);
        if (state.getWaveTimer() >= nextWave.value()) {
            advanceToWave(state, nextWave.wave());
        }
    }

    private void tickKillWaveTransition(SpawnerState state, LevelConfig.Wave nextWave, float dt) {
        int total = state.getTotalMobsInCurrentWave();
        if (total == 0) return;

        int alive = state.getAliveMobsInCurrentWave();
        float killedRatio = (total - alive) / (float) total;

        boolean thresholdReached = killedRatio >= nextWave.value();
        boolean timedOut = nextWave.timeout() > 0f
            && state.getKillWaveTimeoutTimer() >= nextWave.timeout();

        if (thresholdReached || timedOut) {
            advanceToWave(state, nextWave.wave());
            return;
        }

        state.addKillWaveTimeoutTimer(dt);
    }

    private void advanceToWave(SpawnerState state, int waveIndex) {
        state.resetForNewWave();
        state.setActiveWave(waveIndex);
        spawnAllMobsForWave(state, waveIndex);
        waveBarStateProvider.onWaveStarted(state);
    }

    private void spawnAllMobsForWave(SpawnerState state, int waveIndex) {
        var entries = state.getConfig().mobs();
        int totalForWave = 0;

        for (int i = 0; i < entries.size(); i++) {
            LevelConfig.MobEntry entry = entries.get(i);
            if (entry.wave() != waveIndex) continue;

            int count = rollCount(entry);
            totalForWave += count;
            state.setPendingSpawns(i, count);
        }

        state.setTotalMobsInCurrentWave(state.getTotalMobsInCurrentWave() + totalForWave);
        state.setAliveMobsInCurrentWave(state.getAliveMobsInCurrentWave() + totalForWave);
    }

    private void tickSpawnStagger(SpawnerState state, float dt) {
        var entries = state.getConfig().mobs();

        for (int i = 0; i < entries.size(); i++) {
            LevelConfig.MobEntry entry = entries.get(i);
            int pending = state.getPendingSpawns(i);
            if (pending <= 0) continue;

            state.addSpawnRateAccumulator(i, dt);

            if (state.getSpawnRateAccumulator(i) >= entry.spawnStaggerInterval()) {
                state.resetSpawnRateAccumulator(i);
                eventSink.onMobsRequested(state, entry, 1);
                state.decrementPendingSpawns(i);
            }
        }
    }

    private static boolean hasPendingSpawns(SpawnerState state) {
        var entries = state.getConfig().mobs();
        for (int i = 0; i < entries.size(); i++) {
            if (state.getPendingSpawns(i) > 0) return true;
        }
        return false;
    }

    @Nullable
    private static LevelConfig.Wave findWaveConfig(SpawnerState state, int waveIndex) {
        for (LevelConfig.Wave wc : state.getConfig().waves()) {
            if (wc.wave() == waveIndex) return wc;
        }
        return null;
    }

    private static int rollCount(LevelConfig.MobEntry entry) {
        if (entry.minCount() == entry.maxCount()) return entry.minCount();
        return entry.minCount()
            + ThreadLocalRandom.current().nextInt(entry.maxCount() - entry.minCount() + 1);
    }

    /**
     * Functional interface used to persist checkpoint progress without coupling
     * {@link WaveController} to a CommandBuffer or player ref.
     */
    @FunctionalInterface
    public interface ProgressWriter {
        void write(LevelProgressComponent progress);
    }
}
