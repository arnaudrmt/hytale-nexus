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

    private static final double OCCLUSION_CYLINDER_RADIUS = 5.0;
    private static final double OCCLUSION_CYLINDER_RADIUS_SQ = OCCLUSION_CYLINDER_RADIUS * OCCLUSION_CYLINDER_RADIUS;
    private static final double PLAYER_EYE_HEIGHT = 1.62;
    private static final double ISO_CAMERA_PITCH_RAD = Math.toRadians(-45.0);
    private static final double OCCLUSION_FLOOR_Y_OFFSET = 0.0;

    // How many extra blocks past the player's head the AABB extends.
    private static final double OCCLUSION_RAY_EXTENSION_BLOCKS = 3.0;

    /**
     * setBlock settings: skip particles (4), skip block entity (2),
     * skip filler set (8), skip filler remove (16), skip height update (512).
     */
    private static final int BLOCK_SET_SILENT_FLAGS = 4 | 2 | 8 | 16 | 512;

    private static final double VISIBILITY_BARRIER_RADIUS = 2.0;
    private static final double VISIBILITY_BARRIER_RADIUS_SQ = VISIBILITY_BARRIER_RADIUS * VISIBILITY_BARRIER_RADIUS;

    private int cachedBarrierBlockId = Integer.MIN_VALUE;
    private int cachedEpicChestBlockId = Integer.MIN_VALUE;

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

        cacheBlockIdsIfNeeded();

        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        PlayerCameraComponent camera = chunk.getComponent(index, PlayerCameraComponent.getComponentType());
        PlayerOcclusionComponent occlusion = chunk.getComponent(index, PlayerOcclusionComponent.getComponentType());
        TransformComponent transform = store.getComponent(ref, EntityModule.get().getTransformComponentType());

        if (camera == null || occlusion == null || transform == null) return;
        if (occlusion.isDestroyed()) return;

        World world = store.getExternalData().getWorld();

        Vector3d playerFeetPosition = transform.getPosition();
        double playerFeetY = playerFeetPosition.getY();
        double playerEyeX = playerFeetPosition.getX();
        double playerEyeY = playerFeetY + PLAYER_EYE_HEIGHT;
        double playerEyeZ = playerFeetPosition.getZ();
        float isoDistanceBlocks = camera.getEffectiveIsoDistance();

        double isoCameraYaw = CameraPacketBuilder.ISO_CAMERA_YAW_RAD;
        double isoPitchCos = Math.cos(ISO_CAMERA_PITCH_RAD);
        double isoPitchSin = Math.sin(ISO_CAMERA_PITCH_RAD);

        double cameraOriginX = playerEyeX + Math.sin(isoCameraYaw) * isoPitchCos * isoDistanceBlocks;
        double cameraOriginY = playerEyeY + (-isoPitchSin) * isoDistanceBlocks;
        double cameraOriginZ = playerEyeZ + Math.cos(isoCameraYaw) * isoPitchCos * isoDistanceBlocks;

        double rayDirectionX = playerEyeX - cameraOriginX;
        double rayDirectionY = playerEyeY - cameraOriginY;
        double rayDirectionZ = playerEyeZ - cameraOriginZ;

        double rayLength = Math.sqrt(rayDirectionX * rayDirectionX + rayDirectionY * rayDirectionY + rayDirectionZ * rayDirectionZ);
        if (rayLength < 1e-6) return;

        double rayNormX = rayDirectionX / rayLength;
        double rayNormY = rayDirectionY / rayLength;
        double rayNormZ = rayDirectionZ / rayLength;

        double extendedRayLength = rayLength + OCCLUSION_RAY_EXTENSION_BLOCKS;
        double rayEndX = cameraOriginX + rayNormX * extendedRayLength;
        double rayEndY = cameraOriginY + rayNormY * extendedRayLength;
        double rayEndZ = cameraOriginZ + rayNormZ * extendedRayLength;
        double occlusionFloorY = playerFeetY + OCCLUSION_FLOOR_Y_OFFSET;

        int boundingBoxPadding = (int) Math.ceil(OCCLUSION_CYLINDER_RADIUS);
        int minX = (int) Math.floor(Math.min(cameraOriginX, rayEndX)) - boundingBoxPadding;
        int maxX = (int) Math.ceil(Math.max(cameraOriginX, rayEndX)) + boundingBoxPadding;
        int minY = (int) Math.floor(Math.min(cameraOriginY, rayEndY)) - boundingBoxPadding;
        int maxY = (int) Math.ceil(Math.max(cameraOriginY, rayEndY)) + boundingBoxPadding;
        int minZ = (int) Math.floor(Math.min(cameraOriginZ, rayEndZ)) - boundingBoxPadding;
        int maxZ = (int) Math.ceil(Math.max(cameraOriginZ, rayEndZ)) + boundingBoxPadding;

        // Snapshot all mob positions once per tick to avoid per-block store queries
        List<Vector3d> entityPositions = collectEntityPositions(store);

        LongOpenHashSet occludedPositionsThisTick = new LongOpenHashSet();

        for (int bx = minX; bx <= maxX; bx++) {
            for (int by = minY; by <= maxY; by++) {
                if (by <= occlusionFloorY || by < 0 || by >= 320) continue;
                for (int bz = minZ; bz <= maxZ; bz++) {
                    long packed = PlayerOcclusionComponent.pack(bx, by, bz);

                    if (occlusion.getReplacedBlocks().containsKey(packed)) {
                        occludedPositionsThisTick.add(packed);
                        continue;
                    }

                    if (!isBlockInOcclusionCylinder(bx + 0.5, by + 0.5, bz + 0.5,
                        cameraOriginX, cameraOriginY, cameraOriginZ,
                        rayNormX, rayNormY, rayNormZ, rayLength)) continue;

                    int blockId = sampleBlock(world, bx, by, bz);
                    if (blockId == BlockType.EMPTY_ID) continue;

                    occludedPositionsThisTick.add(packed);
                }
            }
        }

        for (long packed : occludedPositionsThisTick) {
            int[] c = PlayerOcclusionComponent.unpack(packed);

            if (!occlusion.isReplaced(c[0], c[1], c[2])) {
                int originalId = sampleBlock(world, c[0], c[1], c[2]);
                if (originalId == BlockType.EMPTY_ID) continue;
                if (originalId == cachedEpicChestBlockId) continue;

                int originalRot = sampleRotation(world, c[0], c[1], c[2]);
                int originalFiller = sampleFiller(world, c[0], c[1], c[2]);
                occlusion.putReplaced(c[0], c[1], c[2], originalId, originalRot, originalFiller);

                if (isNearPlayerOrMob(c[0], c[1], c[2], playerFeetPosition, entityPositions)) {
                    replaceWithBarrier(world, c[0], c[1], c[2]);
                } else {
                    replaceWithAir(world, c[0], c[1], c[2]);
                }

            } else {
                int currentBlock = sampleBlock(world, c[0], c[1], c[2]);
                boolean shouldBeSolid = isNearPlayerOrMob(c[0], c[1], c[2], playerFeetPosition, entityPositions);
                boolean isSolid = currentBlock == cachedBarrierBlockId;

                if (shouldBeSolid && !isSolid) {
                    replaceWithBarrier(world, c[0], c[1], c[2]);
                } else if (!shouldBeSolid && isSolid) {
                    replaceWithAir(world, c[0], c[1], c[2]);
                }
            }
        }

        LongOpenHashSet previouslyOccludedPositions = occlusion.getReplacedPositions();
        for (long packedBlockPos : previouslyOccludedPositions) {
            if (!occludedPositionsThisTick.contains(packedBlockPos)) {
                int[] blockCoords = PlayerOcclusionComponent.unpack(packedBlockPos);
                BlockWorldUtil.restoreBlock(world, blockCoords[0], blockCoords[1], blockCoords[2],
                    occlusion.getOriginalBlockId(blockCoords[0], blockCoords[1], blockCoords[2]),
                    occlusion.getOriginalRotation(blockCoords[0], blockCoords[1], blockCoords[2]),
                    occlusion.getOriginalFiller(blockCoords[0], blockCoords[1], blockCoords[2]));
                occlusion.removeReplaced(blockCoords[0], blockCoords[1], blockCoords[2]);
            }
        }

        cmd.run(s -> s.putComponent(ref, PlayerOcclusionComponent.getComponentType(), occlusion));
    }

    private static boolean isBlockInOcclusionCylinder(
        double blockCenterX, double blockCenterY, double blockCenterZ,
        double cameraOriginX, double cameraOriginY, double cameraOriginZ,
        double rayNormX, double rayNormY, double rayNormZ,
        double rayLength) {
        double ex = blockCenterX - cameraOriginX;
        double ey = blockCenterY - cameraOriginY;
        double ez = blockCenterZ - cameraOriginZ;
        double proj = ex * rayNormX + ey * rayNormY + ez * rayNormZ;
        if (proj < 0 || proj > rayLength) return false;
        double rx = ex - proj * rayNormX;
        double ry = ey - proj * rayNormY;
        double rz = ez - proj * rayNormZ;
        return rx * rx + ry * ry + rz * rz <= OCCLUSION_CYLINDER_RADIUS_SQ;
    }

    private static List<Vector3d> collectEntityPositions(Store<EntityStore> store) {
        List<Vector3d> positions = new ArrayList<>();
        store.forEachChunk(
            Query.or(SpawnerTagComponent.getComponentType(), ProjectileComponent.getComponentType()),
            (archetypeChunk, _) -> {
                for (int i = 0; i < archetypeChunk.size(); i++) {
                    Ref<EntityStore> mobRef = archetypeChunk.getReferenceTo(i);
                    TransformComponent t = store.getComponent(mobRef, EntityModule.get().getTransformComponentType());
                    if (t != null) positions.add(t.getPosition());
                }
            }
        );
        return positions;
    }

    private static boolean isNearPlayerOrMob(int bx, int by, int bz, Vector3d playerFeet,
                                             List<Vector3d> entityPositions) {
        double blockCenterX = bx + 0.5;
        double blockCenterY = by + 0.5;
        double blockCenterZ = bz + 0.5;

        double playerDeltaX = blockCenterX - playerFeet.getX();
        double playerDeltaZ = blockCenterZ - playerFeet.getZ();
        if (playerDeltaX * playerDeltaX + playerDeltaZ * playerDeltaZ <= VISIBILITY_BARRIER_RADIUS_SQ) return true;

        for (Vector3d entity : entityPositions) {
            double dx = blockCenterX - entity.getX();
            double dy = blockCenterY - entity.getY();
            double dz = blockCenterZ - entity.getZ();
            if (dx * dx + dy * dy + dz * dz <= VISIBILITY_BARRIER_RADIUS_SQ) return true;
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
        setBlock(world, x, y, z, BlockType.EMPTY_ID, BlockType.EMPTY);
    }

    private void replaceWithBarrier(World world, int x, int y, int z) {
        int id = cachedBarrierBlockId;
        BlockType bt = BlockType.getAssetMap().getAsset(id);
        if (bt == null) return;
        setBlock(world, x, y, z, id, bt);
    }

    private static void setBlock(World world, int x, int y, int z,
                                 int blockId, @NonNullDecl BlockType blockType) {
        WorldChunk chunk = world.getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(x, z));
        if (chunk == null) return;
        chunk.setBlock(x & 31, y, z & 31, blockId, blockType, 0, 0, BLOCK_SET_SILENT_FLAGS);
    }

    private int debugAirBlockId = Integer.MIN_VALUE;
    private int debugBarrierBlockId = Integer.MIN_VALUE;

    private void cacheBlockIdsIfNeeded() {
        if (cachedBarrierBlockId == Integer.MIN_VALUE)
            cachedBarrierBlockId = BlockType.getAssetMap().getIndex("Barrier");
        if (cachedEpicChestBlockId == Integer.MIN_VALUE)
            cachedEpicChestBlockId = BlockType.getAssetMap().getIndex("Furniture_Dungeon_Chest_Epic");
        if (debugAirBlockId == Integer.MIN_VALUE)
            debugAirBlockId = BlockType.getAssetMap().getIndex("Cloth_Block_Wool_Green");
        if (debugBarrierBlockId == Integer.MIN_VALUE)
            debugBarrierBlockId = BlockType.getAssetMap().getIndex("Cloth_Block_Wool_Red");
    }
}
