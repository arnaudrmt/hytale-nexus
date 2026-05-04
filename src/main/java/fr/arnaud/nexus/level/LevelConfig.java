package fr.arnaud.nexus.level;

import fr.arnaud.nexus.math.WorldPosition;

import javax.annotation.Nullable;
import java.util.List;

public record LevelConfig(String id, String key_name, WorldPosition spawnPoint, WorldPosition finishPoint,
                          List<Spawner> spawners, List<StandaloneChest> standaloneChests, String instanceTemplate) {

    public LevelConfig(String id, String key_name, WorldPosition spawnPoint, WorldPosition finishPoint,
                       List<Spawner> spawners, List<StandaloneChest> standaloneChests, String instanceTemplate) {
        this.id = id;
        this.key_name = key_name;
        this.spawnPoint = spawnPoint;
        this.finishPoint = finishPoint;
        this.spawners = List.copyOf(spawners);
        this.standaloneChests = List.copyOf(standaloneChests);
        this.instanceTemplate = instanceTemplate;
    }

    /**
     * @param lootChest Null means no chest spawns after this spawner clears.
     */
    public record Spawner(WorldPosition position, float activationRadius, float spawnRadius, List<Wave> waves,
                          List<MobEntry> mobs, @Nullable LootChest lootChest) {
        public Spawner(WorldPosition position, float activationRadius, float spawnRadius,
                       List<Wave> waves, List<MobEntry> mobs, @Nullable LootChest lootChest) {
            this.position = position;
            this.activationRadius = activationRadius;
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

    public record Wave(int wave, WaveType type, float value, float timeout) {
    }

    /**
     * @param spawnStaggerInterval Seconds between individual mob spawns within a batch. Sourced from
     *                             the level JSON or the index defaults if omitted.
     * @param minEssence           Minimum essence dust dropped when this mob dies.
     * @param maxEssence           Maximum essence dust dropped when this mob dies.
     */
    public record MobEntry(String mobId, int minCount, int maxCount, float spawnStaggerInterval, int wave,
                           int minEssence, int maxEssence, @Nullable MobLoot lootTable) {
    }

    public static class LootChest {
        private final List<LootEntry> items;

        public LootChest(List<LootEntry> items) {
            this.items = List.copyOf(items);
        }

        public List<LootEntry> getItems() {
            return items;
        }
    }

    public record LootEntry(String itemId, float chance) {
    }

    public record StandaloneChest(WorldPosition position, float activationRadius, List<LootEntry> items) {
        public StandaloneChest(WorldPosition position, float activationRadius, List<LootEntry> items) {
            this.position = position;
            this.activationRadius = activationRadius;
            this.items = List.copyOf(items);
        }
    }

    public record MobLoot(List<MobLootEntry> items) {
        public MobLoot(List<MobLootEntry> items) {
            this.items = List.copyOf(items);
        }
    }

    public record MobLootEntry(String itemId, float chance) {
    }
}
