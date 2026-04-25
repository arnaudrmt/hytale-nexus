package fr.arnaud.nexus.spawner;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.RespawnSystems;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.component.RunSessionComponent;
import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.level.LevelConfig;
import fr.arnaud.nexus.level.LevelProgressComponent;
import org.jetbrains.annotations.NotNull;

public final class PlayerRespawnSystem extends RespawnSystems.OnRespawnSystem {

    @Override
    public Query<EntityStore> getQuery() {
        return Player.getComponentType();
    }

    @Override
    public void onComponentAdded(@NotNull Ref<EntityStore> ref, @NotNull DeathComponent component, @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> commandBuffer) {

        RunSessionComponent session = store.getComponent(ref, RunSessionComponent.getComponentType());
        if (session != null) {
            session.incrementDeathCount();
        }
    }

    @Override
    public void onComponentRemoved(Ref<EntityStore> ref, DeathComponent component,
                                   Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {
        LevelProgressComponent progress = store.getComponent(ref, LevelProgressComponent.getComponentType());
        Vector3d respawnPos = resolveRespawnPosition(progress);

        commandBuffer.run(s -> s.addComponent(ref, Teleport.getComponentType(),
            Teleport.createForPlayer(respawnPos, Vector3f.FORWARD)));
    }

    private Vector3d resolveRespawnPosition(LevelProgressComponent progress) {
        if (progress != null && progress.hasCheckpoint()) {
            return new Vector3d(progress.checkpointX, progress.checkpointY, progress.checkpointZ);
        }

        LevelConfig config = Nexus.get().getLevelManager().getCurrentConfig();
        if (config != null) {
            LevelConfig.Position spawn = config.getSpawnPoint();
            return new Vector3d(spawn.getX(), spawn.getY(), spawn.getZ());
        }

        return new Vector3d(0.5, 80.0, 0.5);
    }


}
