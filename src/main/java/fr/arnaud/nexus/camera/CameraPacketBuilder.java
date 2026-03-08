package fr.arnaud.nexus.camera;

import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.protocol.packets.camera.SetServerCamera;

/**
 * Factory class for creating {@link SetServerCamera} packets.
 * Handles the state transitions between ISO and First-Person views.
 */
public final class CameraPacketBuilder {

    // --- Camera geometry constants ---
    public static final float ISO_YAW_RAD = (float) Math.toRadians(225.0);
    private static final float ISO_PITCH_RAD = (float) Math.toRadians(-45.0);

    // --- Lerp speeds for positional and rotational smoothing

    private static final float ISO_POS_LERP = 0.1f;
    private static final float TRANSITION_POS_LERP = 0.75f;
    private static final float TRANSITION_ROT_LERP = 0.85f;

    private CameraPacketBuilder() {
    }

    /**
     * Builds the packet for the default Isometric view.
     * <p>
     * IMPORTANT: {@code movementForceRotation} must match the camera yaw so the
     * client remaps WASD axes relative to the camera, not the player's body yaw.
     * Without this, pressing S when the character faces the camera moves them in
     * the wrong direction (controls appear flipped at ±180° body yaw).
     */
    public static SetServerCamera buildIso(CameraComponent cam) {
        ServerCameraSettings s = new ServerCameraSettings();

        s.isFirstPerson = false;
        s.distance = cam.getEffectiveIsoDistance();
        s.rotationType = RotationType.Custom;
        s.rotation = new Direction(ISO_YAW_RAD, ISO_PITCH_RAD, 0f);

        // Align WASD input axes to the fixed camera yaw so movement is always
        // camera-relative, regardless of the player's current body orientation.
        s.movementForceRotation = new Direction(ISO_YAW_RAD, 0f, 0f);

        /*
         * LocalPlayerYawOrientation restricts the entity to horizontal look only,
         * which prevents the camera from snapping to vertical aim while in ISO.
         */
        s.applyLookType = ApplyLookType.LocalPlayerLookOrientation;
        s.allowPitchControls = false;

        s.mouseInputTargetType = MouseInputTargetType.Any;
        s.mouseInputType = MouseInputType.LookAtTarget;
        s.sendMouseMotion = true;
        s.displayReticle = false;
        s.displayCursor = true;

        applyStandardPhysics(s, ISO_POS_LERP, 1.0f);
        return new SetServerCamera(ClientCameraView.Custom, true, s);
    }

    /**
     * Builds the packet for the First-Person Glimpse state.
     */
    public static SetServerCamera buildGlimpseActive() {
        ServerCameraSettings s = new ServerCameraSettings();

        s.isFirstPerson = true;
        s.distance = 0f;
        s.eyeOffset = true;

        s.rotationType = RotationType.AttachedToPlusOffset;
        s.applyLookType = ApplyLookType.LocalPlayerLookOrientation;
        s.allowPitchControls = true;

        s.mouseInputTargetType = MouseInputTargetType.Any;
        s.mouseInputType = MouseInputType.LookAtTarget;
        s.sendMouseMotion = true;
        s.displayReticle = true;
        s.displayCursor = false;

        applyStandardPhysics(s, 1.0f, 1.0f);
        return new SetServerCamera(ClientCameraView.Custom, true, s);
    }

    /**
     * Builds the packet for the ISO → Glimpse transition.
     *
     * @param t Normalized transition progress (0.0 to 1.0).
     */
    public static SetServerCamera buildEntryTransition(float t, float isoDistance) {
        float eased = easeInOutCubic(t);
        boolean inFps = eased >= 0.95f;

        ServerCameraSettings s = new ServerCameraSettings();

        s.distance = lerp(isoDistance, 0f, eased);
        s.isFirstPerson = inFps;
        s.eyeOffset = inFps;

        s.rotationType = RotationType.Custom;
        s.rotation = new Direction(ISO_YAW_RAD, lerp(ISO_PITCH_RAD, 0f, eased), 0f);

        // --- Restore Yaw orientation only when fully transitioned ---
        s.applyLookType = inFps ? ApplyLookType.LocalPlayerLookOrientation : ApplyLookType.Rotation;
        s.allowPitchControls = inFps;

        s.mouseInputTargetType = inFps ? MouseInputTargetType.Any : MouseInputTargetType.None;
        s.sendMouseMotion = true;
        s.displayReticle = inFps;
        s.mouseInputType = MouseInputType.LookAtTarget;
        s.displayCursor = false;

        applyStandardPhysics(s, TRANSITION_POS_LERP, TRANSITION_ROT_LERP);
        return new SetServerCamera(ClientCameraView.Custom, true, s);
    }

    /**
     * Builds the packet for the Glimpse → ISO exit transition.
     *
     * @param t Normalized transition progress (0.0 to 1.0).
     */
    public static SetServerCamera buildExitTransition(float t, float isoDistance) {
        float eased = easeInOutQuad(t);
        boolean inFps = eased < 0.05f;

        ServerCameraSettings s = new ServerCameraSettings();
        s.distance = lerp(0f, isoDistance, eased);
        s.isFirstPerson = inFps;
        s.eyeOffset = inFps;

        s.rotationType = RotationType.Custom;
        s.rotation = new Direction(ISO_YAW_RAD, lerp(0f, ISO_PITCH_RAD, eased), 0f);

        s.applyLookType = inFps ? ApplyLookType.LocalPlayerLookOrientation : ApplyLookType.Rotation;
        s.allowPitchControls = inFps;
        s.mouseInputTargetType = inFps ? MouseInputTargetType.Any : MouseInputTargetType.None;
        s.sendMouseMotion = true;
        s.displayReticle = inFps;
        s.mouseInputType = MouseInputType.LookAtTarget;
        s.displayCursor = eased > 0.5f;

        applyStandardPhysics(s, TRANSITION_POS_LERP, TRANSITION_ROT_LERP);
        return new SetServerCamera(ClientCameraView.Custom, true, s);
    }

    /**
     * Helper to apply default movement/attachment settings to the settings object.
     * This reduces code duplication significantly.
     */
    private static void applyStandardPhysics(ServerCameraSettings s, float posLerp, float rotLerp) {
        s.positionLerpSpeed = posLerp;
        s.rotationLerpSpeed = rotLerp;
        s.positionDistanceOffsetType = PositionDistanceOffsetType.DistanceOffset;
        s.attachedToType = AttachedToType.LocalPlayer;
        s.positionType = PositionType.AttachedToPlusOffset;
        s.canMoveType = CanMoveType.AttachedToLocalPlayer;
        s.applyMovementType = ApplyMovementType.CharacterController;
    }

    /**
     * Releases server camera control, restoring the client's default camera behavior.
     */
    public static SetServerCamera buildReset() {
        return new SetServerCamera(ClientCameraView.Custom, false, null);
    }

    // --- Math Utilities ---

    /**
     * Easing function: Cubic S-curve for snappy movement.
     */
    public static float easeInOutCubic(float t) {
        if (t < 0.5f) return 4f * t * t * t;
        float f = -2f * t + 2f;
        return 1f - (f * f * f) / 2f;
    }

    /**
     * Easing function: Quadratic S-curve for gentle movement.
     */
    public static float easeInOutQuad(float t) {
        return t < 0.5f ? 2f * t * t : 1f - (-2f * t + 2f) * (-2f * t + 2f) / 2f;
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
