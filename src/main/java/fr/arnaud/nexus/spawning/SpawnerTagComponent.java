package fr.arnaud.nexus.spawning;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class SpawnerTagComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, SpawnerTagComponent> TYPE;
    private int spawnerId;

    public SpawnerTagComponent() {
    }

    public SpawnerTagComponent(int spawnerId) {
        this.spawnerId = spawnerId;
    }

    public int getSpawnerId() {
        return spawnerId;
    }

    public static final BuilderCodec<SpawnerTagComponent> CODEC = BuilderCodec
        .builder(SpawnerTagComponent.class, SpawnerTagComponent::new)
        .append(
            new KeyedCodec<>("SpawnerId", Codec.INTEGER),
            (component, value) -> component.spawnerId = value,
            component -> component.spawnerId
        )
        .add().build();

    public static void setComponentType(ComponentType<EntityStore, SpawnerTagComponent> type) {
        TYPE = type;
    }

    public static ComponentType<EntityStore, SpawnerTagComponent> getComponentType() {
        return TYPE;
    }

    @Override
    public SpawnerTagComponent clone() {
        return new SpawnerTagComponent(this.spawnerId);
    }
}
