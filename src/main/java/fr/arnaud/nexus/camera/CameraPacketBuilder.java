package fr.arnaud.nexus.camera;

import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.protocol.packets.camera.SetServerCamera;

/**
 * Handles the state transitions between ISO and First-Person views.
 */
public final class CameraPacketBuilder {

    public static final float ISO_CAMERA_YAW_RAD = (float) Math.toRadians(225.0);
    private static final float ISO_CAMERA_PITCH_RAD = (float) Math.toRadians(-45.0);

    private static final float ISO_POSITION_LERP_SPEED = 0.1f;
    private static final float TRANSITION_POSITION_LERP_SPEED = 0.75f;
    private static final float TRANSITION_ROTATION_LERP_SPEED = 0.85f;

    private CameraPacketBuilder() {
    }

    public static SetServerCamera buildIso(PlayerCameraComponent cam) {
        ServerCameraSettings s = new ServerCameraSettings();

        s.isFirstPerson = false;
        s.distance = cam.getEffectiveIsoDistance();
        s.rotationType = RotationType.Custom;
        s.rotation = new Direction(ISO_CAMERA_YAW_RAD, ISO_CAMERA_PITCH_RAD, 0f);
        s.movementForceRotation = new Direction(ISO_CAMERA_YAW_RAD, 0f, 0f);

        s.applyLookType = ApplyLookType.Rotation;

        s.mouseInputTargetType = MouseInputTargetType.Any;
        s.mouseInputType = MouseInputType.LookAtPlane;
        s.planeNormal = new Vector3f(0f, 1f, 0f);
        s.sendMouseMotion = true;
        s.displayReticle = false;
        s.displayCursor = true;

        applyStandardPhysics(s, ISO_POSITION_LERP_SPEED, 1.0f);
        return new SetServerCamera(ClientCameraView.Custom, true, s);
    }

    public static SetServerCamera buildGlimpseActive() {
        return new SetServerCamera(ClientCameraView.FirstPerson, true, null);
    }

    public static SetServerCamera buildEntryTransition(float progress, float baseIsoDistance) {
        float eased = easeInOutCubic(progress);

        ServerCameraSettings cameraSettings = buildTransitionSettings(lerp(baseIsoDistance, 0f, eased), lerp(ISO_CAMERA_PITCH_RAD, 0f, eased));
        applyStandardPhysics(cameraSettings, TRANSITION_POSITION_LERP_SPEED, TRANSITION_ROTATION_LERP_SPEED);

        return new SetServerCamera(ClientCameraView.Custom, true, cameraSettings);
    }

    public static SetServerCamera buildExitTransition(float progress, float baseIsoDistance) {
        float eased = easeInOutQuad(progress);

        ServerCameraSettings cameraSettings = buildTransitionSettings(lerp(0f, baseIsoDistance, eased), lerp(0f, ISO_CAMERA_PITCH_RAD, eased));
        applyStandardPhysics(cameraSettings, TRANSITION_POSITION_LERP_SPEED, TRANSITION_ROTATION_LERP_SPEED);

        return new SetServerCamera(ClientCameraView.Custom, true, cameraSettings);
    }

    private static ServerCameraSettings buildTransitionSettings(float distance, float pitchRad) {
        ServerCameraSettings s = new ServerCameraSettings();
        s.distance = distance;
        s.isFirstPerson = false;
        s.eyeOffset = false;
        s.rotationType = RotationType.Custom;
        s.rotation = new Direction(ISO_CAMERA_YAW_RAD, pitchRad, 0f);
        s.applyLookType = ApplyLookType.Rotation;
        s.mouseInputTargetType = MouseInputTargetType.None;
        s.mouseInputType = MouseInputType.LookAtTarget;
        s.sendMouseMotion = true;
        s.displayReticle = false;
        s.displayCursor = true;
        applyStandardPhysics(s, TRANSITION_POSITION_LERP_SPEED, TRANSITION_ROTATION_LERP_SPEED);
        return s;
    }

    private static void applyStandardPhysics(ServerCameraSettings cameraSettings,
                                             float positionLerpSpeed, float rotationLerpSpeed) {
        cameraSettings.positionLerpSpeed = positionLerpSpeed;
        cameraSettings.rotationLerpSpeed = rotationLerpSpeed;
        cameraSettings.positionDistanceOffsetType = PositionDistanceOffsetType.DistanceOffset;
        cameraSettings.attachedToType = AttachedToType.LocalPlayer;
        cameraSettings.positionType = PositionType.AttachedToPlusOffset;
        cameraSettings.canMoveType = CanMoveType.Always;
        cameraSettings.applyMovementType = ApplyMovementType.CharacterController;
    }

    public static float easeInOutCubic(float t) {
        if (t < 0.5f) return 4f * t * t * t;
        float f = -2f * t + 2f;
        return 1f - (f * f * f) / 2f;
    }

    public static float easeInOutQuad(float t) {
        return t < 0.5f ? 2f * t * t : 1f - (-2f * t + 2f) * (-2f * t + 2f) / 2f;
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
