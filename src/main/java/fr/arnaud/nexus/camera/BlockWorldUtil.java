package fr.arnaud.nexus.camera;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;

public final class BlockWorldUtil {

    static final int BLOCK_SET_SILENT_FLAGS = 4 | 2 | 8 | 16 | 512;

    private BlockWorldUtil() {}

    public static void restoreBlock(World world, int x, int y, int z, int blockId, int rotation, int filler) {
        BlockType bt = BlockType.getAssetMap().getAsset(blockId);
        if (bt == null) return;
        WorldChunk chunk = world.getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(x, z));
        if (chunk == null) return;
        chunk.setBlock(x & 31, y, z & 31, blockId, bt, rotation, filler, BLOCK_SET_SILENT_FLAGS);
    }
}
