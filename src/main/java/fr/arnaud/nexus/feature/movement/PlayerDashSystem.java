package fr.arnaud.nexus.feature.movement;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.feature.combat.PlayerBodyStateComponent;
import fr.arnaud.nexus.input.PlayerCursorTargetComponent;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.Objects;

public final class PlayerDashSystem {

    private static final float STAMINA_COST = 0.4f;

    public PlayerDashSystem() {
    }

    /**
     * Attempts to begin a dash for the given player entity.
     * Guards: player must be idle and have enough flow. Direction is resolved
     * from cursor target with yaw fallback.
     */
    public void tryDash(@NonNullDecl Player player,
                        @NonNullDecl Ref<EntityStore> ref,
                        @NonNullDecl Store<EntityStore> store) {
        PlayerDashComponent dash = store.getComponent(ref, PlayerDashComponent.getComponentType());
        if (dash == null || !dash.isIdle()) return;

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return;

        float[] dir = resolveDirection(ref, store, transform);
        dash.beginDash(dir[0], dir[1]);
        // TODO: Remove Stamina

        store.putComponent(ref, PlayerDashComponent.getComponentType(), dash);
    }

    // --- Direction resolution ---

    private static float[] resolveDirection(@NonNullDecl Ref<EntityStore> ref,
                                            @NonNullDecl Store<EntityStore> store,
                                            @NonNullDecl TransformComponent transform) {
        float px = (float) transform.getPosition().getX();
        float pz = (float) transform.getPosition().getZ();

        PlayerCursorTargetComponent cursor = store.getComponent(ref, PlayerCursorTargetComponent.getComponentType());

        if (cursor != null && cursor.hasEntityTarget()) {
            float[] dir = directionToEntity(cursor, store, px, pz);
            if (dir != null) return dir;
        }

        if (cursor != null && cursor.hasBlockTarget()) {
            float[] dir = directionToBlock(Objects.requireNonNull(cursor.getTargetBlock()), px, pz);
            if (dir != null) return dir;
        }

        return directionFromYaw(ref, store);
    }

    private static float[] directionToEntity(@NonNullDecl PlayerCursorTargetComponent cursor,
                                             @NonNullDecl Store<EntityStore> store,
                                             float px, float pz) {
        var entityRef = Objects.requireNonNull(cursor.getTargetEntity()).getReference();
        TransformComponent entityTransform = store.getComponent(Objects.requireNonNull(entityRef), TransformComponent.getComponentType());
        if (entityTransform == null) return null;
        return normalizeXZ(
            (float) entityTransform.getPosition().getX() - px,
            (float) entityTransform.getPosition().getZ() - pz
        );
    }

    private static float[] directionToBlock(@NonNullDecl Vector3i target, float px, float pz) {
        return normalizeXZ(target.getX() + 0.5f - px, target.getZ() + 0.5f - pz);
    }

    private static float[] directionFromYaw(@NonNullDecl Ref<EntityStore> ref,
                                            @NonNullDecl Store<EntityStore> store) {
        PlayerBodyStateComponent body = store.getComponent(ref, PlayerBodyStateComponent.getComponentType());
        if (body != null) {
            float yaw = body.getAimYaw();
            return new float[]{-(float) Math.sin(yaw), (float) Math.cos(yaw)};
        }
        return new float[]{0f, 1f};
    }

    private static float[] normalizeXZ(float dx, float dz) {
        float len = (float) Math.sqrt(dx * dx + dz * dz);
        return len > 0.01f ? new float[]{dx / len, dz / len} : null;
    }
}
