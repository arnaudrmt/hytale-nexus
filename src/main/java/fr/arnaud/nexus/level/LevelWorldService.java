package fr.arnaud.nexus.level;

import com.hypixel.hytale.builtin.instances.InstancesPlugin;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.events.StartWorldEvent;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import fr.arnaud.nexus.camera.CameraPacketBuilder;
import fr.arnaud.nexus.core.Nexus;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class LevelWorldService {

    public static final String LEVEL_WORLD_KEY_PREFIX = "nexus-level-";

    private volatile World currentLevelWorld;
    private volatile LevelConfig activeLevelConfig;
    private CompletableFuture<World> pendingLevelWorldFuture;
    private final ConcurrentHashMap<String, World> preloadedLevelWorlds = new ConcurrentHashMap<>();

    public void handleWorldStart(StartWorldEvent event) {
        World world = event.getWorld();

        if (world.getName().startsWith(LEVEL_WORLD_KEY_PREFIX)) {
            String levelId = world.getName().substring(LEVEL_WORLD_KEY_PREFIX.length());
            String firstLevelId = LevelRegistry.getInstance().getFirstLevelId();

            if (levelId.equals(firstLevelId)) {
                activateLevel(world, levelId);
            } else {
                preloadedLevelWorlds.put(levelId, world);
                Nexus.getInstance().getLogger().at(Level.INFO).log("Preloaded level world ready: " + levelId);
            }
            return;
        }

        // Any non-level world (lobby, default) triggers the initial level world creation.
        if (pendingLevelWorldFuture != null) return;

        String firstLevelId = LevelRegistry.getInstance().getFirstLevelId();
        pendingLevelWorldFuture = getOrCreateLevelWorld(firstLevelId, world)
            .thenApply(w -> {
                currentLevelWorld = w;
                return w;
            })
            .exceptionally(ex -> {
                Nexus.getInstance().getLogger().at(Level.SEVERE).log("Failed to spawn starting level world: " + ex.getMessage());
                return null;
            });
    }

    public void activateLevel(World world, String levelId) {
        LevelConfig config = LevelRegistry.getInstance().getLevel(levelId);

        if (config == null) {
            Nexus.getInstance().getLogger().at(Level.SEVERE).log("activateLevel: config not found for: " + levelId);
            return;
        }

        world.getWorldConfig().setGameTime(Instant.ofEpochSecond(25200L));
        currentLevelWorld = world;
        activeLevelConfig = config;

        Nexus.getInstance().getSpawnerManager().onLevelLoaded(world, config);

        world.execute(() -> {
            Message primary = Message.translation(config.key_name());
            Message secondary = Message.translation("nexus.level.subtitle");
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor()
                                          .schedule(() -> world.execute(() ->
                                              EventTitleUtil.showEventTitleToWorld(
                                                  primary, secondary, true, null,
                                                  4.0f, 1.5f, 1.5f,
                                                  world.getEntityStore().getStore())
                                          ), 10, java.util.concurrent.TimeUnit.SECONDS);
        });

        Nexus.getInstance().getLogger().at(Level.INFO)
             .log("Level activated: " + levelId + " | Spawners: " + config.spawners().size());

        String nextLevelId = LevelRegistry.getInstance().getNextLevelId(levelId);
        if (nextLevelId != null && !nextLevelId.isEmpty()) {
            schedulePreload(nextLevelId, world);
        }
    }

    public void schedulePreload(String levelId, World returnWorld) {
        if (preloadedLevelWorlds.containsKey(levelId)) return;
        getOrCreateLevelWorld(levelId, returnWorld)
            .thenAccept(world -> {
                if (world != null) preloadedLevelWorlds.put(levelId, world);
            });
    }

    public World consumePreloadedWorld(String levelId) {
        return preloadedLevelWorlds.remove(levelId);
    }

    public CompletableFuture<World> getOrCreateLevelWorld(String levelId, World returnWorld) {
        LevelConfig config = LevelRegistry.getInstance().getLevel(levelId);
        if (config == null) return CompletableFuture.failedFuture(
            new IllegalStateException("Level config not found: " + levelId));

        String worldKey = LEVEL_WORLD_KEY_PREFIX + levelId;

        World existing = Universe.get().getWorld(worldKey);
        if (existing != null) return CompletableFuture.completedFuture(existing);

        if (Universe.get().isWorldLoadable(worldKey)) {
            return Universe.get().loadWorld(worldKey)
                           .exceptionally(ex -> {
                               Nexus.getInstance().getLogger().at(Level.SEVERE)
                                    .log("Failed to load existing level world '" + levelId + "': " + ex.getMessage());
                               return null;
                           });
        }

        Transform spawnTransform = new Transform(
            new Vector3d(config.spawnPoint().x(), config.spawnPoint().y(), config.spawnPoint().z()),
            new Vector3f(0f, CameraPacketBuilder.ISO_CAMERA_YAW_RAD, 0f)
        );

        return InstancesPlugin.get()
                              .spawnInstance(config.instanceTemplate(), worldKey, returnWorld, spawnTransform)
                              .exceptionally(ex -> {
                                  Nexus.getInstance().getLogger().at(Level.SEVERE)
                                       .log("Failed to spawn level world '" + levelId + "': " + ex.getMessage());
                                  return null;
                              });
    }

    public World getCurrentLevelWorld() {
        return currentLevelWorld;
    }

    /**
     * Returns the config for the level that is currently active, or null if no level has been activated yet.
     */
    @Nullable
    public LevelConfig getActiveLevelConfig() {
        return activeLevelConfig;
    }

    public CompletableFuture<World> getPendingLevelWorldFuture() {
        return pendingLevelWorldFuture;
    }
}
