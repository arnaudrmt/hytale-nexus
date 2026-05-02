package fr.arnaud.nexus.spawner.system;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.RespawnSystems;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.camera.CameraPacketBuilder;
import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.level.LevelConfig;
import fr.arnaud.nexus.level.LevelProgressComponent;
import fr.arnaud.nexus.math.WorldPosition;
import fr.arnaud.nexus.session.RunSessionComponent;
import org.jetbrains.annotations.NotNull;

public final class PlayerRespawnSystem extends RespawnSystems.OnRespawnSystem {

    @Override
    public Query<EntityStore> getQuery() {
        return Player.getComponentType();
    }

    @Override
    public void onComponentAdded(@NotNull Ref<EntityStore> ref, @NotNull DeathComponent component,
                                 @NotNull Store<EntityStore> store,
                                 @NotNull CommandBuffer<EntityStore> commandBuffer) {
        RunSessionComponent session = store.getComponent(ref, RunSessionComponent.getComponentType());
        if (session != null) {
            session.incrementDeathCount();
            session.setEssenceDustSnapshot(
                Nexus.getInstance().getPlayerStatsManager().getEssenceDust(ref, store)
            );
        }
    }

    @Override
    public void onComponentRemoved(@NotNull Ref<EntityStore> ref, @NotNull DeathComponent component,
                                   @NotNull Store<EntityStore> store,
                                   CommandBuffer<EntityStore> commandBuffer) {
        commandBuffer.run(s -> {
            LevelProgressComponent progress = s.getComponent(ref, LevelProgressComponent.getComponentType());
            Vector3d respawnPos = resolveRespawnPosition(progress);

            s.addComponent(ref, Teleport.getComponentType(),
                Teleport.createForPlayer(new Transform(
                    respawnPos,
                    new Vector3f(0f, CameraPacketBuilder.ISO_CAMERA_YAW_RAD, 0f)
                )));

            RunSessionComponent session = s.getComponent(ref, RunSessionComponent.getComponentType());
            if (session == null) return;
            Nexus.getInstance().getPlayerStatsManager()
                 .addEssenceDust(ref, s, session.getEssenceDustSnapshot());
        });
    }

    private Vector3d resolveRespawnPosition(LevelProgressComponent progress) {
        if (progress != null && progress.hasReachedCheckpoint() && progress.lastCheckpointPosition != null) {
            return new Vector3d(
                progress.lastCheckpointPosition.x(),
                progress.lastCheckpointPosition.y(),
                progress.lastCheckpointPosition.z()
            );
        }

        LevelConfig currentConfig = Nexus.getInstance().getLevelWorldService().getActiveLevelConfig();
        if (currentConfig != null) {
            WorldPosition spawn = currentConfig.spawnPoint();
            return new Vector3d(spawn.x(), spawn.y(), spawn.z());
        }

        return new Vector3d(0.5, 80.0, 0.5);
    }
}
