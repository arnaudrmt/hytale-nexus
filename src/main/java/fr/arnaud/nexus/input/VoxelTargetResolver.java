package fr.arnaud.nexus.input;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.util.TargetUtil;
import fr.arnaud.nexus.camera.CameraPacketBuilder;
import fr.arnaud.nexus.camera.PlayerOcclusionComponent;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;
import java.util.LinkedList;

/**
 * Resolves the true target block for a player click using the engine's own
 * {@link TargetUtil#getTargetBlockAvoidLocations} voxel traversal.
 *
 * <p>The ray originates from the reconstructed ISO camera position and travels
 * through the engine target and beyond. Barrier blocks are excluded via the
 * block predicate; occluded (Air-replaced) blocks are excluded via the avoid
 * positions list.
 */
public final class VoxelTargetResolver {

    private static final double EYE_HEIGHT = 1.62;
    private static final double ISO_PITCH_RAD = Math.toRadians(-45.0);
    private static final double SEARCH_RANGE = 48.0;

    private int barrierBlockId = Integer.MIN_VALUE;

    @Nullable
    public Vector3i resolve(@NonNullDecl Vector3d playerFeet,
                            @NonNullDecl Vector3i engineTarget,
                            float camDistance,
                            @Nullable PlayerOcclusionComponent occlusion,
                            @NonNullDecl World world) {
        ensureBarrierIdCached();

        // ── Reconstruct ISO camera world position ────────────────────────────
        double headX = playerFeet.getX();
        double headY = playerFeet.getY() + EYE_HEIGHT;
        double headZ = playerFeet.getZ();

        double yaw = CameraPacketBuilder.ISO_YAW_RAD;
        double cosPitch = Math.cos(ISO_PITCH_RAD);
        double sinPitch = Math.sin(ISO_PITCH_RAD);

        double camX = headX + Math.sin(yaw) * cosPitch * camDistance;
        double camY = headY + (-sinPitch) * camDistance;
        double camZ = headZ + Math.cos(yaw) * cosPitch * camDistance;

        // ── Ray direction: camera → engineTarget ─────────────────────────────
        double dx = engineTarget.getX() + 0.5 - camX;
        double dy = engineTarget.getY() + 0.5 - camY;
        double dz = engineTarget.getZ() + 0.5 - camZ;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-6) return null;

        double ix = dx / len;
        double iy = dy / len;
        double iz = dz / len;

        // ── Avoid list: positions replaced with Air by occlusion system ───────
        LongOpenHashSet occludedPositions = occlusion != null
            ? occlusion.getBlockUtilPackedPositions()
            : new LongOpenHashSet();

        LinkedList<LongOpenHashSet> avoidList = new LinkedList<>();
        avoidList.add(occludedPositions);

        // Barrier is excluded via the predicate — it is a block type, not a
        // specific position, so it can't be listed in the avoid set.
        final int barrierIdFinal = barrierBlockId;

        return TargetUtil.getTargetBlockAvoidLocations(
            world,
            blockId -> blockId != BlockType.EMPTY_ID && blockId != barrierIdFinal,
            camX, camY, camZ,
            ix, iy, iz,
            SEARCH_RANGE,
            avoidList
        );
    }

    private void ensureBarrierIdCached() {
        if (barrierBlockId == Integer.MIN_VALUE) {
            barrierBlockId = BlockType.getAssetMap().getIndex("Barrier");
        }
    }
}
