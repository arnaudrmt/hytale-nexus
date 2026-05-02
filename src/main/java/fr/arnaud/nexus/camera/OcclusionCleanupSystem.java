package fr.arnaud.nexus.camera;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.core.Nexus;
import org.jetbrains.annotations.NotNull;

public final class OcclusionCleanupSystem extends RefSystem<EntityStore> {

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(PlayerRef.getComponentType(), PlayerOcclusionComponent.getComponentType());
    }

    @Override
    public void onEntityAdded(@NotNull Ref<EntityStore> ref, @NotNull AddReason reason,
                              @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> cmd) {
    }

    @Override
    public void onEntityRemove(@NotNull Ref<EntityStore> ref, @NotNull RemoveReason reason,
                               Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> cmd) {
        PlayerOcclusionComponent occlusion = store.getComponent(ref, PlayerOcclusionComponent.getComponentType());
        if (occlusion == null) return;

        World world = Nexus.getInstance().getLevelWorldService().getCurrentLevelWorld();
        if (world == null) return;

        occlusion.markDestroyed();

        for (long packedBlockPos : occlusion.getReplacedPositions()) {
            int[] blockCoords = PlayerOcclusionComponent.unpack(packedBlockPos);
            BlockWorldUtil.restoreBlock(world, blockCoords[0], blockCoords[1], blockCoords[2],
                occlusion.getOriginalBlockId(blockCoords[0], blockCoords[1], blockCoords[2]),
                occlusion.getOriginalRotation(blockCoords[0], blockCoords[1], blockCoords[2]),
                occlusion.getOriginalFiller(blockCoords[0], blockCoords[1], blockCoords[2]));
        }
    }
}
