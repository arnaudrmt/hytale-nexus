package fr.arnaud.nexus.ui.inventory;

import com.hypixel.hytale.builtin.crafting.component.BenchBlock;
import com.hypixel.hytale.builtin.crafting.window.SimpleCraftingWindow;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.window.ClientOpenWindow;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class InventoryPacketInterceptor {

    private static final int STATION_X = 0;
    private static final int STATION_Y = 0;
    private static final int STATION_Z = 0;
    private static final String STATION_BLOCK_ID = "Nexus_Weapon_Station";

    private PacketFilter registeredFilter;

    public void register() {
        registeredFilter = PacketAdapters.registerInbound(
            (PlayerPacketFilter) this::interceptClientOpenWindow
        );
    }

    public void unregister() {
        if (registeredFilter != null) {
            PacketAdapters.deregisterInbound(registeredFilter);
            registeredFilter = null;
        }
    }

    private boolean interceptClientOpenWindow(PlayerRef playerRef, Packet packet) {
        if (!(packet instanceof ClientOpenWindow openWindow)) return false;

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) return false;

        World world = ref.getStore().getExternalData().getWorld();
        world.setBlock(STATION_X, STATION_Y, STATION_Z, STATION_BLOCK_ID);

        world.execute(() -> {

            Player player = ref.getStore().getComponent(ref, Player.getComponentType());
            BlockType blockType = BlockType.getAssetMap().getAsset(STATION_BLOCK_ID);
            BenchBlock benchBlock = resolveBenchBlock(world, STATION_X, STATION_Y, STATION_Z);

            SimpleCraftingWindow window = new SimpleCraftingWindow(STATION_X, STATION_Y, STATION_Z, 0, blockType, benchBlock);
            //player.getPageManager().setPageWithWindows(ref, ref.getStore(), Page.Bench, true, window);
        });

        return true;
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
