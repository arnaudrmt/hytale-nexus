package fr.arnaud.nexus.level;

import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import fr.arnaud.nexus.Nexus;

import javax.annotation.Nonnull;
import java.util.logging.Level;

/**
 * Handles transitions between Nexus level instances.
 * <p>
 * Call {@link #sendPlayerToLevel} to move a player to a specific level. The
 * method resolves the target {@link World} from {@link Universe}: it returns
 * the already-running world if present, or loads it from disk if it exists
 * but is not yet running. If the world is neither running nor on disk, a
 * warning is logged and the transfer is skipped.
 * <p>
 * POI schematic pasting is handled independently by {@link LevelWorldLoadSystem}
 * via {@code StartWorldEvent} — this class has no paste responsibility.
 */
public final class LevelManager {

    public void sendPlayerToLevel(@Nonnull PlayerRef playerRef, @Nonnull LevelId levelId) {
        LevelDefinition definition = LevelRegistry.get(levelId);
        String instanceName = definition.instanceName();

        resolveWorld(instanceName).thenAccept(world -> {
            if (world == null) {
                Nexus.get().getLogger().at(Level.WARNING)
                     .log("Target world not found and could not be loaded for level: "
                         + levelId + " (instance: " + instanceName + ")");
                return;
            }

            Vector3d spawnPosition = new Vector3d(
                definition.playerSpawn().x,
                definition.playerSpawn().y,
                definition.playerSpawn().z
            );

            world.addPlayer(playerRef, new Transform(spawnPosition));
        });
    }

    private java.util.concurrent.CompletableFuture<World> resolveWorld(@Nonnull String instanceName) {
        Universe universe = Universe.get();

        World running = universe.getWorld(instanceName);
        if (running != null) {
            return java.util.concurrent.CompletableFuture.completedFuture(running);
        }

        if (universe.isWorldLoadable(instanceName)) {
            return universe.loadWorld(instanceName);
        }

        return java.util.concurrent.CompletableFuture.completedFuture(null);
    }
}
