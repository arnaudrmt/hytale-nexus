package fr.arnaud.nexus.level;

import com.hypixel.hytale.builtin.instances.InstancesPlugin;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.player.ChunkTracker;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import fr.arnaud.nexus.camera.CameraPacketBuilder;
import fr.arnaud.nexus.component.RunSessionComponent;
import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.event.RunCompletedEvent;
import fr.arnaud.nexus.spawner.SpawnerTagComponent;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
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

        cmd.run(s -> {
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

    private void loadNextLevel(Ref<EntityStore> playerRef,
                               CommandBuffer<EntityStore> cmd,
                               World world,
                               String nextLevelId) {
        boolean loaded = levelManager.loadLevel(nextLevelId);
        if (!loaded) return;

        LevelConfig nextConfig = levelManager.getCurrentConfig();
        LevelConfig.Position spawn = nextConfig.getSpawnPoint();
        Vector3d spawnPos = new Vector3d(spawn.getX(), spawn.getY(), spawn.getZ());

        updateCheckpoint(playerRef, cmd, spawn);

        cmd.run(s -> {
            PlayerRef playerRefComponent = s.getComponent(playerRef, PlayerRef.getComponentType());
            ChunkTracker chunkTracker = s.getComponent(playerRef, ChunkTracker.getComponentType());
            if (playerRefComponent != null && chunkTracker != null) {
                chunkTracker.unloadAll(playerRefComponent);
            }
        });

        Teleport teleport = Teleport.createForPlayer(spawnPos,
            new Vector3f(0f, CameraPacketBuilder.ISO_YAW_RAD, 0f));
        cmd.run(s -> s.addComponent(playerRef, Teleport.getComponentType(), teleport));

        world.execute(() -> {
            Store<EntityStore> store = world.getEntityStore().getStore();
            store.forEachChunk(
                SpawnerTagComponent.getComponentType(),
                (chunk, buf) -> {
                    for (int i = 0; i < chunk.size(); i++) {
                        Ref<EntityStore> ref = chunk.getReferenceTo(i);
                        if (ref.isValid()) buf.removeEntity(ref, RemoveReason.REMOVE);
                    }
                }
            );
        });

        world.execute(() -> {
            Nexus.get().getMobSpawnerManager().onLevelLoaded(world, nextConfig);
            showLevelTitle(nextConfig, world);
        });
    }

    private void updateCheckpoint(Ref<EntityStore> playerRef,
                                  CommandBuffer<EntityStore> cmd,
                                  LevelConfig.Position spawn) {
        LevelProgressComponent progress = cmd.getComponent(playerRef, LevelProgressComponent.getComponentType());
        if (progress == null) return;
        progress.checkpointX = (float) spawn.getX();
        progress.checkpointY = (float) spawn.getY();
        progress.checkpointZ = (float) spawn.getZ();
        cmd.run(s -> s.putComponent(playerRef, LevelProgressComponent.getComponentType(), progress));
    }

    private CompletableFuture<Void> preloadLevelChunks(LevelConfig config, World world) {
        LevelConfig.Position spawn = config.getSpawnPoint();
        List<CompletableFuture<?>> futures = new ArrayList<>();

        futures.add(loadChunkAt(spawn.getX(), spawn.getZ(), world));

        for (LevelConfig.SpawnerConfig spawner : config.getSpawners()) {
            LevelConfig.Position pos = spawner.getPosition();
            futures.add(loadChunkAt(pos.getX(), pos.getZ(), world));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    private CompletableFuture<?> loadChunkAt(double x, double z, World world) {
        long chunkIndex = ChunkUtil.indexChunkFromBlock((int) x, (int) z);
        return world.getChunkStore().getChunkReferenceAsync(chunkIndex, 4);
    }

    private void showLevelTitle(@NonNullDecl LevelConfig config, @NonNullDecl World world) {
        world.execute(() -> {
            var store = world.getEntityStore().getStore();
            Message primary = Message.translation("nexus.level.title").param("name", config.getName());
            Message secondary = Message.translation("nexus.level.subtitle").param("difficulty", config.getDifficulty());
            EventTitleUtil.showEventTitleToWorld(primary, secondary, true, null, 4.0f, 1.5f, 1.5f, store);
        });
    }
}
