package fr.arnaud.nexus.command;

import com.hypixel.hytale.builtin.crafting.component.BenchBlock;
import com.hypixel.hytale.builtin.crafting.window.SimpleCraftingWindow;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

/**
 * Temporary dev command that opens the weapon management UI directly.
 */
public final class OpenWeaponStationCommand extends AbstractPlayerCommand {

    private static final int STATION_X = 0;
    private static final int STATION_Y = 0;
    private static final int STATION_Z = 0;
    private static final String STATION_BLOCK_ID = "Nexus_Weapon_Station";

    public OpenWeaponStationCommand() {
        super("openweapon", "Opens the Nexus weapon management UI");
    }

    @Override
    protected void execute(@NonNullDecl CommandContext context,
                           @NonNullDecl Store<EntityStore> store,
                           @NonNullDecl Ref<EntityStore> ref,
                           @NonNullDecl PlayerRef playerRef,
                           @NonNullDecl World world) {

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;

        BlockType blockType = BlockType.getAssetMap().getAsset(STATION_BLOCK_ID);
        if (blockType == null) return;

        BenchBlock benchBlock = resolveBenchBlock(world, STATION_X, STATION_Y, STATION_Z);
        if (benchBlock == null) return;

        SimpleCraftingWindow window = new SimpleCraftingWindow(STATION_X, STATION_Y, STATION_Z, 0, blockType, benchBlock);
        player.getPageManager().setPageWithWindows(ref, store, Page.Bench, true, window);
    }

    private BenchBlock resolveBenchBlock(World world, int x, int y, int z) {
        long chunkIndex = ChunkUtil.indexChunk(ChunkUtil.chunkCoordinate(x), ChunkUtil.chunkCoordinate(z));
        Ref<ChunkStore> chunkRef = world.getChunkStore().getChunkReference(chunkIndex);
        if (chunkRef == null || !chunkRef.isValid()) return null;

        Store<ChunkStore> chunkStore = chunkRef.getStore();
        WorldChunk worldChunk = chunkStore.getComponent(chunkRef, WorldChunk.getComponentType());
        if (worldChunk == null) return null;

        Ref<ChunkStore> blockEntityRef = worldChunk.getBlockComponentEntity(x, y, z);
        if (blockEntityRef == null || !blockEntityRef.isValid()) return null;

        return chunkStore.getComponent(blockEntityRef, BenchBlock.getComponentType());
    }
}
