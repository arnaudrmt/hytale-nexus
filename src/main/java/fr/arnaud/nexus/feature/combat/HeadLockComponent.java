package fr.arnaud.nexus.feature.combat;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;

public final class HeadLockComponent implements Component<EntityStore> {

    @Nullable
    private static ComponentType<EntityStore, HeadLockComponent> componentType;

    private final Vector3f targetRotation = new Vector3f();
    private int lockedEntityNetworkId = -1;
    private float remainingSec = 0f;
    private boolean active = false;

    public HeadLockComponent() {
    }

    public void lockOnEntity(Vector3f rotation, float durationSec, int networkId) {
        targetRotation.assign(rotation);
        remainingSec = durationSec;
        active = true;
        lockedEntityNetworkId = networkId;
    }

    public void unlock() {
        active = false;
        remainingSec = 0f;
        lockedEntityNetworkId = -1;
    }

    public boolean tick(float dt) {
        if (!active) return false;
        remainingSec -= dt;
        if (remainingSec <= 0f) {
            active = false;
            return false;
        }
        return true;
    }

    public boolean isActive() {
        return active;
    }

    public int getLockedEntityNetworkId() {
        return lockedEntityNetworkId;
    }

    public float getRemainingTimeSec() {
        return remainingSec;
    }

    @NonNullDecl
    public Vector3f getTargetRotation() {
        return targetRotation;
    }

    @NonNullDecl
    public static ComponentType<EntityStore, HeadLockComponent> getComponentType() {
        if (componentType == null) throw new IllegalStateException("HeadLockComponent not registered.");
        return componentType;
    }

    public static void setComponentType(@Nullable ComponentType<EntityStore, HeadLockComponent> type) {
        componentType = type;
    }

    @Override
    @NonNullDecl
    public HeadLockComponent clone() {
        HeadLockComponent c = new HeadLockComponent();
        c.targetRotation.assign(this.targetRotation);
        c.remainingSec = this.remainingSec;
        c.active = this.active;
        c.lockedEntityNetworkId = this.lockedEntityNetworkId;
        return c;
    }
}
