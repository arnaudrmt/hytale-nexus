package fr.arnaud.nexus.math;

import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import fr.arnaud.nexus.camera.CameraPacketBuilder;
import org.jetbrains.annotations.NotNull;

public record WorldPosition(double x, double y, double z) {

    public Vector3d toVector3d() {
        return new Vector3d(x, y, z);
    }

    public Transform toSpawnTransform() {
        return new Transform(toVector3d(), new Vector3f(0f, CameraPacketBuilder.ISO_CAMERA_YAW_RAD, 0f));
    }

    @NotNull
    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }
}
