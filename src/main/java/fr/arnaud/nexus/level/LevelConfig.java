package fr.arnaud.nexus.level;

import java.util.List;

/**
 * Immutable data model representing a single level's static configuration.
 * Parsed once from JSON at world load time. Never modified at runtime.
 */
public final class LevelConfig {

    private final String id;
    private final String name;
    private final float difficulty;
    private final Position spawnPoint;
    private final Position finishPoint;
    private final List<SpawnerConfig> spawners;

    public LevelConfig(String id, String name, float difficulty,
                       Position spawnPoint, Position finishPoint,
                       List<SpawnerConfig> spawners) {
        this.id = id;
        this.name = name;
        this.difficulty = difficulty;
        this.spawnPoint = spawnPoint;
        this.finishPoint = finishPoint;
        this.spawners = List.copyOf(spawners);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public float getDifficulty() {
        return difficulty;
    }

    public Position getSpawnPoint() {
        return spawnPoint;
    }

    public Position getFinishPoint() {
        return finishPoint;
    }

    public List<SpawnerConfig> getSpawners() {
        return spawners;
    }

    // -------------------------------------------------------------------------

    public static final class Position {
        private final double x;
        private final double y;
        private final double z;

        public Position(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getZ() {
            return z;
        }

        @Override
        public String toString() {
            return "(" + x + ", " + y + ", " + z + ")";
        }
    }

    // -------------------------------------------------------------------------

    public static final class SpawnerConfig {
        private final Position position;

        /**
         * Distance in blocks at which a player triggers this spawner.
         */
        private final float triggerRadius;

        /**
         * Max radius in blocks for random mob scatter around the spawner position.
         */
        private final float spawnRadius;

        private final List<WaveConfig> waves;
        private final List<MobEntry> mobs;

        /**
         * @param waves may be {@code null} or empty — means no wave system, all mobs spawn at once
         */
        public SpawnerConfig(Position position, float triggerRadius, float spawnRadius,
                             List<WaveConfig> waves, List<MobEntry> mobs) {
            this.position = position;
            this.triggerRadius = triggerRadius;
            this.spawnRadius = spawnRadius;
            this.waves = (waves == null || waves.isEmpty()) ? List.of() : List.copyOf(waves);
            this.mobs = List.copyOf(mobs);
        }

        public Position getPosition() {
            return position;
        }

        public float getTriggerRadius() {
            return triggerRadius;
        }

        public float getSpawnRadius() {
            return spawnRadius;
        }

        public List<MobEntry> getMobs() {
            return mobs;
        }

        public List<WaveConfig> getWaves() {
            return waves;
        }

        /**
         * Returns {@code true} if this spawner uses a wave system.
         */
        public boolean hasWaves() {
            return !waves.isEmpty();
        }
    }

    // -------------------------------------------------------------------------

    public enum WaveType {
        TIME,
        KILL
    }

    public static final class WaveConfig {
        private final int wave;
        private final WaveType type;
        private final float value;

        /**
         * Timeout before force-spawning this wave regardless of kill condition.
         * Only meaningful when {@code type == KILL}. {@code 0} means no timeout.
         */
        private final float timeout;

        public WaveConfig(int wave, WaveType type, float value, float timeout) {
            this.wave = wave;
            this.type = type;
            this.value = value;
            this.timeout = timeout;
        }

        public int getWave() {
            return wave;
        }

        public WaveType getType() {
            return type;
        }

        /**
         * For {@code TIME}: seconds to wait after the previous wave spawned.
         * For {@code KILL}: percentile (0.0–1.0) of the previous wave that must die before this one triggers.
         */
        public float getValue() {
            return value;
        }

        /**
         * Seconds before this wave force-spawns regardless of kill percentage.
         * {@code 0} disables the timeout. Only applies to {@code KILL} type waves.
         */
        public float getTimeout() {
            return timeout;
        }
    }

    // -------------------------------------------------------------------------

    public static final class MobEntry {
        private final String mobId;
        private final int minCount;
        private final int maxCount;

        /**
         * Seconds between each individual mob spawn within this entry.
         * {@code 0} (or absent in JSON) spawns all at once.
         */
        private final float spawnRate;

        /**
         * Wave index this mob belongs to. {@code 0} (or absent in JSON) means
         * no wave system — spawn immediately with the spawner.
         */
        private final int wave;

        public MobEntry(String mobId, int minCount, int maxCount, float spawnRate, int wave) {
            this.mobId = mobId;
            this.minCount = minCount;
            this.maxCount = maxCount;
            this.spawnRate = spawnRate;
            this.wave = wave;
        }

        public String getMobId() {
            return mobId;
        }

        public int getMinCount() {
            return minCount;
        }

        public int getMaxCount() {
            return maxCount;
        }

        public float getSpawnRate() {
            return spawnRate;
        }

        public int getWave() {
            return wave;
        }

        /**
         * Returns {@code true} if this mob belongs to a named wave.
         */
        public boolean hasWave() {
            return wave > 0;
        }
    }
}
