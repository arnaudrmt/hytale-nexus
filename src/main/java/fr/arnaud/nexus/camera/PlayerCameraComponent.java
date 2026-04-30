package fr.arnaud.nexus.camera;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;

public final class PlayerCameraComponent implements Component<EntityStore> {

    private boolean clientReady = false;

    @Nullable
    private static ComponentType<EntityStore, PlayerCameraComponent> componentType;

    public static final float ENTRY_TRANSITION_DURATION_SEC = 0.35f;
    public static final float EXIT_TRANSITION_DURATION_SEC = 0.50f;
    public static final float ISO_DISTANCE = 12.0f;
    public static final float ENCOUNTER_ZOOM_BONUS = 3.0f;

    private CameraMode mode = CameraMode.ISO_RUN;
    private float entryElapsedSec = 0f;
    private float exitElapsedSec = 0f;
    private float speedFovBonus = 0f;
    private boolean encounterZoomOut = false;

    private boolean packetDirty = true;

    public PlayerCameraComponent() {
    }

    private PlayerCameraComponent(CameraMode mode,
                                  float entryElapsedSec, float exitElapsedSec,
                                  boolean packetDirty,
                                  float speedFovBonus, boolean encounterZoomOut,
                                  boolean clientReady) {
        this.mode = mode;
        this.entryElapsedSec = entryElapsedSec;
        this.exitElapsedSec = exitElapsedSec;
        this.packetDirty = packetDirty;
        this.speedFovBonus = speedFovBonus;
        this.encounterZoomOut = encounterZoomOut;
        this.clientReady = clientReady;
    }

    public boolean isClientReady() {
        return clientReady;
    }

    public void markClientReady() {
        clientReady = true;
        packetDirty = true;
    }

    public boolean beginGlimpseEntry() {
        if (mode != CameraMode.ISO_RUN) return false;
        mode = CameraMode.GLIMPSE_TRANSITION;
        entryElapsedSec = 0f;
        packetDirty = true;
        return true;
    }

    public void completeGlimpseEntry() {
        mode = CameraMode.GLIMPSE_ACTIVE;
        packetDirty = true;
    }

    public boolean beginGlimpseExit() {
        if (mode != CameraMode.GLIMPSE_ACTIVE) return false;
        mode = CameraMode.GLIMPSE_EXIT_TRANSITION;
        exitElapsedSec = 0f;
        packetDirty = true;
        return true;
    }

    public void completeGlimpseExit() {
        mode = CameraMode.ISO_RUN;
        entryElapsedSec = 0f;
        exitElapsedSec = 0f;
        packetDirty = true;
    }

    public void setSpeedFovBonus(float normalised) {
        float clamped = Math.max(0f, Math.min(1f, normalised));
        if (Float.compare(speedFovBonus, clamped) != 0) {
            speedFovBonus = clamped;
            if (mode == CameraMode.ISO_RUN) packetDirty = true;
        }
    }

    public void setEncounterZoomOut(boolean active) {
        if (encounterZoomOut != active) {
            encounterZoomOut = active;
            if (mode == CameraMode.ISO_RUN) packetDirty = true;
        }
    }

    public CameraMode getMode() {
        return mode;
    }

    public float getSpeedFovBonus() {
        return speedFovBonus;
    }

    public boolean isEncounterZoomOut() {
        return encounterZoomOut;
    }

    public boolean isPacketDirty() {
        return packetDirty;
    }

    public void clearPacketDirty() {
        packetDirty = false;
    }

    public float advanceEntry(float deltaSec) {
        entryElapsedSec += deltaSec;
        return entryElapsedSec;
    }

    public float advanceExit(float deltaSec) {
        exitElapsedSec += deltaSec;
        return exitElapsedSec;
    }

    public float getEntryProgress() {
        return Math.min(1f, entryElapsedSec / ENTRY_TRANSITION_DURATION_SEC);
    }

    public float getExitProgress() {
        return Math.min(1f, exitElapsedSec / EXIT_TRANSITION_DURATION_SEC);
    }

    public float getEffectiveIsoDistance() {
        return ISO_DISTANCE + (encounterZoomOut ? ENCOUNTER_ZOOM_BONUS : 0f);
    }

    @NonNullDecl
    public static ComponentType<EntityStore, PlayerCameraComponent> getComponentType() {
        if (componentType == null) throw new IllegalStateException("CameraComponent not yet registered.");
        return componentType;
    }

    public static void setComponentType(@Nullable ComponentType<EntityStore, PlayerCameraComponent> type) {
        componentType = type;
    }

    @NonNullDecl
    @Override
    public PlayerCameraComponent clone() {
        return new PlayerCameraComponent(mode, entryElapsedSec, exitElapsedSec,
            packetDirty, speedFovBonus, encounterZoomOut, clientReady);
    }

}
