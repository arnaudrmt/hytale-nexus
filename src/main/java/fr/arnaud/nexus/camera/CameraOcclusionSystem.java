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
import com.hypixel.hytale.server.core.entity.entities.ProjectileComponent;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.spawner.SpawnerTagComponent;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.ArrayList;
import java.util.List;

public final class CameraOcclusionSystem extends EntityTickingSystem<EntityStore> {

    private static final double CYLINDER_RADIUS = 5.0;
    private static final double CYLINDER_RADIUS_SQ = CYLINDER_RADIUS * CYLINDER_RADIUS;
    private static final double EYE_HEIGHT = 1.62;
    private static final double ISO_PITCH_RAD = Math.toRadians(-45.0);
    private static final double FLOOR_Y_OFFSET = 0.0;

    /**
     * How many extra blocks past the player's head the AABB extends.
     */
    private static final double RAY_EXTENSION = 3.0;

    /**
     * setBlock settings: skip particles (4), skip block entity (2),
     * skip filler set (8), skip filler remove (16), skip height update (512).
     */
    private static final int SILENT_SETTINGS = 4 | 2 | 8 | 16 | 512;

    private static final double BARRIER_RADIUS = 2.0;
    private static final double BARRIER_RADIUS_SQ = BARRIER_RADIUS * BARRIER_RADIUS;
    private static final double BARRIER_HEIGHT = 2.0;

    private int barrierBlockId = Integer.MIN_VALUE;
    private int epicChestBlockId = Integer.MIN_VALUE;

    private static final boolean DEBUG_OCCLUSION = false;

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
        if (occlusion.isDestroyed()) return;

        World world = ((EntityStore) store.getExternalData()).getWorld();

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

        double rdx = headX - camX, rdy = headY - camY, rdz = headZ - camZ;
        double dist = Math.sqrt(rdx * rdx + rdy * rdy + rdz * rdz);
        if (dist < 1e-6) return;

        double ix = rdx / dist, iy = rdy / dist, iz = rdz / dist;

        double rayEnd = dist + RAY_EXTENSION;
        double endX = camX + ix * rayEnd;
        double endY = camY + iy * rayEnd;
        double endZ = camZ + iz * rayEnd;
        double floorY = feetY + FLOOR_Y_OFFSET;

        int bbR = (int) Math.ceil(CYLINDER_RADIUS);
        int minX = (int) Math.floor(Math.min(camX, endX)) - bbR;
        int maxX = (int) Math.ceil(Math.max(camX, endX)) + bbR;
        int minY = (int) Math.floor(Math.min(camY, endY)) - bbR;
        int maxY = (int) Math.ceil(Math.max(camY, endY)) + bbR;
        int minZ = (int) Math.floor(Math.min(camZ, endZ)) - bbR;
        int maxZ = (int) Math.ceil(Math.max(camZ, endZ)) + bbR;

        /*
         * Snapshot all mob positions once per tick to avoid per-block store queries
         */
        List<Vector3d> mobPositions = collectMobPositions(store);
        List<Vector3d> projectilePositions = collectProjectilePositions(store);

        LongOpenHashSet currentPositions = new LongOpenHashSet();

        for (int bx = minX; bx <= maxX; bx++) {
            for (int by = minY; by <= maxY; by++) {
                if (by <= floorY || by < 0 || by >= 320) continue;
                for (int bz = minZ; bz <= maxZ; bz++) {
                    long packed = PlayerOcclusionComponent.pack(bx, by, bz);

                    if (occlusion.getReplacedBlocks().containsKey(packed)) {
                        currentPositions.add(packed);
                        continue;
                    }

                    double cx = bx + 0.5, cy = by + 0.5, cz = bz + 0.5;
                    double ex = cx - camX, ey = cy - camY, ez = cz - camZ;
                    double proj = ex * ix + ey * iy + ez * iz;

                    if (proj < 0 || proj > dist) continue;

                    double rx = ex - proj * ix, ry = ey - proj * iy, rz = ez - proj * iz;
                    if (rx * rx + ry * ry + rz * rz > CYLINDER_RADIUS_SQ) continue;

                    int blockId = sampleBlock(world, bx, by, bz);
                    if (blockId == BlockType.EMPTY_ID) continue;

                    currentPositions.add(packed);
                }
            }
        }

        for (long packed : currentPositions) {
            int[] c = PlayerOcclusionComponent.unpack(packed);

            if (!occlusion.isReplaced(c[0], c[1], c[2])) {
                int originalId = sampleBlock(world, c[0], c[1], c[2]);
                if (originalId == BlockType.EMPTY_ID) continue;
                if (originalId == epicChestBlockId) continue;

                int originalRot = sampleRotation(world, c[0], c[1], c[2]);
                int originalFiller = sampleFiller(world, c[0], c[1], c[2]);
                occlusion.putReplaced(c[0], c[1], c[2], originalId, originalRot, originalFiller);

                if (isNearPlayerOrMob(c[0], c[1], c[2], feet, mobPositions, projectilePositions)) {
                    replaceWithBarrier(world, c[0], c[1], c[2]);
                } else {
                    replaceWithAir(world, c[0], c[1], c[2]);
                }

            } else {
                int currentBlock = sampleBlock(world, c[0], c[1], c[2]);
                boolean shouldBeSolid = isNearPlayerOrMob(c[0], c[1], c[2], feet, mobPositions, projectilePositions);
                boolean isSolid = (currentBlock == barrierBlockId)
                    || (DEBUG_OCCLUSION && currentBlock == debugBarrierBlockId);

                if (shouldBeSolid && !isSolid) {
                    replaceWithBarrier(world, c[0], c[1], c[2]);
                } else if (!shouldBeSolid && isSolid) {
                    replaceWithAir(world, c[0], c[1], c[2]);
                }
            }
        }

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

    private static List<Vector3d> collectMobPositions(Store<EntityStore> store) {
        List<Vector3d> positions = new ArrayList<>();
        store.forEachChunk(
            SpawnerTagComponent.getComponentType(),
            (archetypeChunk, cmd) -> {
                for (int i = 0; i < archetypeChunk.size(); i++) {
                    Ref<EntityStore> mobRef = archetypeChunk.getReferenceTo(i);
                    TransformComponent t = store.getComponent(mobRef, EntityModule.get().getTransformComponentType());
                    if (t != null) positions.add(t.getPosition());
                }
            }
        );
        return positions;
    }

    private static List<Vector3d> collectProjectilePositions(Store<EntityStore> store) {
        List<Vector3d> positions = new ArrayList<>();
        store.forEachChunk(
            ProjectileComponent.getComponentType(),
            (archetypeChunk, cmd) -> {
                for (int i = 0; i < archetypeChunk.size(); i++) {
                    Ref<EntityStore> projRef = archetypeChunk.getReferenceTo(i);
                    TransformComponent t = store.getComponent(projRef, EntityModule.get().getTransformComponentType());
                    if (t != null) positions.add(t.getPosition());
                }
            }
        );
        return positions;
    }

    private static boolean isNearPlayerOrMob(int bx, int by, int bz, Vector3d playerFeet,
                                             List<Vector3d> mobPositions,
                                             List<Vector3d> projectilePositions) {
        double cx = bx + 0.5, cy = by + 0.5, cz = bz + 0.5;

        double pdx = cx - playerFeet.getX();
        double pdz = cz - playerFeet.getZ();
        if (pdx * pdx + pdz * pdz <= BARRIER_RADIUS_SQ) return true;

        for (Vector3d mob : mobPositions) {
            double mdx = cx - mob.getX();
            double mdz = cz - mob.getZ();
            if (mdx * mdx + mdz * mdz <= BARRIER_RADIUS_SQ) return true;
        }

        for (Vector3d proj : projectilePositions) {
            double pjdx = cx - proj.getX();
            double pjdy = cy - proj.getY();
            double pjdz = cz - proj.getZ();
            if (pjdx * pjdx + pjdy * pjdy + pjdz * pjdz <= BARRIER_RADIUS_SQ) return true;
        }

        return false;
    }

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

    private void replaceWithAir(World world, int x, int y, int z) {
        if (DEBUG_OCCLUSION) {
            setBlock(world, x, y, z, debugAirBlockId, BlockType.getAssetMap().getAsset(debugAirBlockId), 0, 0);
        } else {
            setBlock(world, x, y, z, BlockType.EMPTY_ID, BlockType.EMPTY, 0, 0);
        }
    }

    private void replaceWithBarrier(World world, int x, int y, int z) {
        int id = DEBUG_OCCLUSION ? debugBarrierBlockId : barrierBlockId;
        BlockType bt = BlockType.getAssetMap().getAsset(id);
        if (bt == null) return;
        setBlock(world, x, y, z, id, bt, 0, 0);
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

    private int debugAirBlockId = Integer.MIN_VALUE;
    private int debugBarrierBlockId = Integer.MIN_VALUE;

    private void ensureBlockIdsCached() {
        if (barrierBlockId == Integer.MIN_VALUE)
            barrierBlockId = BlockType.getAssetMap().getIndex("Barrier");
        if (epicChestBlockId == Integer.MIN_VALUE)
            epicChestBlockId = BlockType.getAssetMap().getIndex("Furniture_Dungeon_Chest_Epic");
        if (debugAirBlockId == Integer.MIN_VALUE)
            debugAirBlockId = BlockType.getAssetMap().getIndex("Cloth_Block_Wool_Green");
        if (debugBarrierBlockId == Integer.MIN_VALUE)
            debugBarrierBlockId = BlockType.getAssetMap().getIndex("Cloth_Block_Wool_Red");
    }
}
