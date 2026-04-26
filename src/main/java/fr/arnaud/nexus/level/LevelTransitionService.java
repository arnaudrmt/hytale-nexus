package fr.arnaud.nexus.level;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import fr.arnaud.nexus.component.RunSessionComponent;
import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.event.RunCompletedEvent;
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

    /**
     * Called when the player enters the finish portal.
     * Records the level split, then either loads the next level or fires {@link RunCompletedEvent}.
     */
    public void onPortalEntered(@NonNullDecl Ref<EntityStore> playerRef,
                                @NonNullDecl CommandBuffer<EntityStore> cmd,
                                @NonNullDecl World world) {
        RunSessionComponent session = cmd.getComponent(playerRef, RunSessionComponent.getComponentType());
        if (session != null) {
            session.pauseSession();
            session.recordLevelSplit();
            cmd.run(s -> s.putComponent(playerRef, RunSessionComponent.getComponentType(), session));
        }

        @Nullable String nextLevelId = levelManager.getCurrentConfig().getNextLevelId();

        if (nextLevelId == null) {
            completeRun(playerRef, session);
            return;
        }

        loadNextLevel(playerRef, cmd, world, nextLevelId);
    }

    private void completeRun(@NonNullDecl Ref<EntityStore> playerRef,
                             @Nullable RunSessionComponent session) {
        if (session == null) return;
        // Snapshot before dispatch so the handler receives a stable, immutable copy
        RunCompletedEvent.dispatch(playerRef, session.clone());
    }

    private void loadNextLevel(@NonNullDecl Ref<EntityStore> playerRef,
                               @NonNullDecl CommandBuffer<EntityStore> cmd,
                               @NonNullDecl World world,
                               @NonNullDecl String nextLevelId) {
        boolean loaded = levelManager.loadLevel(nextLevelId);
        if (!loaded) return;

        LevelConfig nextConfig = levelManager.getCurrentConfig();
        LevelConfig.Position spawn = nextConfig.getSpawnPoint();
        Vector3d spawnPos = new Vector3d(spawn.getX(), spawn.getY(), spawn.getZ());

        Nexus.get().getMobSpawnerManager().onLevelLoaded(world, nextConfig);

        LevelProgressComponent progress = cmd.getComponent(playerRef, LevelProgressComponent.getComponentType());
        if (progress != null) {
            progress.checkpointX = (float) spawn.getX();
            progress.checkpointY = (float) spawn.getY();
            progress.checkpointZ = (float) spawn.getZ();
            cmd.run(s -> s.putComponent(playerRef, LevelProgressComponent.getComponentType(), progress));
        }

        showLoadingTitle(world);
        preloadLevelChunks(nextConfig, world)
            .thenRun(() -> world.execute(() -> {
                cmd.run(s -> s.addComponent(playerRef, Teleport.getComponentType(),
                    Teleport.createForPlayer(spawnPos, Vector3f.FORWARD)));
                showLevelTitle(nextConfig, world);
            }));
    }

    private CompletableFuture<Void> preloadLevelChunks(LevelConfig config, World world) {
        LevelConfig.Position spawn = config.getSpawnPoint();
        List<CompletableFuture<?>> futures = new ArrayList<>();

        // Always pre-load spawn chunk
        futures.add(loadChunkAt(spawn.getX(), spawn.getZ(), world));

        // Pre-load every spawner chunk — NPCs cannot spawn in unloaded chunks
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

    private void showLoadingTitle(@NonNullDecl World world) {
        world.execute(() -> {
            var store = world.getEntityStore().getStore();
            Message msg = Message.translation("nexus.level.loading");
            EventTitleUtil.showEventTitleToWorld(msg, Message.translation("nexus.level.loading.subtitle"), true, null, 0f, 99f, 0f, store);
        });
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
