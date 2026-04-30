package fr.arnaud.nexus.level;

import com.hypixel.hytale.builtin.instances.InstancesPlugin;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import fr.arnaud.nexus.camera.CameraPacketBuilder;
import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.session.RunCompletedEvent;
import fr.arnaud.nexus.session.RunSessionComponent;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public final class LevelTransitionService {

    private final LevelManager levelManager;

    public LevelTransitionService(@NonNullDecl LevelManager levelManager) {
        this.levelManager = levelManager;
    }

    public void onPortalEntered(@NonNullDecl Ref<EntityStore> playerRef,
                                @NonNullDecl CommandBuffer<EntityStore> cmd,
                                @NonNullDecl World currentWorld) {
        RunSessionComponent session = cmd.getComponent(playerRef, RunSessionComponent.getComponentType());
        if (session != null) {
            session.pauseSession();
            session.recordLevelSplit();
            cmd.run(s -> s.putComponent(playerRef, RunSessionComponent.getComponentType(), session));
        }

        cmd.run(_ -> {
            LevelProgressComponent progress = new LevelProgressComponent();
            cmd.putComponent(playerRef, LevelProgressComponent.getComponentType(), progress);
        });

        @Nullable String nextLevelId = levelManager.getCurrentConfig().getNextLevelId();

        if (nextLevelId == null) {
            completeRun(playerRef, session);
            return;
        }

        currentWorld.execute(() -> {
            var store = currentWorld.getEntityStore().getStore();
            NotificationUtil.sendNotificationToWorld(
                Message.translation("nexus.level.loading"),
                null, null, null,
                NotificationStyle.Default,
                store
            );
        });
        loadNextLevel(playerRef, currentWorld, nextLevelId);
    }

    private void loadNextLevel(Ref<EntityStore> playerRef,
                               World currentWorld,
                               String nextLevelId) {
        NexusWorldLoadSystem worldLoadSystem = Nexus.get().getNexusWorldLoadSystem();

        World preloaded = worldLoadSystem.takePreloadedWorld(nextLevelId);
        CompletableFuture<World> nextWorldFuture = preloaded != null
            ? CompletableFuture.completedFuture(preloaded)
            : worldLoadSystem.spawnLevelWorld(nextLevelId, currentWorld);

        nextWorldFuture.thenAccept(nextWorld -> {
            if (nextWorld == null) return;

            String levelId = nextWorld.getName().substring(
                NexusWorldLoadSystem.LEVEL_WORLD_KEY_PREFIX.length());
            LevelConfig nextConfig = LevelConfigLoader.loadAndParseLevelConfig(levelId);
            if (nextConfig == null) return;

            LevelConfig.Position spawn = nextConfig.getSpawnPoint();
            Transform spawnTransform = new Transform(
                new Vector3d(spawn.getX(), spawn.getY(), spawn.getZ()),
                new Vector3f(0f, CameraPacketBuilder.ISO_YAW_RAD, 0f)
            );

            Nexus.get().getNexusWorldLoadSystem().activateLevelWorld(nextWorld, levelId);

            currentWorld.execute(() ->
                InstancesPlugin.teleportPlayerToLoadingInstance(
                    playerRef,
                    playerRef.getStore(),
                    CompletableFuture.completedFuture(nextWorld),
                    spawnTransform
                )
            );
        });
    }

    private void completeRun(@NonNullDecl Ref<EntityStore> playerRef,
                             @Nullable RunSessionComponent session) {
        if (session == null) return;
        RunCompletedEvent.dispatch(playerRef, session.clone());
    }
}
