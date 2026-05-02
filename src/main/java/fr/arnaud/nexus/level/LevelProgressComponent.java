package fr.arnaud.nexus.level;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.math.WorldPosition;

import javax.annotation.Nullable;
import java.util.*;

public class LevelProgressComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, LevelProgressComponent> componentType;

    public List<Integer> activatedSpawnerIndices = new ArrayList<>();
    public List<Integer> clearedSpawnerIndices = new ArrayList<>();

    public Map<Integer, List<String>> pendingSpawnerChestLoot = new HashMap<>();
    public Map<Integer, List<String>> pendingStandaloneChestLoot = new HashMap<>();

    private transient int[] decodingSpawnerChestCounts;
    private transient int[] decodingStandaloneChestCounts;

    public @Nullable WorldPosition lastCheckpointPosition;

    public LevelProgressComponent() {
    }

    public static final BuilderCodec<LevelProgressComponent> CODEC = BuilderCodec
        .builder(LevelProgressComponent.class, LevelProgressComponent::new)
        .append(
            new KeyedCodec<>("ActivatedSpawners", new ArrayCodec<>(Codec.INTEGER, Integer[]::new)),
            (c, v) -> c.activatedSpawnerIndices = v != null ? new ArrayList<>(Arrays.asList(v)) : new ArrayList<>(),
            c -> c.activatedSpawnerIndices.toArray(new Integer[0])
        )
        .add().append(new KeyedCodec<>("CheckpointX", Codec.FLOAT),
            (c, v) -> c.lastCheckpointPosition = mergeCheckpointX(c.lastCheckpointPosition, v),
            c -> c.lastCheckpointPosition != null ? (float) c.lastCheckpointPosition.x() : Float.NaN)
        .add().append(new KeyedCodec<>("CheckpointY", Codec.FLOAT),
            (c, v) -> c.lastCheckpointPosition = mergeCheckpointY(c.lastCheckpointPosition, v),
            c -> c.lastCheckpointPosition != null ? (float) c.lastCheckpointPosition.y() : Float.NaN)
        .add().append(new KeyedCodec<>("CheckpointZ", Codec.FLOAT),
            (c, v) -> c.lastCheckpointPosition = mergeCheckpointZ(c.lastCheckpointPosition, v),
            c -> c.lastCheckpointPosition != null ? (float) c.lastCheckpointPosition.z() : Float.NaN)
        .add().append(
            new KeyedCodec<>("ClearedSpawners", new ArrayCodec<>(Codec.INTEGER, Integer[]::new)),
            (c, v) -> c.clearedSpawnerIndices = v != null ? new ArrayList<>(Arrays.asList(v)) : new ArrayList<>(),
            c -> c.clearedSpawnerIndices.toArray(new Integer[0])
        )
        .add().append(
            new KeyedCodec<>("SpawnerChestKeys", new ArrayCodec<>(Codec.INTEGER, Integer[]::new)),
            (c, v) -> {
                if (v != null) for (Integer k : v) c.pendingSpawnerChestLoot.put(k, new ArrayList<>());
            },
            c -> lootKeys(c.pendingSpawnerChestLoot)
        )
        .add().append(
            new KeyedCodec<>("SpawnerChestCounts", new ArrayCodec<>(Codec.INTEGER, Integer[]::new)),
            (c, v) -> c.decodingSpawnerChestCounts = toIntArray(v),
            c -> lootCounts(c.pendingSpawnerChestLoot)
        )
        .add().append(
            new KeyedCodec<>("SpawnerChestItems", new ArrayCodec<>(Codec.STRING, String[]::new)),
            (c, v) -> fillLootMap(c.pendingSpawnerChestLoot, c.decodingSpawnerChestCounts, v),
            c -> lootItems(c.pendingSpawnerChestLoot)
        )
        .add().append(
            new KeyedCodec<>("StandaloneChestKeys", new ArrayCodec<>(Codec.INTEGER, Integer[]::new)),
            (c, v) -> {
                if (v != null) for (Integer k : v) c.pendingStandaloneChestLoot.put(k, new ArrayList<>());
            },
            c -> lootKeys(c.pendingStandaloneChestLoot)
        )
        .add().append(
            new KeyedCodec<>("StandaloneChestCounts", new ArrayCodec<>(Codec.INTEGER, Integer[]::new)),
            (c, v) -> c.decodingStandaloneChestCounts = toIntArray(v),
            c -> lootCounts(c.pendingStandaloneChestLoot)
        )
        .add().append(
            new KeyedCodec<>("StandaloneChestItems", new ArrayCodec<>(Codec.STRING, String[]::new)),
            (c, v) -> fillLootMap(c.pendingStandaloneChestLoot, c.decodingStandaloneChestCounts, v),
            c -> lootItems(c.pendingStandaloneChestLoot)
        )
        .add()
        .build();

    public void recordSpawnerActivated(int spawnerIndex) {
        if (!activatedSpawnerIndices.contains(spawnerIndex))
            activatedSpawnerIndices.add(spawnerIndex);
    }

    public void recordSpawnerChestLoot(int spawnerIndex, List<String> items) {
        pendingSpawnerChestLoot.put(spawnerIndex, new ArrayList<>(items));
    }

    public void clearSpawnerChestLoot(int spawnerIndex) {
        pendingSpawnerChestLoot.remove(spawnerIndex);
    }

    public void recordStandaloneChestLoot(int chestIndex, List<String> items) {
        pendingStandaloneChestLoot.put(chestIndex, new ArrayList<>(items));
    }

    public void clearStandaloneChestLoot(int chestIndex) {
        pendingStandaloneChestLoot.remove(chestIndex);
    }

    public boolean hasReachedCheckpoint() {
        return lastCheckpointPosition != null;
    }

    private static Integer[] lootKeys(Map<Integer, List<String>> map) {
        return map.keySet().toArray(new Integer[0]);
    }

    private static Integer[] lootCounts(Map<Integer, List<String>> map) {
        return map.values().stream().map(List::size).toArray(Integer[]::new);
    }

    private static String[] lootItems(Map<Integer, List<String>> map) {
        return map.values().stream().flatMap(List::stream).toArray(String[]::new);
    }

    private static int[] toIntArray(@Nullable Integer[] boxed) {
        if (boxed == null) return new int[0];
        int[] result = new int[boxed.length];
        for (int i = 0; i < boxed.length; i++) result[i] = boxed[i];
        return result;
    }

    private static void fillLootMap(Map<Integer, List<String>> map,
                                    int[] counts, @Nullable String[] items) {
        if (items == null || counts == null || counts.length == 0) return;
        List<Integer> keys = new ArrayList<>(map.keySet());
        int offset = 0;
        for (int i = 0; i < Math.min(keys.size(), counts.length); i++) {
            List<String> list = map.get(keys.get(i));
            for (int j = 0; j < counts[i] && offset < items.length; j++, offset++) {
                list.add(items[offset]);
            }
        }
    }

    private static WorldPosition mergeCheckpointX(@Nullable WorldPosition existing, float x) {
        if (Float.isNaN(x)) return null;
        return existing != null ? new WorldPosition(x, existing.y(), existing.z()) : new WorldPosition(x, 0, 0);
    }

    private static WorldPosition mergeCheckpointY(@Nullable WorldPosition existing, float y) {
        if (existing == null || Float.isNaN(y)) return existing;
        return new WorldPosition(existing.x(), y, existing.z());
    }

    private static WorldPosition mergeCheckpointZ(@Nullable WorldPosition existing, float z) {
        if (existing == null || Float.isNaN(z)) return existing;
        return new WorldPosition(existing.x(), existing.y(), z);
    }

    public static void setComponentType(ComponentType<EntityStore, LevelProgressComponent> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, LevelProgressComponent> getComponentType() {
        if (componentType == null) throw new IllegalStateException("LevelProgressComponent not registered.");
        return componentType;
    }

    @Override
    public LevelProgressComponent clone() {
        LevelProgressComponent clone = new LevelProgressComponent();
        clone.activatedSpawnerIndices.addAll(this.activatedSpawnerIndices);
        clone.clearedSpawnerIndices.addAll(this.clearedSpawnerIndices);
        clone.lastCheckpointPosition = this.lastCheckpointPosition;
        this.pendingSpawnerChestLoot.forEach((k, v) ->
            clone.pendingSpawnerChestLoot.put(k, new ArrayList<>(v)));
        this.pendingStandaloneChestLoot.forEach((k, v) ->
            clone.pendingStandaloneChestLoot.put(k, new ArrayList<>(v)));
        return clone;
    }
}
