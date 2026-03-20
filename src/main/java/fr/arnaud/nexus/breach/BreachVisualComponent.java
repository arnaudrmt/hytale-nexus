package fr.arnaud.nexus.breach;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;

/**
 * Tracks the visual transition state for the Breach sequence.
 * Drives sky time-of-day lerp and time dilation in lockstep with the camera.
 * Ephemeral — state is meaningless across sessions.
 */
public final class BreachVisualComponent implements Component<EntityStore> {

    public enum Phase {IDLE, FADE_IN, HELD, FADE_OUT}

    public static final float FADE_DURATION_SECONDS = 1.6f;
    public static final float TIME_DILATION_NORMAL = 1.0f;
    public static final float TIME_DILATION_BREACH = 0.3f;

    @Nullable
    private static ComponentType<EntityStore, BreachVisualComponent> componentType;

    private Phase phase = Phase.IDLE;
    private float progress = 0f;

    public BreachVisualComponent() {
    }

    public void beginFadeIn() {
        phase = Phase.FADE_IN;
        progress = 0f;
    }

    public void beginFadeOut() {
        phase = Phase.FADE_OUT;
        progress = 1f;
    }

    /**
     * Advances the transition. Returns the new [0,1] blend factor.
     * FADE_IN → HELD at progress 1; FADE_OUT → IDLE at progress 0.
     */
    public float tick(float deltaSeconds) {
        float step = deltaSeconds / FADE_DURATION_SECONDS;
        switch (phase) {
            case FADE_IN -> {
                progress = Math.min(1f, progress + step);
                if (progress >= 1f) phase = Phase.HELD;
            }
            case FADE_OUT -> {
                progress = Math.max(0f, progress - step);
                if (progress <= 0f) phase = Phase.IDLE;
            }
            default -> {
            }
        }
        return progress;
    }

    public Phase getPhase() {
        return phase;
    }

    public float getProgress() {
        return progress;
    }

    @NonNullDecl
    public static ComponentType<EntityStore, BreachVisualComponent> getComponentType() {
        if (componentType == null) throw new IllegalStateException("BreachVisualComponent not registered.");
        return componentType;
    }

    public static void setComponentType(
        @NonNullDecl ComponentType<EntityStore, BreachVisualComponent> type) {
        componentType = type;
    }

    @Override
    @NonNullDecl
    public BreachVisualComponent clone() {
        BreachVisualComponent c = new BreachVisualComponent();
        c.phase = this.phase;
        c.progress = this.progress;
        return c;
    }
}
