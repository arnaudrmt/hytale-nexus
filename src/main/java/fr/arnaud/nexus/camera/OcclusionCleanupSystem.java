package fr.arnaud.nexus.camera;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.core.Nexus;

public final class OcclusionCleanupSystem extends RefSystem<EntityStore> {

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(PlayerRef.getComponentType(), PlayerOcclusionComponent.getComponentType());
    }

    @Override
    public void onEntityAdded(Ref<EntityStore> ref, AddReason reason,
                              Store<EntityStore> store, CommandBuffer<EntityStore> cmd) {
    }

    @Override
    public void onEntityRemove(Ref<EntityStore> ref, RemoveReason reason,
                               Store<EntityStore> store, CommandBuffer<EntityStore> cmd) {
        PlayerOcclusionComponent occlusion = store.getComponent(ref, PlayerOcclusionComponent.getComponentType());
        if (occlusion == null) return;

        World world = Nexus.get().getNexusWorldLoadSystem().getNexusWorld();
        if (world == null) return;

        occlusion.markDestroyed();

        for (long packed : occlusion.getReplacedPositions()) {
            int[] c = PlayerOcclusionComponent.unpack(packed);
            WorldChunk chunk = world.getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(c[0], c[2]));
            restoreBlock(world, c[0], c[1], c[2],
                occlusion.getOriginalBlockId(c[0], c[1], c[2]),
                occlusion.getOriginalRotation(c[0], c[1], c[2]),
                occlusion.getOriginalFiller(c[0], c[1], c[2]));
        }
    }

    private static void restoreBlock(World world, int x, int y, int z, int blockId, int rotation, int filler) {
        BlockType bt = BlockType.getAssetMap().getAsset(blockId);
        if (bt == null) return;
        WorldChunk chunk = world.getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(x, z));
        if (chunk == null) return;
        chunk.setBlock(x & 31, y, z & 31, blockId, bt, rotation, filler, 4 | 2 | 8 | 16 | 512);
    }
}
