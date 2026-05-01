package fr.arnaud.nexus.level;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public record LevelConfig(String id, String name, float difficulty, Position spawnPoint, Position finishPoint,
                          List<SpawnerConfig> spawners, List<IndependentChestConfig> independentChests,
                          String nextLevelId, String instanceTemplate) {

    public LevelConfig(String id, String name, float difficulty,
                       Position spawnPoint, Position finishPoint,
                       List<SpawnerConfig> spawners,
                       List<IndependentChestConfig> independentChests,
                       String nextLevelId, String instanceTemplate) {
        this.id = id;
        this.name = name;
        this.difficulty = difficulty;
        this.spawnPoint = spawnPoint;
        this.finishPoint = finishPoint;
        this.spawners = List.copyOf(spawners);
        this.independentChests = List.copyOf(independentChests);
        this.nextLevelId = nextLevelId;
        this.instanceTemplate = instanceTemplate;
    }

    @Override
    @Nullable
    public String nextLevelId() {
        return nextLevelId;
    }

    public record Position(double x, double y, double z) {

        @NotNull
        @Override
        public String toString() {
            return "(" + x + ", " + y + ", " + z + ")";
        }
    }

    /**
     * @param lootChest Null means no chest spawns.
     */
    public record SpawnerConfig(Position position, float triggerRadius, float spawnRadius, List<WaveConfig> waves,
                                List<MobEntry> mobs, @Nullable LootChestConfig lootChest) {
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

        public boolean hasWaves() {
            return !waves.isEmpty();
        }

        public boolean hasLootChest() {
            return lootChest != null;
        }
    }

    public enum WaveType {TIME, KILL}

    public record WaveConfig(int wave, WaveType type, float value, float timeout) {
    }

    /**
     * @param minEssence Minimum essence dust dropped when this mob dies.
     * @param maxEssence Maximum essence dust dropped when this mob dies.
     */
    public record MobEntry(String mobId, int minCount, int maxCount, float spawnRate, int wave, int minEssence,
                           int maxEssence, @Nullable MobLootConfig lootTable) {
    }

    public static class LootChestConfig {
        private final List<LootChestItem> items;

        public LootChestConfig(List<LootChestItem> items) {
            this.items = List.copyOf(items);
        }

        public List<LootChestItem> getItems() {
            return items;
        }
    }

    public record LootChestItem(String itemId, float chance) {
    }

    public record IndependentChestConfig(Position position, float triggerRadius, List<LootChestItem> items) {
        public IndependentChestConfig(Position position, float triggerRadius, List<LootChestItem> items) {
            this.position = position;
            this.triggerRadius = triggerRadius;
            this.items = List.copyOf(items);
        }
    }

    public record MobLootConfig(List<MobLootItem> items) {
        public MobLootConfig(List<MobLootItem> items) {
            this.items = List.copyOf(items);
        }
    }

    public record MobLootItem(String itemId, float chance) {
    }
}
