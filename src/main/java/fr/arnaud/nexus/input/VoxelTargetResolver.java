package fr.arnaud.nexus.input;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.util.TargetUtil;
import fr.arnaud.nexus.camera.CameraPacketBuilder;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;
import java.util.LinkedList;

/**
 * Resolves the true target block by recasting from the reconstructed ISO camera
 * position. Barriers and occluded blocks are already Air in the world by the
 * time this runs, so no predicate or avoid list is needed.
 */
public final class VoxelTargetResolver {

    private static final double EYE_HEIGHT = 1.62;
    private static final double ISO_PITCH_RAD = Math.toRadians(-45.0);
    private static final double SEARCH_RANGE = 48.0;

    @Nullable
    public Vector3i resolve(@NonNullDecl Vector3d playerFeet,
                            @NonNullDecl Vector3i engineTarget,
                            float camDistance,
                            @NonNullDecl World world) {
        double headX = playerFeet.getX();
        double headY = playerFeet.getY() + EYE_HEIGHT;
        double headZ = playerFeet.getZ();

        double yaw = CameraPacketBuilder.ISO_YAW_RAD;
        double cosPitch = Math.cos(ISO_PITCH_RAD);
        double sinPitch = Math.sin(ISO_PITCH_RAD);

        double camX = headX + Math.sin(yaw) * cosPitch * camDistance;
        double camY = headY + (-sinPitch) * camDistance;
        double camZ = headZ + Math.cos(yaw) * cosPitch * camDistance;

        double dx = engineTarget.getX() + 0.5 - camX;
        double dy = engineTarget.getY() + 0.5 - camY;
        double dz = engineTarget.getZ() + 0.5 - camZ;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-6) return null;

        return TargetUtil.getTargetBlockAvoidLocations(
            world,
            blockId -> blockId != BlockType.EMPTY_ID,
            camX, camY, camZ,
            dx / len, dy / len, dz / len,
            SEARCH_RANGE,
            new LinkedList<>()
        );
    }
}
