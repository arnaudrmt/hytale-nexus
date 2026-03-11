package fr.arnaud.nexus.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;

/**
 * Owns the Switch Strike activation window state machine.
 * <p>
 * The window opens when the player's Razorstrike ability lands a hit.
 * The player must swap weapons within the window duration to confirm the
 * Switch Strike. A confirmed Strike is flagged via {@link #consume()} and
 * downstream systems react via {@link fr.arnaud.nexus.event.SwitchStrikeActivatedEvent}.
 */
public final class SwitchStrikeComponent implements Component<EntityStore> {

    public static final float WINDOW_DURATION_SEC = 1.0f;
    public static final float WINDOW_DURATION_HARD_SEC = 0.5f;

    @Nullable
    private static ComponentType<EntityStore, SwitchStrikeComponent> componentType;

    private boolean windowOpen;
    private float windowRemainingSec;
    private boolean consumed;

    public SwitchStrikeComponent() {
    }

    public void openWindow(float durationSec) {
        windowOpen = true;
        windowRemainingSec = durationSec;
        consumed = false;
    }

    /**
     * Ticks the window timer down. Call once per frame from the owning system.
     */
    public void tick(float deltaSec) {
        if (!windowOpen) return;
        windowRemainingSec -= deltaSec;
        if (windowRemainingSec <= 0f) {
            windowOpen = false;
            consumed = false;
        }
    }

    /**
     * Confirms and closes the Switch Strike window. Returns false if the window
     * was not open or was already consumed.
     */
    public boolean consume() {
        if (!windowOpen || consumed) return false;
        consumed = true;
        windowOpen = false;
        return true;
    }

    public boolean isWindowOpen() {
        return windowOpen;
    }

    // --- ECS Boilerplate ---

    @NonNullDecl
    public static ComponentType<EntityStore, SwitchStrikeComponent> getComponentType() {
        if (componentType == null) throw new IllegalStateException("SwitchStrikeComponent not registered.");
        return componentType;
    }

    public static void setComponentType(@Nullable ComponentType<EntityStore, SwitchStrikeComponent> type) {
        componentType = type;
    }

    @Override
    @NonNullDecl
    public SwitchStrikeComponent clone() {
        SwitchStrikeComponent c = new SwitchStrikeComponent();
        c.windowOpen = this.windowOpen;
        c.windowRemainingSec = this.windowRemainingSec;
        c.consumed = this.consumed;
        return c;
    }
}
