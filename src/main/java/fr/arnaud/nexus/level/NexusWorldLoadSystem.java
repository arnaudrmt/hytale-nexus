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

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public final class NexusWorldLoadSystem {

    private static final String DEFAULT_WORLD_NAME = "default";
    private static final String STARTING_LEVEL_ID = "level_1";
    public static final String LEVEL_WORLD_KEY_PREFIX = "nexus-level-";

    private volatile World activeWorld;
    private CompletableFuture<World> pendingActiveWorld;
    private final java.util.concurrent.ConcurrentHashMap<String, World> preloadedWorlds = new java.util.concurrent.ConcurrentHashMap<>();

    public void onWorldStart(StartWorldEvent event) {
        World world = event.getWorld();

        if (world.getName().startsWith(LEVEL_WORLD_KEY_PREFIX)) {
            String levelId = world.getName().substring(LEVEL_WORLD_KEY_PREFIX.length());
            if (levelId.equals(STARTING_LEVEL_ID)) {
                onPlayerJoinWorld(world, levelId);
            } else {
                preloadedWorlds.put(levelId, world);
                Nexus.getInstance().getLogger().at(Level.INFO)
                     .log("Preloaded level world ready: " + levelId);
            }
            return;
        }

        if (!DEFAULT_WORLD_NAME.equals(world.getName())) return;
        if (pendingActiveWorld != null) return;

        pendingActiveWorld = spawnLevelWorld(STARTING_LEVEL_ID, world)
            .thenApply(w -> {
                activeWorld = w;
                return w;
            })
            .exceptionally(ex -> {
                Nexus.getInstance().getLogger().at(Level.SEVERE)
                     .log("Failed to spawn starting level world: " + ex.getMessage());
                return null;
            });
    }

    public void onPlayerJoinWorld(World world, String levelId) {
        LevelManager levelManager = Nexus.getInstance().getLevelManager();
        if (!levelManager.isLevelLoaded() || !levelManager.getLevelId().equals(levelId)) {
            boolean loaded = levelManager.loadLevel(levelId);
            if (!loaded) {
                Nexus.getInstance().getLogger().at(Level.SEVERE)
                     .log("activateLevelWorld: failed to load config for: " + levelId);
                return;
            }
        }

        world.getWorldConfig().setGameTime(Instant.MAX);
        activeWorld = world;

        LevelConfig config = levelManager.getCurrentConfig();
        Nexus.getInstance().getMobSpawnerManager().onLevelLoaded(world, config);

        world.execute(() -> {
            Message primary = Message.translation("nexus.level.title").param("name", config.name());
            Message secondary = Message.translation("nexus.level.subtitle").param("difficulty", config.difficulty());
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor()
                                          .schedule(() -> world.execute(() ->
                                              EventTitleUtil.showEventTitleToWorld(primary, secondary, true, null, 4.0f, 1.5f, 1.5f,
                                                  world.getEntityStore().getStore())
                                          ), 10, java.util.concurrent.TimeUnit.SECONDS);
        });

        Nexus.getInstance().getLogger().at(Level.INFO)
             .log("Level activated: " + levelId
                 + " | Difficulty: " + config.difficulty()
                 + " | Spawners: " + config.spawners().size());

        if (config.nextLevelId() != null) {
            preloadNextLevel(config.nextLevelId(), world);
        }
    }

    public void preloadNextLevel(String levelId, World returnWorld) {
        if (preloadedWorlds.containsKey(levelId)) return;

        spawnLevelWorld(levelId, returnWorld)
            .thenAccept(world -> {
                if (world != null) preloadedWorlds.put(levelId, world);
            });
    }

    public World takePreloadedWorld(String levelId) {
        return preloadedWorlds.remove(levelId);
    }

    // Spawns a fresh instance world for the given level config.
    public CompletableFuture<World> spawnLevelWorld(String levelId, World returnWorld) {
        LevelConfig config = LevelConfigLoader.loadAndParseLevelConfig(levelId);
        if (config == null) return CompletableFuture.failedFuture(
            new IllegalStateException("Level config not found: " + levelId));

        String worldKey = LEVEL_WORLD_KEY_PREFIX + levelId;

        World existing = Universe.get().getWorld(worldKey);
        if (existing != null) {
            return CompletableFuture.completedFuture(existing);
        }

        if (Universe.get().isWorldLoadable(worldKey)) {
            return Universe.get().loadWorld(worldKey)
                           .exceptionally(ex -> {
                               Nexus.getInstance().getLogger().at(Level.SEVERE)
                                    .log("Failed to load existing level world '" + levelId + "': " + ex.getMessage());
                               return null;
                           });
        }

        Transform spawnTransform = new Transform(
            new Vector3d(config.spawnPoint().x(),
                config.spawnPoint().y(),
                config.spawnPoint().z()),
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

    public World getActiveWorld() {
        return activeWorld;
    }

    public CompletableFuture<World> getPendingActiveWorld() {
        return pendingActiveWorld;
    }
}
