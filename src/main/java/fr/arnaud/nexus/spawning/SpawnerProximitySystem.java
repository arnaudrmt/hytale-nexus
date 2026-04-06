package fr.arnaud.nexus.spawning;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.Nexus;
import fr.arnaud.nexus.level.LevelProgressComponent;

public final class SpawnerProximitySystem extends EntityTickingSystem<EntityStore> {

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
            PlayerRef.getComponentType(),
            TransformComponent.getComponentType()
        );
    }

    @Override
    public void tick(float dt, int index, ArchetypeChunk<EntityStore> chunk,
                     Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {

        TransformComponent transform = chunk.getComponent(index, TransformComponent.getComponentType());
        if (transform == null) return;
        if (!Nexus.get().getLevelManager().isLevelLoaded()) return;

        Ref<EntityStore> playerRef = chunk.getReferenceTo(index);
        LevelProgressComponent progress = chunk.getComponent(index, LevelProgressComponent.getComponentType());

        if (progress == null) {
            progress = new LevelProgressComponent();
            commandBuffer.putComponent(playerRef, LevelProgressComponent.getComponentType(), progress);
        }

        Vector3d position = transform.getPosition();
        Nexus.get().getMobSpawnManager().tick(dt, position, progress, commandBuffer, playerRef);
    }
}
