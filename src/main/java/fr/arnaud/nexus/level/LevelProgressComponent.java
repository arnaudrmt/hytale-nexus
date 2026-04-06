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

    public LevelProgressComponent() {
    }

    public static final BuilderCodec<LevelProgressComponent> CODEC = BuilderCodec
        .builder(LevelProgressComponent.class, LevelProgressComponent::new)
        .append(
            new KeyedCodec<>("TriggeredSpawners", new ArrayCodec<>(Codec.INTEGER, size -> new Integer[size])),
            (component, value) -> component.triggeredSpawners = value != null ? new ArrayList<>(Arrays.asList(value)) : new ArrayList<>(),
            component -> component.triggeredSpawners.toArray(new Integer[0])
        )
        .add().build();

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
        return clone;
    }
}
