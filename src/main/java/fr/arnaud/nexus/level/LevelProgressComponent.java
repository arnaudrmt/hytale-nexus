package fr.arnaud.nexus.level;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LevelProgressComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, LevelProgressComponent> TYPE;

    public List<Integer> triggeredSpawners = new ArrayList<>();

    public float checkpointX = Float.NaN;
    public float checkpointY = Float.NaN;
    public float checkpointZ = Float.NaN;

    public LevelProgressComponent() {
    }

    public static final BuilderCodec<LevelProgressComponent> CODEC = BuilderCodec
        .builder(LevelProgressComponent.class, LevelProgressComponent::new)
        .append(
            new KeyedCodec<>("TriggeredSpawners", new ArrayCodec<>(Codec.INTEGER, size -> new Integer[size])),
            (component, value) -> component.triggeredSpawners = value != null ? new ArrayList<>(Arrays.asList(value)) : new ArrayList<>(),
            component -> component.triggeredSpawners.toArray(new Integer[0])
        )
        .add().append(new KeyedCodec<>("CheckpointX", Codec.FLOAT), (c, v) -> c.checkpointX = v, c -> c.checkpointX)
        .add().append(new KeyedCodec<>("CheckpointY", Codec.FLOAT), (c, v) -> c.checkpointY = v, c -> c.checkpointY)
        .add().append(new KeyedCodec<>("CheckpointZ", Codec.FLOAT), (c, v) -> c.checkpointZ = v, c -> c.checkpointZ)
        .add().build();

    public boolean hasCheckpoint() {
        return !Float.isNaN(checkpointX);
    }

    public static void setComponentType(ComponentType<EntityStore, LevelProgressComponent> type) {
        TYPE = type;
    }

    public static ComponentType<EntityStore, LevelProgressComponent> getComponentType() {
        return TYPE;
    }

    @Override
    public LevelProgressComponent clone() {
        LevelProgressComponent clone = new LevelProgressComponent();
        clone.triggeredSpawners.addAll(this.triggeredSpawners);
        clone.checkpointX = this.checkpointX;
        clone.checkpointY = this.checkpointY;
        clone.checkpointZ = this.checkpointZ;
        return clone;
    }
}
