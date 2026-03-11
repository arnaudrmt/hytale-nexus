package fr.arnaud.nexus.camera;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;

/**
 * ECS component storing per-player camera state.
 * <p>
 * Manages the transitions between the default Isometric view (ISO)
 * and the First-person view (referred to internally as "Glimpse").
 * Networking updates are driven by the {@code packetDirty} flag.
 */
public final class CameraComponent implements Component<EntityStore> {

    @Nullable
    private static ComponentType<EntityStore, CameraComponent> componentType;

    @NonNullDecl
    public static ComponentType<EntityStore, CameraComponent> getComponentType() {
        if (componentType == null) throw new IllegalStateException("CameraComponent not yet registered.");
        return componentType;
    }

    public static void setComponentType(@Nullable ComponentType<EntityStore, CameraComponent> type) {
        componentType = type;
    }

    /**
     * Duration (in seconds) to zoom into First-Person view.
     */
    public static final float ENTRY_TRANSITION_DURATION_SEC = 0.35f;

    /**
     * Duration (in seconds) to pull back out to Isometric view. Slower to prevent motion sickness.
     */
    public static final float EXIT_TRANSITION_DURATION_SEC = 0.50f;

    public static final float ISO_DISTANCE = 12.0f;

    /**
     * Extra camera distance added when fighting a group of enemies to give players a wider field of view.
     */
    public static final float ENCOUNTER_ZOOM_BONUS = 3.0f;

    /**
     * The number of nearby enemies required to trigger the encounter zoom.
     */
    public static final int ENCOUNTER_ENEMY_THRESHOLD = 4;

    private CameraMode mode = CameraMode.ISO_RUN;
    private float entryElapsedSec = 0f;
    private float exitElapsedSec = 0f;

    /**
     * True if state has changed and needs to be synced to the client this tick.
     */
    private boolean packetDirty = true;

    private float speedFovBonus = 0f;
    private boolean encounterZoomOut = false;

    public CameraComponent() {
    }

    private CameraComponent(CameraMode mode,
                            float entryElapsedSec, float exitElapsedSec,
                            boolean packetDirty,
                            float speedFovBonus, boolean encounterZoomOut) {
        this.mode = mode;
        this.entryElapsedSec = entryElapsedSec;
        this.exitElapsedSec = exitElapsedSec;
        this.packetDirty = packetDirty;
        this.speedFovBonus = speedFovBonus;
        this.encounterZoomOut = encounterZoomOut;
    }

    /**
     * Initiates the transition from ISO to Glimpse (First-person) view.
     *
     * @return true if the transition successfully started, false if the camera wasn't in ISO mode.
     */
    public boolean beginGlimpseEntry() {
        if (mode != CameraMode.ISO_RUN) return false;
        mode = CameraMode.GLIMPSE_TRANSITION;
        entryElapsedSec = 0f;
        packetDirty = true;
        return true;
    }

    /**
     * Finalizes the entry transition.
     * Must be called by the CameraSystem once the entry animation completes.
     */
    public void completeGlimpseEntry() {
        mode = CameraMode.GLIMPSE_ACTIVE;
        packetDirty = true;
    }

    /**
     * Initiates the transition from Glimpse (First-person) back to ISO view.
     *
     * @return true if the transition successfully started, false if not currently in Glimpse mode.
     */
    public boolean beginGlimpseExit() {
        if (mode != CameraMode.GLIMPSE_ACTIVE) return false;
        mode = CameraMode.GLIMPSE_EXIT_TRANSITION;
        exitElapsedSec = 0f;
        packetDirty = true;
        return true;
    }

    /**
     * Finalizes the exit transition.
     * Must be called by the CameraSystem once the exit animation completes.
     */
    public void completeGlimpseExit() {
        mode = CameraMode.ISO_RUN;
        entryElapsedSec = 0f;
        exitElapsedSec = 0f;
        packetDirty = true;
    }

    /**
     * Adjusts the FOV based on player speed.
     *
     * @param normalised A value between 0.0 and 1.0 representing player speed.
     */
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
    @Override
    public CameraComponent clone() {
        return new CameraComponent(mode, entryElapsedSec, exitElapsedSec,
            packetDirty, speedFovBonus, encounterZoomOut);
    }
}
