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
import fr.arnaud.nexus.math.WorldPosition;
import fr.arnaud.nexus.session.RunCompletedEvent;
import fr.arnaud.nexus.session.RunSessionComponent;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public final class LevelTransitionService {

    public LevelTransitionService() {
    }

    public void handlePortalEntered(@NonNullDecl Ref<EntityStore> playerRef,
                                    @NonNullDecl CommandBuffer<EntityStore> cmd,
                                    @NonNullDecl World currentWorld) {
        RunSessionComponent session = cmd.getComponent(playerRef, RunSessionComponent.getComponentType());
        if (session != null) {
            session.pauseSession();
            session.recordLevelSplit();
            cmd.run(s -> s.putComponent(playerRef, RunSessionComponent.getComponentType(), session));
        }

        cmd.run(s -> {
            LevelProgressComponent progress = new LevelProgressComponent();
            cmd.putComponent(playerRef, LevelProgressComponent.getComponentType(), progress);
        });

        String activeLevelId = Nexus.getInstance().getLevelWorldService()
                                    .getCurrentLevelWorld().getName()
                                    .substring(LevelWorldService.LEVEL_WORLD_KEY_PREFIX.length());

        @Nullable String nextLevelId = LevelRegistry.getInstance().getNextLevelId(activeLevelId);

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
        LevelWorldService worldLoadSystem = Nexus.getInstance().getLevelWorldService();

        World preloaded = worldLoadSystem.consumePreloadedWorld(nextLevelId);
        CompletableFuture<World> nextWorldFuture = preloaded != null
            ? CompletableFuture.completedFuture(preloaded)
            : worldLoadSystem.getOrCreateLevelWorld(nextLevelId, currentWorld);

        nextWorldFuture.thenAccept(nextWorld -> {
            if (nextWorld == null) return;

            String levelId = nextWorld.getName().substring(
                LevelWorldService.LEVEL_WORLD_KEY_PREFIX.length());
            LevelConfig nextConfig = LevelRegistry.getInstance().getLevel(levelId);
            if (nextConfig == null) return;

            WorldPosition spawn = nextConfig.spawnPoint();
            Transform spawnTransform = new Transform(
                new Vector3d(spawn.x(), spawn.y(), spawn.z()),
                new Vector3f(0f, CameraPacketBuilder.ISO_CAMERA_YAW_RAD, 0f)
            );

            Nexus.getInstance().getLevelWorldService().activateLevel(nextWorld, levelId);

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
