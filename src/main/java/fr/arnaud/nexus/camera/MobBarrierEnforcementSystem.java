package fr.arnaud.nexus.camera;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.spawner.SpawnerTagComponent;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

/**
 * Re-materializes barrier blocks from the occlusion hole when a mob enters
 * their radius, preventing mobs from escaping the arena.
 *
 * <p>Runs every {@value #TICK_INTERVAL} ticks (~333ms at 30 TPS).
 * A barrier is only removed again once every mob within radius has left.
 * The source of truth for which positions are valid barrier slots is
 * {@link PlayerOcclusionComponent#barrierColumnPositions}, so this system
 * never touches non-barrier blocks.
 */
public final class MobBarrierEnforcementSystem extends EntityTickingSystem<EntityStore> {

    private static final int TICK_INTERVAL = 10;
    private static final double ENFORCE_RADIUS_SQ = 2.0 * 2.0;

    /**
     * packed position → active mob ref-count this pass. Cleared after each flush.
     */
    private final Long2IntOpenHashMap barrierRefCounts = new Long2IntOpenHashMap();
    private PlayerOcclusionComponent cachedOcclusion = null;

    /**
     * Barrier positions currently force-spawned by this system.
     */
    private final LongOpenHashSet enforcedPositions = new LongOpenHashSet();

    private int mobsProcessedThisInterval = 0;
    private int tickCounter = 0;
    private int barrierBlockId = Integer.MIN_VALUE;

    @NonNullDecl
    @Override
    public Query<EntityStore> getQuery() {
        return SpawnerTagComponent.getComponentType();
    }

    @Override
    public void tick(float deltaSeconds, int index,
                     @NonNullDecl ArchetypeChunk<EntityStore> chunk,
                     @NonNullDecl Store<EntityStore> store,
                     @NonNullDecl CommandBuffer<EntityStore> cmd) {

        if (index == 0) tickCounter++;
        if (tickCounter % TICK_INTERVAL != 0) return;

        ensureBarrierIdCached();

        World world = Nexus.get().getNexusWorldLoadSystem().getNexusWorld();
        if (world == null) return;

        // Resolve occlusion once per interval on first mob processed (index 0, first chunk)
        if (index == 0 && mobsProcessedThisInterval == 0) {
            cachedOcclusion = resolveOcclusionComponent(store);
        }
        if (cachedOcclusion == null) return;

        TransformComponent transform = store.getComponent(
            chunk.getReferenceTo(index), EntityModule.get().getTransformComponentType());
        if (transform != null) {
            accumulateNearbyBarriers(transform.getPosition(), cachedOcclusion);
        }

        mobsProcessedThisInterval++;

        int totalMobs = store.getEntityCountFor(SpawnerTagComponent.getComponentType());
        if (mobsProcessedThisInterval >= totalMobs) {
            flushBarrierState(world, cachedOcclusion);
            mobsProcessedThisInterval = 0;
            cachedOcclusion = null;
        }
    }

    // ── Core logic ──────────────────────────────────────────────────────────────

    private void accumulateNearbyBarriers(Vector3d feet, PlayerOcclusionComponent occlusion) {
        LongOpenHashSet barrierSlots = occlusion.getBarrierColumnPositions();
        if (barrierSlots.isEmpty()) return;

        double fx = feet.getX(), fy = feet.getY(), fz = feet.getZ();

        for (long packed : barrierSlots) {
            int[] c = PlayerOcclusionComponent.unpack(packed);
            double dx = (c[0] + 0.5) - fx;
            double dz = (c[2] + 0.5) - fz;
            double dy = (c[1] + 0.5) - fy;

            if (dx * dx + dz * dz > ENFORCE_RADIUS_SQ) continue;
            if (dy < 0 || dy > 2.0) continue;

            barrierRefCounts.merge(packed, 1, Integer::sum);
        }
    }

    private void flushBarrierState(World world, PlayerOcclusionComponent occlusion) {
        // Spawn barriers where mobs are present but block is currently Air
        for (long packed : barrierRefCounts.keySet()) {
            if (!enforcedPositions.contains(packed)) {
                int[] c = PlayerOcclusionComponent.unpack(packed);
                if (sampleBlock(world, c[0], c[1], c[2]) == BlockType.EMPTY_ID) {
                    spawnBarrier(world, c[0], c[1], c[2]);
                    enforcedPositions.add(packed);
                }
            }
        }

        // Restore Air where all mobs have left
        LongOpenHashSet toRestore = new LongOpenHashSet();
        for (long packed : enforcedPositions) {
            if (barrierRefCounts.getOrDefault(packed, 0) == 0) {
                toRestore.add(packed);
            }
        }
        for (long packed : toRestore) {
            int[] c = PlayerOcclusionComponent.unpack(packed);
            replaceWithAir(world, c[0], c[1], c[2]);
            enforcedPositions.remove(packed);
        }

        barrierRefCounts.clear();
    }

    // ── Block I/O ────────────────────────────────────────────────────────────────

    private void spawnBarrier(World world, int x, int y, int z) {
        BlockType bt = BlockType.getAssetMap().getAsset(barrierBlockId);
        if (bt == null) return;
        setBlock(world, x, y, z, barrierBlockId, bt);
    }

    private static void replaceWithAir(World world, int x, int y, int z) {
        setBlock(world, x, y, z, BlockType.EMPTY_ID, BlockType.EMPTY);
    }

    private static void setBlock(World world, int x, int y, int z, int blockId, BlockType blockType) {
        WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(x, z));
        if (chunk == null) return;
        chunk.setBlock(x & 31, y, z & 31, blockId, blockType, 0, 0, 4 | 2 | 8 | 16 | 512);
    }

    private static int sampleBlock(World world, int x, int y, int z) {
        if (y < 0 || y >= 320) return BlockType.EMPTY_ID;
        WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(x, z));
        if (chunk == null) return BlockType.EMPTY_ID;
        return chunk.getBlock(x & 31, y, z & 31);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    /**
     * Walks the store to find the single player's OcclusionComponent.
     */
    private static PlayerOcclusionComponent resolveOcclusionComponent(Store<EntityStore> store) {
        // Single-player: walk the player archetype chunk directly via forEachChunk
        PlayerOcclusionComponent[] result = {null};
        store.forEachChunk(
            Query.and(PlayerRef.getComponentType(), PlayerOcclusionComponent.getComponentType()),
            (archetypeChunk, cmd) -> {
                if (archetypeChunk.size() > 0) {
                    result[0] = archetypeChunk.getComponent(0, PlayerOcclusionComponent.getComponentType());
                }
            }
        );
        return result[0];
    }

    private void ensureBarrierIdCached() {
        if (barrierBlockId == Integer.MIN_VALUE) {
            barrierBlockId = BlockType.getAssetMap().getIndex("Barrier");
        }
    }
}
