package fr.arnaud.nexus.ability.impl;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.ability.AbstractCoreAbilitySystem;
import fr.arnaud.nexus.ability.ActiveCoreComponent;
import fr.arnaud.nexus.ability.CoreAbility;
import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.feature.combat.PlayerBodyStateComponent;
import fr.arnaud.nexus.feature.movement.PlayerDashComponent;
import fr.arnaud.nexus.feature.resource.PlayerStatsManager;
import fr.arnaud.nexus.input.PlayerCursorTargetComponent;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;
import java.util.Objects;

public final class DashAbility extends AbstractCoreAbilitySystem {

    @NonNullDecl
    @Override
    public CoreAbility getAbility() {
        return CoreAbility.DASH;
    }

    @NonNullDecl
    @Override
    protected Query<EntityStore> buildQuery() {
        return Query.and(
            super.buildQuery(),
            PlayerDashComponent.getComponentType()
        );
    }

    /**
     * Dash state is advanced by PlayerLocomotionSystem — no per-tick logic needed here.
     */
    @Override
    public void tickCore(float deltaSeconds, int index, ArchetypeChunk<EntityStore> chunk,
                         Store<EntityStore> store, CommandBuffer<EntityStore> cmd,
                         Ref<EntityStore> ref, ActiveCoreComponent activeCore) {
    }

    public void tryActivate(@NonNullDecl Player player,
                            @NonNullDecl Ref<EntityStore> ref,
                            @NonNullDecl Store<EntityStore> store) {
        PlayerDashComponent dash = store.getComponent(ref, PlayerDashComponent.getComponentType());
        if (dash == null || !dash.isIdle()) return;

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return;

        PlayerStatsManager psm = Nexus.get().getPlayerStatsManager();
        if (psm.getStamina(ref, store) < 1) return;

        float[] dir = resolveDirection(ref, store, transform);
        dash.beginDash(dir[0], dir[1]);

        psm.removeStamina(ref, store, 1);

        store.putComponent(ref, PlayerDashComponent.getComponentType(), dash);
    }

    private static float[] resolveDirection(@NonNullDecl Ref<EntityStore> ref,
                                            @NonNullDecl Store<EntityStore> store,
                                            @NonNullDecl TransformComponent transform) {
        float px = (float) transform.getPosition().getX();
        float pz = (float) transform.getPosition().getZ();

        PlayerCursorTargetComponent cursor =
            store.getComponent(ref, PlayerCursorTargetComponent.getComponentType());

        if (cursor != null && cursor.hasEntityTarget()) {
            float[] dir = directionToEntity(cursor, store, px, pz);
            if (dir != null) return dir;
        }

        if (cursor != null && cursor.hasBlockTarget()) {
            float[] dir = directionToBlock(
                Objects.requireNonNull(cursor.getTargetBlock()), px, pz);
            if (dir != null) return dir;
        }

        return directionFromYaw(ref, store);
    }

    @Nullable
    private static float[] directionToEntity(@NonNullDecl PlayerCursorTargetComponent cursor,
                                             @NonNullDecl Store<EntityStore> store,
                                             float px, float pz) {
        var entityRef = Objects.requireNonNull(cursor.getTargetEntity()).getReference();
        TransformComponent entityTransform =
            store.getComponent(Objects.requireNonNull(entityRef), TransformComponent.getComponentType());
        if (entityTransform == null) return null;
        return normalizeXZ(
            (float) entityTransform.getPosition().getX() - px,
            (float) entityTransform.getPosition().getZ() - pz
        );
    }

    @Nullable
    private static float[] directionToBlock(@NonNullDecl Vector3i target, float px, float pz) {
        return normalizeXZ(target.getX() + 0.5f - px, target.getZ() + 0.5f - pz);
    }

    @NonNullDecl
    private static float[] directionFromYaw(@NonNullDecl Ref<EntityStore> ref,
                                            @NonNullDecl Store<EntityStore> store) {
        PlayerBodyStateComponent body =
            store.getComponent(ref, PlayerBodyStateComponent.getComponentType());
        if (body != null) {
            float yaw = body.getAimYaw();
            return new float[]{-(float) Math.sin(yaw), (float) Math.cos(yaw)};
        }
        return new float[]{0f, 1f};
    }

    @Nullable
    private static float[] normalizeXZ(float dx, float dz) {
        float len = (float) Math.sqrt(dx * dx + dz * dz);
        return len > 0.01f ? new float[]{dx / len, dz / len} : null;
    }
}
