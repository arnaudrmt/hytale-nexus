package fr.arnaud.nexus.spawner;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class WaveBarStateProvider {

    public record WaveSnapshot(int spawnerId, int currentWave, int totalWaves,
                               int killed, int total, boolean completed) {
    }

    private final Map<Integer, WaveSnapshot> snapshots = new ConcurrentHashMap<>();

    public void onWaveStarted(SpawnerState state) {
        snapshots.put(state.getId(), buildSnapshot(state));
    }

    public void onMobKilled(SpawnerState state) {
        snapshots.put(state.getId(), buildSnapshot(state));
    }

    public void onLevelReset() {
        snapshots.clear();
    }

    @Nullable
    public WaveSnapshot getSnapshot(int spawnerId) {
        return snapshots.get(spawnerId);
    }

    private static WaveSnapshot buildSnapshot(SpawnerState state) {
        int alive = state.getAliveMobsInCurrentWave();
        int total = state.getTotalMobsInCurrentWave();
        return new WaveSnapshot(
            state.getId(),
            state.getActiveWave(),
            state.getTotalWaves(),
            total - alive,
            total,
            state.isComplete()
        );
    }
}
