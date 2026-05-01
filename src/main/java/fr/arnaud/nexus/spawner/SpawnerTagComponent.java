package fr.arnaud.nexus.spawner;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class SpawnerTagComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, SpawnerTagComponent> componentType;

    private int spawnerId;
    private int minEssence;
    private int maxEssence;

    public SpawnerTagComponent() {
    }

    public SpawnerTagComponent(int spawnerId, int minEssence, int maxEssence) {
        this.spawnerId = spawnerId;
        this.minEssence = minEssence;
        this.maxEssence = maxEssence;
    }

    public int getSpawnerId() {
        return spawnerId;
    }

    public int getMinEssence() {
        return minEssence;
    }

    public int getMaxEssence() {
        return maxEssence;
    }

    public static final BuilderCodec<SpawnerTagComponent> CODEC = BuilderCodec
        .builder(SpawnerTagComponent.class, SpawnerTagComponent::new)
        .append(
            new KeyedCodec<>("SpawnerId", Codec.INTEGER),
            (c, v) -> c.spawnerId = v,
            c -> c.spawnerId
        )
        .add()
        .append(
            new KeyedCodec<>("MinEssence", Codec.INTEGER),
            (c, v) -> c.minEssence = v,
            c -> c.minEssence
        )
        .add()
        .append(
            new KeyedCodec<>("MaxEssence", Codec.INTEGER),
            (c, v) -> c.maxEssence = v,
            c -> c.maxEssence
        )
        .add().build();

    public static void setComponentType(ComponentType<EntityStore, SpawnerTagComponent> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, SpawnerTagComponent> getComponentType() {
        if (componentType == null) throw new IllegalStateException("SpawnerTagComponent not registered.");
        return componentType;
    }

    @Override
    public SpawnerTagComponent clone() {
        return new SpawnerTagComponent(spawnerId, minEssence, maxEssence);
    }
}
