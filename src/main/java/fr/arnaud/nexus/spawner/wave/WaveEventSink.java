package fr.arnaud.nexus.spawner.wave;

import fr.arnaud.nexus.level.LevelConfig;
import fr.arnaud.nexus.spawner.SpawnerState;

/**
 * Receives spawn and chest requests from {@link WaveController}, keeping the wave FSM decoupled from execution.
 */
public interface WaveEventSink {

    void onMobsRequested(SpawnerState state, LevelConfig.MobEntry entry, int count);

    void onChestRequested(SpawnerState state);
}
