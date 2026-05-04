package fr.arnaud.nexus.level;

public record LevelDefaults(
    float activationRadius,
    float spawnRadius,
    float spawnStaggerInterval,
    float waveTimeoutSeconds,
    float killWaveThreshold,
    float timeWaveInterval
) {
    public static LevelDefaults fallback() {
        return new LevelDefaults(8.0f, 4.0f, 0.3f, 30.0f, 0.8f, 10.0f);
    }
}
