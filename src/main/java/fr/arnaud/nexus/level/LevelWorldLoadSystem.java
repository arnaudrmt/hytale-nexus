package fr.arnaud.nexus.level;

import com.hypixel.hytale.builtin.instances.InstancesPlugin;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.events.StartWorldEvent;
import fr.arnaud.nexus.Nexus;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public final class LevelWorldLoadSystem {

    private static final String NEXUS_INSTANCE_TEMPLATE = "Nexus";
    private static final String DEFAULT_WORLD_NAME = "default";
    private static final String STARTING_LEVEL_ID = "level_1";

    private volatile World nexusWorld;
    private CompletableFuture<World> pendingNexusWorld;

    public void onWorldStart(StartWorldEvent event) {
        World world = event.getWorld();

        if (world.getName().contains("Nexus")) {
            if (pendingNexusWorld == null) {
                nexusWorld = world;
                pendingNexusWorld = CompletableFuture.completedFuture(world);
                onNexusWorldReady(world);
            }
            return;
        }

        if (!DEFAULT_WORLD_NAME.equals(world.getName())) {
            return;
        }

        if (pendingNexusWorld != null) {
            return;
        }
        
        Transform returnPoint = new Transform(new Vector3d(0.5, 80.0, 0.5), Vector3f.FORWARD);

        pendingNexusWorld = InstancesPlugin.get().spawnInstance(
            NEXUS_INSTANCE_TEMPLATE,
            world,
            returnPoint
        );

        pendingNexusWorld.thenAccept(w -> {
            if (nexusWorld == null) {
                nexusWorld = w;
                onNexusWorldReady(w);
            }
        });
    }

    private void onNexusWorldReady(World world) {
        LevelManager levelManager = Nexus.get().getLevelManager();

        boolean loaded = levelManager.loadLevel(STARTING_LEVEL_ID);
        if (!loaded) {
            Nexus.get().getLogger().at(Level.SEVERE)
                 .log("Failed to load starting level config: " + STARTING_LEVEL_ID);
            return;
        }

        Nexus.get().getMobSpawnManager().onLevelLoaded(world, levelManager.getCurrentConfig());

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
