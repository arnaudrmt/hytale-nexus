package fr.arnaud.nexus.camera;

/**
 * Represents the current camera state for the player.
 * <p>
 * Defines the state machine transitions between the default Isometric view
 * and the specialized First-person "Glimpse" view.
 */
public enum CameraMode {

    /**
     * Default 3PP isometric view.
     */
    ISO_RUN,

    /**
     * Transitioning into First-person view.
     */
    GLIMPSE_TRANSITION,

    /**
     * Currently in First-person view.
     */
    GLIMPSE_ACTIVE,

    /**
     * Transitioning back to Isometric view.
     */
    GLIMPSE_EXIT_TRANSITION
}
