package fr.arnaud.nexus.level;

import javax.annotation.Nullable;
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
    private final List<IndependentChestConfig> independentChests;
    private final String nextLevelId;

    public LevelConfig(String id, String name, float difficulty,
                       Position spawnPoint, Position finishPoint,
                       List<SpawnerConfig> spawners,
                       List<IndependentChestConfig> independentChests,
                       String nextLevelId) {
        this.id = id;
        this.name = name;
        this.difficulty = difficulty;
        this.spawnPoint = spawnPoint;
        this.finishPoint = finishPoint;
        this.spawners = List.copyOf(spawners);
        this.independentChests = List.copyOf(independentChests);
        this.nextLevelId = nextLevelId;
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

    public List<IndependentChestConfig> getIndependentChests() {
        return independentChests;
    }

    @Nullable
    public String getNextLevelId() {
        return nextLevelId;
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
        private final float triggerRadius;
        private final float spawnRadius;
        private final List<WaveConfig> waves;
        private final List<MobEntry> mobs;

        /**
         * Null when the spawner JSON omits the {@code lootChest} key — means no chest spawns.
         */
        @Nullable
        private final LootChestConfig lootChest;

        public SpawnerConfig(Position position, float triggerRadius, float spawnRadius,
                             List<WaveConfig> waves, List<MobEntry> mobs,
                             @Nullable LootChestConfig lootChest) {
            this.position = position;
            this.triggerRadius = triggerRadius;
            this.spawnRadius = spawnRadius;
            this.waves = (waves == null || waves.isEmpty()) ? List.of() : List.copyOf(waves);
            this.mobs = List.copyOf(mobs);
            this.lootChest = lootChest;
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

        @Nullable
        public LootChestConfig getLootChest() {
            return lootChest;
        }

        public boolean hasWaves() {
            return !waves.isEmpty();
        }

        public boolean hasLootChest() {
            return lootChest != null;
        }
    }

    // -------------------------------------------------------------------------

    public enum WaveType {TIME, KILL}

    public static final class WaveConfig {
        private final int wave;
        private final WaveType type;
        private final float value;
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
         * For {@code TIME}: seconds to wait. For {@code KILL}: kill percentile (0.0–1.0).
         */
        public float getValue() {
            return value;
        }

        /**
         * Seconds before a KILL wave force-spawns. {@code 0} disables the timeout.
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
        private final float spawnRate;
        private final int wave;

        /**
         * Minimum essence dust dropped when this mob dies. Rolled independently per kill.
         */
        private final int minEssence;

        /**
         * Maximum essence dust dropped when this mob dies. Rolled independently per kill.
         */
        private final int maxEssence;

        @Nullable
        private final MobLootConfig lootTable;

        public MobEntry(String mobId, int minCount, int maxCount,
                        float spawnRate, int wave,
                        int minEssence, int maxEssence,
                        @Nullable MobLootConfig lootTable) {
            this.mobId = mobId;
            this.minCount = minCount;
            this.maxCount = maxCount;
            this.spawnRate = spawnRate;
            this.wave = wave;
            this.minEssence = minEssence;
            this.maxEssence = maxEssence;
            this.lootTable = lootTable;
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

        public int getMinEssence() {
            return minEssence;
        }

        public int getMaxEssence() {
            return maxEssence;
        }

        public boolean hasWave() {
            return wave > 0;
        }

        @Nullable
        public MobLootConfig getLootTable() {
            return lootTable;
        }

        public boolean hasLootTable() {
            return lootTable != null && !lootTable.getItems().isEmpty();
        }
    }

    // -------------------------------------------------------------------------

    /**
     * Loot table attached to a spawner. Evaluated once when the spawner's final wave ends.
     * Each item rolls independently — a chest can contain any subset, including none.
     * If all rolls fail, the highest-chance item is guaranteed as a fallback.
     */
    public static class LootChestConfig {
        private final List<LootChestItem> items;

        public LootChestConfig(List<LootChestItem> items) {
            this.items = List.copyOf(items);
        }

        public List<LootChestItem> getItems() {
            return items;
        }
    }

    // -------------------------------------------------------------------------

    public static final class LootChestItem {
        private final String itemId;

        /**
         * Independent drop probability in [0.0, 1.0]. Each item is rolled separately.
         */
        private final float chance;

        public LootChestItem(String itemId, float chance) {
            this.itemId = itemId;
            this.chance = chance;
        }

        public String getItemId() {
            return itemId;
        }

        public float getChance() {
            return chance;
        }
    }

    public static final class IndependentChestConfig {
        private final Position position;
        private final float triggerRadius;
        private final List<LootChestItem> items;

        public IndependentChestConfig(Position position, float triggerRadius, List<LootChestItem> items) {
            this.position = position;
            this.triggerRadius = triggerRadius;
            this.items = List.copyOf(items);
        }

        public Position getPosition() {
            return position;
        }

        public float getTriggerRadius() {
            return triggerRadius;
        }

        public List<LootChestItem> getItems() {
            return items;
        }
    }

    public static final class MobLootConfig {
        private final List<MobLootItem> items;

        public MobLootConfig(List<MobLootItem> items) {
            this.items = List.copyOf(items);
        }

        public List<MobLootItem> getItems() {
            return items;
        }
    }

    public static final class MobLootItem {
        private final String itemId;
        private final float chance;

        public MobLootItem(String itemId, float chance) {
            this.itemId = itemId;
            this.chance = chance;
        }

        public String getItemId() {
            return itemId;
        }

        public float getChance() {
            return chance;
        }
    }
}
