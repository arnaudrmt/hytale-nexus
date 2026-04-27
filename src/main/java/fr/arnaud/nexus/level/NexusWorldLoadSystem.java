package fr.arnaud.nexus.level;

import com.hypixel.hytale.builtin.instances.InstancesPlugin;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.events.StartWorldEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import fr.arnaud.nexus.camera.CameraPacketBuilder;
import fr.arnaud.nexus.core.Nexus;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public final class NexusWorldLoadSystem {

    private static final String NEXUS_INSTANCE_TEMPLATE = "Nexus";
    private static final String NEXUS_WORLD_KEY = "nexus-persistent";
    private static final String DEFAULT_WORLD_NAME = "default";
    private static final String STARTING_LEVEL_ID = "level_1";

    private volatile World nexusWorld;
    private CompletableFuture<World> pendingNexusWorld;

    public void onWorldStart(StartWorldEvent event) {
        World world = event.getWorld();

        if (world.getName().toLowerCase().contains("nexus")) {
            if (pendingNexusWorld == null) {
                nexusWorld = world;
                pendingNexusWorld = CompletableFuture.completedFuture(world);
                onNexusWorldReady(world);
            }
            return;
        }

        if (!DEFAULT_WORLD_NAME.equals(world.getName())) return;
        if (pendingNexusWorld != null) return;

        Universe universe = Universe.get();

        if (universe.isWorldLoadable(NEXUS_WORLD_KEY)) {
            pendingNexusWorld = universe.loadWorld(NEXUS_WORLD_KEY)
                                        .thenApply(w -> {
                                            nexusWorld = w;
                                            onNexusWorldReady(w);
                                            return w;
                                        })
                                        .exceptionally(ex -> {
                                            Nexus.get().getLogger().at(Level.SEVERE)
                                                 .log("Failed to load Nexus world: " + ex.getMessage());
                                            return null;
                                        });
        } else {
            Transform returnPoint = new Transform(new Vector3d(0.5, 80.0, 0.5), new Vector3f(0f, CameraPacketBuilder.ISO_YAW_RAD, 0f));
            pendingNexusWorld = InstancesPlugin.get()
                                               .spawnInstance(NEXUS_INSTANCE_TEMPLATE, NEXUS_WORLD_KEY, world, returnPoint)
                                               .thenApply(w -> {
                                                   nexusWorld = w;
                                                   onNexusWorldReady(w);
                                                   return w;
                                               })
                                               .exceptionally(ex -> {
                                                   Nexus.get().getLogger().at(Level.SEVERE)
                                                        .log("Failed to spawn Nexus world: " + ex.getMessage());
                                                   return null;
                                               });
        }
    }

    private void onNexusWorldReady(World world) {
        LevelManager levelManager = Nexus.get().getLevelManager();

        world.getWorldConfig().setGameTime(Instant.MAX);

        boolean loaded = levelManager.loadLevel(STARTING_LEVEL_ID);
        if (!loaded) {
            Nexus.get().getLogger().at(Level.SEVERE)
                 .log("Failed to load starting level config: " + STARTING_LEVEL_ID);
            return;
        }

        Nexus.get().getMobSpawnerManager().onLevelLoaded(world, levelManager.getCurrentConfig());

        LevelConfig.Position spawn = levelManager.getCurrentConfig().getSpawnPoint();
        long spawnChunkIndex = ChunkUtil.indexChunkFromBlock((int) spawn.getX(), (int) spawn.getZ());
        world.getChunkStore().getChunkReferenceAsync(spawnChunkIndex, 4)
             .thenAccept(ref -> Nexus.get().getLogger()
                                     .at(Level.INFO).log("Nexus spawn chunk pre-loaded"));

        world.execute(() -> {
            Store<EntityStore> store = world.getEntityStore().getStore();
            LevelConfig config = levelManager.getCurrentConfig();
            Message primary = Message.translation("nexus.level.title").param("name", config.getName());
            Message secondary = Message.translation("nexus.level.subtitle").param("difficulty", config.getDifficulty());
            EventTitleUtil.showEventTitleToWorld(primary, secondary, true, null, 4.0f, 1.5f, 1.5f, store);
        });

        Nexus.get().getLogger().at(Level.INFO)
             .log("Level loaded: " + levelManager.getLevelName()
                 + " | Difficulty: " + levelManager.getDifficulty()
                 + " | Spawners: " + levelManager.getSpawners().size());
    }

    public World getNexusWorld() {
        return nexusWorld;
    }

    public CompletableFuture<World> getPendingNexusWorld() {
        return pendingNexusWorld;
    }
}
