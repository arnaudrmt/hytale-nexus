package fr.arnaud.nexus.spawner.portal;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.level.LevelConfig;
import fr.arnaud.nexus.level.LevelTransitionService;
import fr.arnaud.nexus.math.WorldPosition;

import java.util.function.Supplier;

public final class FinishPortalController {

    //FIXME: Fix portal particle not spawning.
    private static final float PORTAL_TRIGGER_RADIUS = 2.0f;
    private static final String PORTAL_PARTICLE = "MagicPortal_Default";

    private final LevelTransitionService levelTransitionService;
    private final Supplier<World> activeWorldSupplier;

    private WorldPosition finishPoint;
    private boolean portalSpawned = false;
    private boolean levelTransitionTriggered = false;

    public FinishPortalController(LevelTransitionService levelTransitionService,
                                  Supplier<World> activeWorldSupplier) {
        this.levelTransitionService = levelTransitionService;
        this.activeWorldSupplier = activeWorldSupplier;
    }

    public void onLevelLoaded(LevelConfig config) {
        this.finishPoint = config.finishPoint();
        this.portalSpawned = false;
        this.levelTransitionTriggered = false;
    }

    public boolean isPortalSpawned() {
        return portalSpawned;
    }

    public void spawnPortal() {
        portalSpawned = true;

        World world = activeWorldSupplier.get();
        if (world == null) return;

        Vector3d portalPos = new Vector3d(
            finishPoint.x() + 0.5,
            finishPoint.y() + 1.5,
            finishPoint.z() + 0.5
        );

        world.execute(() -> world.getPlayerRefs().forEach(_ ->
            ParticleUtil.spawnParticleEffect(PORTAL_PARTICLE, portalPos,
                world.getEntityStore().getStore())
        ));
    }

    public void checkProximityTrigger(Vector3d playerPosition, Ref<EntityStore> playerRef,
                                      CommandBuffer<EntityStore> commandBuffer) {
        if (levelTransitionTriggered) return;

        double dx = playerPosition.getX() - finishPoint.x();
        double dy = playerPosition.getY() - finishPoint.y();
        double dz = playerPosition.getZ() - finishPoint.z();

        if (dx * dx + dy * dy + dz * dz > PORTAL_TRIGGER_RADIUS * PORTAL_TRIGGER_RADIUS) return;

        levelTransitionTriggered = true;

        World world = activeWorldSupplier.get();
        if (world == null) return;

        levelTransitionService.handlePortalEntered(playerRef, commandBuffer, world);
    }
}
