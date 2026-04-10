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
import fr.arnaud.nexus.core.Nexus;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.logging.Level;

/**
 * Manages camera occlusion by temporarily replacing blocks inside a cylinder
 * around the camera-to-player ray with Air every tick.
 *
 * <p>The ray extends from the camera to a point {@value #RAY_EXTENSION} blocks
 * past the player's head, so that low walls at head height are also cleared
 * and the player remains fully visible even when crouching behind them.
 *
 * <p>The cylinder has radius {@value #CYLINDER_RADIUS} blocks measured
 * perpendicularly from the ray axis. Blocks at or below
 * {@code playerFeetY + }{@value #FLOOR_Y_OFFSET} are never replaced.
 *
 * <p>Already-replaced positions are carried forward without re-sampling to
 * prevent the Air-read oscillation that would cause flicker.
 */
public final class CameraOcclusionSystem extends EntityTickingSystem<EntityStore> {

    private static final double CYLINDER_RADIUS = 5.0;
    private static final double CYLINDER_RADIUS_SQ = CYLINDER_RADIUS * CYLINDER_RADIUS;
    private static final double EYE_HEIGHT = 1.62;
    private static final double ISO_PITCH_RAD = Math.toRadians(-45.0);
    private static final double FLOOR_Y_OFFSET = 2.0;

    /**
     * How many blocks past the player's head the ray extends.
     * This ensures walls at exactly head height are included in the cylinder.
     */
    private static final double RAY_EXTENSION = 3.0;

    /**
     * setBlock settings: skip particles (4), skip block entity (2),
     * skip filler set (8), skip filler remove (16), skip height update (512).
     */
    private static final int SILENT_SETTINGS = 4 | 2 | 8 | 16 | 512;

    private int barrierBlockId = Integer.MIN_VALUE;

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

        ensureBarrierIdCached();

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

        // ── Ray: camera → past player head ───────────────────────────────────
        double rdx = headX - camX;
        double rdy = headY - camY;
        double rdz = headZ - camZ;
        double dist = Math.sqrt(rdx * rdx + rdy * rdy + rdz * rdz);
        if (dist < 1e-6) return;

        double ix = rdx / dist, iy = rdy / dist, iz = rdz / dist;

        // Ray end point: extend past the head so walls at head height are cleared
        double rayEnd = dist + RAY_EXTENSION;
        double endX = camX + ix * rayEnd;
        double endY = camY + iy * rayEnd;
        double endZ = camZ + iz * rayEnd;
        double floorY = feetY + FLOOR_Y_OFFSET;

        // ── Compute cylinder AABB using extended end point ───────────────────
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

                    // Perpendicular distance from block centre to ray line
                    double cx = bx + 0.5, cy = by + 0.5, cz = bz + 0.5;
                    double ex = cx - camX, ey = cy - camY, ez = cz - camZ;
                    double proj = ex * ix + ey * iy + ez * iz;
                    if (proj < 0 || proj > rayEnd) continue;
                    double rx = ex - proj * ix, ry = ey - proj * iy, rz = ez - proj * iz;
                    if (rx * rx + ry * ry + rz * rz > CYLINDER_RADIUS_SQ) continue;

                    int blockId = sampleBlock(world, bx, by, bz);
                    if (blockId == BlockType.EMPTY_ID || blockId == barrierBlockId) continue;

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
                replaceWithAir(world, c[0], c[1], c[2]);
                occlusion.putReplaced(c[0], c[1], c[2], originalId);
            }
        }

        // ── Immediately restore blocks that left the cylinder ─────────────────
        LongOpenHashSet previousPositions = occlusion.getReplacedPositions();
        for (long packed : previousPositions) {
            if (!currentPositions.contains(packed)) {
                int[] c = PlayerOcclusionComponent.unpack(packed);
                restoreBlock(world, c[0], c[1], c[2], occlusion.getOriginalBlockId(c[0], c[1], c[2]));
                occlusion.removeReplaced(c[0], c[1], c[2]);
            }
        }

        cmd.run(s -> s.putComponent(ref, PlayerOcclusionComponent.getComponentType(), occlusion));
    }

    private static void replaceWithAir(@NonNullDecl World world, int x, int y, int z) {
        setBlock(world, x, y, z, BlockType.EMPTY_ID, BlockType.EMPTY);
    }

    private static void restoreBlock(@NonNullDecl World world, int x, int y, int z, int blockId) {
        BlockType bt = BlockType.getAssetMap().getAsset(blockId);
        if (bt == null) return;
        setBlock(world, x, y, z, blockId, bt);
    }

    private static void setBlock(@NonNullDecl World world, int x, int y, int z,
                                 int blockId, @NonNullDecl BlockType blockType) {
        WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(x, z));
        if (chunk == null) return;
        chunk.setBlock(x & 31, y, z & 31, blockId, blockType, 0, 0, SILENT_SETTINGS);
    }

    private static int sampleBlock(@NonNullDecl World world, int x, int y, int z) {
        if (y < 0 || y >= 320) return BlockType.EMPTY_ID;
        WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(x, z));
        if (chunk == null) return BlockType.EMPTY_ID;
        return chunk.getBlock(x & 31, y, z & 31);
    }

    private void ensureBarrierIdCached() {
        if (barrierBlockId == Integer.MIN_VALUE) {
            barrierBlockId = BlockType.getAssetMap().getIndex("Barrier");
            Nexus.get().getLogger().at(Level.INFO).log("[HOVER] Barrier block ID cached as: %d", barrierBlockId);
        }
    }
}
