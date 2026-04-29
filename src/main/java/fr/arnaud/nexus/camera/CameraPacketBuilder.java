package fr.arnaud.nexus.camera;

import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.protocol.packets.camera.SetServerCamera;

/**
 * Handles the state transitions between ISO and First-Person views.
 */
public final class CameraPacketBuilder {

    public static final float ISO_YAW_RAD = (float) Math.toRadians(225.0);
    private static final float ISO_PITCH_RAD = (float) Math.toRadians(-45.0);

    private static final float ISO_POS_LERP = 0.1f;
    private static final float TRANSITION_POS_LERP = 0.75f;
    private static final float TRANSITION_ROT_LERP = 0.85f;

    private CameraPacketBuilder() {
    }

    public static SetServerCamera buildIso(PlayerCameraComponent cam) {
        ServerCameraSettings s = new ServerCameraSettings();

        s.isFirstPerson = false;
        s.distance = cam.getEffectiveIsoDistance();
        s.rotationType = RotationType.Custom;
        s.rotation = new Direction(ISO_YAW_RAD, ISO_PITCH_RAD, 0f);
        s.movementForceRotation = new Direction(ISO_YAW_RAD, 0f, 0f);

        s.applyLookType = ApplyLookType.Rotation;

        s.mouseInputTargetType = MouseInputTargetType.Any;
        s.mouseInputType = MouseInputType.LookAtPlane;
        s.planeNormal = new Vector3f(0f, 1f, 0f);
        s.sendMouseMotion = true;
        s.displayReticle = false;
        s.displayCursor = true;

        applyStandardPhysics(s, ISO_POS_LERP, 1.0f);
        return new SetServerCamera(ClientCameraView.Custom, true, s);
    }

    public static SetServerCamera buildGlimpseActive() {
        return new SetServerCamera(ClientCameraView.FirstPerson, true, null);
    }

    public static SetServerCamera buildEntryTransition(float t, float isoDistance) {
        float eased = easeInOutCubic(t);

        ServerCameraSettings s = new ServerCameraSettings();
        s.distance = lerp(isoDistance, 0f, eased);
        s.isFirstPerson = false;
        s.eyeOffset = false;

        s.rotationType = RotationType.Custom;
        s.rotation = new Direction(ISO_YAW_RAD, lerp(ISO_PITCH_RAD, 0f, eased), 0f);
        s.applyLookType = ApplyLookType.Rotation;

        s.mouseInputTargetType = MouseInputTargetType.None;
        s.mouseInputType = MouseInputType.LookAtTarget;
        s.sendMouseMotion = true;
        s.displayReticle = false;

        applyStandardPhysics(s, TRANSITION_POS_LERP, TRANSITION_ROT_LERP);
        return new SetServerCamera(ClientCameraView.Custom, true, s);
    }

    public static SetServerCamera buildExitTransition(float t, float isoDistance) {
        float eased = easeInOutQuad(t);

        ServerCameraSettings s = new ServerCameraSettings();
        s.distance = lerp(0f, isoDistance, eased);
        s.isFirstPerson = false;
        s.eyeOffset = false;

        s.rotationType = RotationType.Custom;
        s.rotation = new Direction(ISO_YAW_RAD, lerp(0f, ISO_PITCH_RAD, eased), 0f);
        s.applyLookType = ApplyLookType.Rotation;

        s.mouseInputTargetType = MouseInputTargetType.None;
        s.mouseInputType = MouseInputType.LookAtTarget;
        s.sendMouseMotion = true;
        s.displayReticle = false;
        s.displayCursor = true;

        applyStandardPhysics(s, TRANSITION_POS_LERP, TRANSITION_ROT_LERP);
        return new SetServerCamera(ClientCameraView.Custom, true, s);
    }

    public static SetServerCamera buildReset() {
        return new SetServerCamera(ClientCameraView.Custom, false, null);
    }

    private static void applyStandardPhysics(ServerCameraSettings s, float posLerp, float rotLerp) {
        s.positionLerpSpeed = posLerp;
        s.rotationLerpSpeed = rotLerp;
        s.positionDistanceOffsetType = PositionDistanceOffsetType.DistanceOffset;
        s.attachedToType = AttachedToType.LocalPlayer;
        s.positionType = PositionType.AttachedToPlusOffset;
        s.canMoveType = CanMoveType.Always;
        s.applyMovementType = ApplyMovementType.CharacterController;
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
