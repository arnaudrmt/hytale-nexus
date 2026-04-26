package fr.arnaud.nexus.camera;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
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
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

/**
 * Manages camera occlusion by temporarily replacing blocks inside a cylinder
 * around the camera-to-player ray with Air (or Barrier) every tick.
 *
 * <p>The cylinder runs from the ISO camera position to the player's head only —
 * the AABB is computed over the full ray length for wall detection, but the
 * cylinder membership test clamps to {@code proj <= dist} so blocks behind the
 * player are never cleared.
 *
 * <p>Blocks within {@value #BARRIER_RADIUS} blocks of the player's feet and
 * below {@value #BARRIER_HEIGHT} blocks height are replaced with Barrier
 * rather than Air. Only the bottom two block-height rows of that column are
 * kept as solid Barrier; rows above are replaced with Air. This ensures mobs
 * cannot path through the occlusion hole while the wall above still vanishes.
 *
 * <p>Already-replaced positions are carried forward without re-sampling to
 * prevent the Air-read oscillation that would cause flicker.
 *
 * <p>Block rotation index and filler are captured before replacement and
 * restored verbatim, so oriented blocks (stairs, logs, slabs) return in the
 * correct state.
 */
public final class CameraOcclusionSystem extends EntityTickingSystem<EntityStore> {

    private static final double CYLINDER_RADIUS = 5.0;
    private static final double CYLINDER_RADIUS_SQ = CYLINDER_RADIUS * CYLINDER_RADIUS;
    private static final double EYE_HEIGHT = 1.62;
    private static final double ISO_PITCH_RAD = Math.toRadians(-45.0);
    private static final double FLOOR_Y_OFFSET = 0.0;

    /**
     * How many extra blocks past the player's head the AABB extends so that
     * walls at exactly head height are captured. The cylinder membership test
     * still cuts off at the player's head ({@code proj <= dist}).
     */
    private static final double RAY_EXTENSION = 3.0;

    /**
     * setBlock settings: skip particles (4), skip block entity (2),
     * skip filler set (8), skip filler remove (16), skip height update (512).
     */
    private static final int SILENT_SETTINGS = 4 | 2 | 8 | 16 | 512;

    private static final double BARRIER_RADIUS = 2.0;
    private static final double BARRIER_RADIUS_SQ = BARRIER_RADIUS * BARRIER_RADIUS;
    /**
     * Full barrier-zone height checked by {@link #isBarrierProtected}.
     * Only the lower {@value #BARRIER_SOLID_HEIGHT} block-rows within this
     * zone are kept as solid Barrier; the rest become Air.
     */
    private static final double BARRIER_HEIGHT = 2.0;
    private static final double BARRIER_SOLID_HEIGHT = 2.0;

    private int barrierBlockId = Integer.MIN_VALUE;
    private int epicChestBlockId = Integer.MIN_VALUE;

    @NonNullDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
            PlayerCameraComponent.getComponentType(),
            PlayerRef.getComponentType(),
            PlayerOcclusionComponent.getComponentType()
        );
    }

    @Override
    public void tick(float deltaSeconds, int index,
                     @NonNullDecl ArchetypeChunk<EntityStore> chunk,
                     @NonNullDecl Store<EntityStore> store,
                     @NonNullDecl CommandBuffer<EntityStore> cmd) {

        ensureBlockIdsCached();

        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        PlayerCameraComponent cam = chunk.getComponent(index, PlayerCameraComponent.getComponentType());
        PlayerOcclusionComponent occlusion = chunk.getComponent(index, PlayerOcclusionComponent.getComponentType());
        TransformComponent transform = store.getComponent(ref, EntityModule.get().getTransformComponentType());

        if (cam == null || occlusion == null || transform == null) return;

        World world = ((EntityStore) store.getExternalData()).getWorld();

        // ── Camera position ──────────────────────────────────────────────────
        Vector3d feet = transform.getPosition();
        double feetY = feet.getY();
        double headX = feet.getX();
        double headY = feetY + EYE_HEIGHT;
        double headZ = feet.getZ();
        float camDist = cam.getEffectiveIsoDistance();

        double yaw = CameraPacketBuilder.ISO_YAW_RAD;
        double cosPitch = Math.cos(ISO_PITCH_RAD);
        double sinPitch = Math.sin(ISO_PITCH_RAD);

        double camX = headX + Math.sin(yaw) * cosPitch * camDist;
        double camY = headY + (-sinPitch) * camDist;
        double camZ = headZ + Math.cos(yaw) * cosPitch * camDist;

        // ── Ray: camera → player head ─────────────────────────────────────────
        double rdx = headX - camX;
        double rdy = headY - camY;
        double rdz = headZ - camZ;
        double dist = Math.sqrt(rdx * rdx + rdy * rdy + rdz * rdz);
        if (dist < 1e-6) return;

        double ix = rdx / dist, iy = rdy / dist, iz = rdz / dist;

        // AABB extends RAY_EXTENSION past the head to catch walls at head height,
        // but the cylinder membership test below uses dist (not rayEnd) as the
        // far limit so blocks behind the player are never cleared.
        double rayEnd = dist + RAY_EXTENSION;
        double endX = camX + ix * rayEnd;
        double endY = camY + iy * rayEnd;
        double endZ = camZ + iz * rayEnd;
        double floorY = feetY + FLOOR_Y_OFFSET;

        // ── AABB over the full extended ray ───────────────────────────────────
        int bbR = (int) Math.ceil(CYLINDER_RADIUS);
        int minX = (int) Math.floor(Math.min(camX, endX)) - bbR;
        int maxX = (int) Math.ceil(Math.max(camX, endX)) + bbR;
        int minY = (int) Math.floor(Math.min(camY, endY)) - bbR;
        int maxY = (int) Math.ceil(Math.max(camY, endY)) + bbR;
        int minZ = (int) Math.floor(Math.min(camZ, endZ)) - bbR;
        int maxZ = (int) Math.ceil(Math.max(camZ, endZ)) + bbR;

        LongOpenHashSet currentPositions = new LongOpenHashSet();

        for (int bx = minX; bx <= maxX; bx++) {
            for (int by = minY; by <= maxY; by++) {
                if (by <= floorY || by < 0 || by >= 320) continue;
                for (int bz = minZ; bz <= maxZ; bz++) {
                    long packed = PlayerOcclusionComponent.pack(bx, by, bz);

                    // Carry already-replaced positions forward without re-sampling —
                    // reading Air from a replaced block would wrongly exclude it.
                    if (occlusion.getReplacedBlocks().containsKey(packed)) {
                        currentPositions.add(packed);
                        continue;
                    }

                    double cx = bx + 0.5, cy = by + 0.5, cz = bz + 0.5;
                    double ex = cx - camX, ey = cy - camY, ez = cz - camZ;
                    double proj = ex * ix + ey * iy + ez * iz;

                    // Cylinder runs camera → player head only; skip anything behind
                    // the camera or past the player's head.
                    if (proj < 0 || proj > dist) continue;

                    double rx = ex - proj * ix, ry = ey - proj * iy, rz = ez - proj * iz;
                    if (rx * rx + ry * ry + rz * rz > CYLINDER_RADIUS_SQ) continue;

                    int blockId = sampleBlock(world, bx, by, bz);
                    if (blockId == BlockType.EMPTY_ID) continue;
                    if (blockId == barrierBlockId && isBarrierProtected(bx, by, bz, feet.getX(), feetY, feet.getZ()))
                        continue;

                    currentPositions.add(packed);
                }
            }
        }

        // ── Replace newly entering blocks ────────────────────────────────────
        for (long packed : currentPositions) {
            int[] c = PlayerOcclusionComponent.unpack(packed);
            if (!occlusion.isReplaced(c[0], c[1], c[2])) {
                int originalId = sampleBlock(world, c[0], c[1], c[2]);
                if (originalId == BlockType.EMPTY_ID) continue;
                if (originalId == epicChestBlockId) continue;

                int originalRot = sampleRotation(world, c[0], c[1], c[2]);
                int originalFiller = sampleFiller(world, c[0], c[1], c[2]);

                if (isBarrierProtected(c[0], c[1], c[2], feet.getX(), feetY, feet.getZ())) {
                    double dy = (c[1] + 0.5) - feetY;
                    if (dy >= 0 && dy <= BARRIER_SOLID_HEIGHT) {
                        // Keep solid barrier so mobs cannot path through the hole
                        replaceWithBarrier(world, c[0], c[1], c[2]);
                        occlusion.markBarrierColumn(c[0], c[1], c[2]);
                    } else {
                        replaceWithAir(world, c[0], c[1], c[2]);
                    }
                } else {
                    replaceWithAir(world, c[0], c[1], c[2]);
                }

                occlusion.putReplaced(c[0], c[1], c[2], originalId, originalRot, originalFiller);
            }
        }

        // ── Immediately restore blocks that left the cylinder ─────────────────
        LongOpenHashSet previousPositions = occlusion.getReplacedPositions();
        for (long packed : previousPositions) {
            if (!currentPositions.contains(packed)) {
                int[] c = PlayerOcclusionComponent.unpack(packed);
                restoreBlock(world, c[0], c[1], c[2],
                    occlusion.getOriginalBlockId(c[0], c[1], c[2]),
                    occlusion.getOriginalRotation(c[0], c[1], c[2]),
                    occlusion.getOriginalFiller(c[0], c[1], c[2]));
                occlusion.removeReplaced(c[0], c[1], c[2]);
            }
        }

        cmd.run(s -> s.putComponent(ref, PlayerOcclusionComponent.getComponentType(), occlusion));
    }

    // ── Block sampling ────────────────────────────────────────────────────────

    private static int sampleBlock(World world, int x, int y, int z) {
        if (y < 0 || y >= 320) return BlockType.EMPTY_ID;
        WorldChunk chunk = world.getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(x, z));
        if (chunk == null) return BlockType.EMPTY_ID;
        return chunk.getBlock(x & 31, y, z & 31);
    }

    private static int sampleRotation(World world, int x, int y, int z) {
        if (y < 0 || y >= 320) return 0;
        WorldChunk chunk = world.getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(x, z));
        if (chunk == null) return 0;
        return chunk.getRotationIndex(x & 31, y, z & 31);
    }

    private static int sampleFiller(World world, int x, int y, int z) {
        if (y < 0 || y >= 320) return 0;
        WorldChunk chunk = world.getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(x, z));
        if (chunk == null) return 0;
        return chunk.getFiller(x & 31, y, z & 31);
    }

    // ── Block writing ─────────────────────────────────────────────────────────

    private static void replaceWithAir(World world, int x, int y, int z) {
        setBlock(world, x, y, z, BlockType.EMPTY_ID, BlockType.EMPTY, 0, 0);
    }

    private void replaceWithBarrier(World world, int x, int y, int z) {
        BlockType barrierType = BlockType.getAssetMap().getAsset(barrierBlockId);
        if (barrierType == null) return;
        setBlock(world, x, y, z, barrierBlockId, barrierType, 0, 0);
    }

    private static void restoreBlock(World world, int x, int y, int z, int blockId, int rotation, int filler) {
        BlockType bt = BlockType.getAssetMap().getAsset(blockId);
        if (bt == null) return;
        setBlock(world, x, y, z, blockId, bt, rotation, filler);
    }

    private static void setBlock(World world, int x, int y, int z,
                                 int blockId, @NonNullDecl BlockType blockType,
                                 int rotation, int filler) {
        WorldChunk chunk = world.getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(x, z));
        if (chunk == null) return;
        chunk.setBlock(x & 31, y, z & 31, blockId, blockType, rotation, filler, SILENT_SETTINGS);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static boolean isBarrierProtected(int bx, int by, int bz, double feetX, double feetY, double feetZ) {
        double dx = (bx + 0.5) - feetX;
        double dz = (bz + 0.5) - feetZ;
        double dy = (by + 0.5) - feetY;
        return dx * dx + dz * dz <= BARRIER_RADIUS_SQ
            && dy >= 0 && dy <= BARRIER_HEIGHT;
    }

    private void ensureBlockIdsCached() {
        if (barrierBlockId == Integer.MIN_VALUE) {
            barrierBlockId = BlockType.getAssetMap().getIndex("Barrier");
        }
        if (epicChestBlockId == Integer.MIN_VALUE) {
            epicChestBlockId = BlockType.getAssetMap().getIndex("Furniture_Dungeon_Chest_Epic");
        }
    }
}
