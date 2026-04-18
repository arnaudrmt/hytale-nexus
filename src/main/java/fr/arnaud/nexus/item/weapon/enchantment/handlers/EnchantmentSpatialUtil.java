package fr.arnaud.nexus.item.weapon.enchantment.handlers;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.server.core.entity.knockback.KnockbackComponent;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.List;

public final class EnchantmentSpatialUtil {

    private EnchantmentSpatialUtil() {
    }

    public static List<Ref<EntityStore>> getEntitiesInRadius(
        Ref<EntityStore> centerRef,
        float radius,
        CommandBuffer<EntityStore> cmd
    ) {
        TransformComponent transform = cmd.getComponent(
            centerRef, TransformComponent.getComponentType());
        if (transform == null) return List.of();

        SpatialResource<Ref<EntityStore>, EntityStore> spatial =
            cmd.getResource(EntityModule.get().getEntitySpatialResourceType());

        List<Ref<EntityStore>> results = new ArrayList<>();
        spatial.getSpatialStructure().collect(transform.getPosition(), radius, results);
        return results;
    }

    public static double distanceBetween(
        Ref<EntityStore> a,
        Ref<EntityStore> b,
        CommandBuffer<EntityStore> cmd
    ) {
        TransformComponent ta = cmd.getComponent(a, TransformComponent.getComponentType());
        TransformComponent tb = cmd.getComponent(b, TransformComponent.getComponentType());
        if (ta == null || tb == null) return Double.MAX_VALUE;
        return ta.getPosition().distanceTo(tb.getPosition());
    }

    /**
     * Applies a radial knockback impulse from source to target using KnockbackComponent.
     * This properly syncs to the client via the engine's knockback system.
     */
    public static void applyRadialImpulse(
        Ref<EntityStore> sourceRef,
        Ref<EntityStore> targetRef,
        float force,
        CommandBuffer<EntityStore> cmd
    ) {
        TransformComponent sourceTransform = cmd.getComponent(
            sourceRef, TransformComponent.getComponentType());
        TransformComponent targetTransform = cmd.getComponent(
            targetRef, TransformComponent.getComponentType());
        if (sourceTransform == null || targetTransform == null) return;

        // Compute direction from source to target
        Vector3d direction = targetTransform.getPosition().clone();
        direction.subtract(sourceTransform.getPosition());
        double length = direction.length();
        if (length < 0.001) {
            // Entities at same position — push straight up
            direction.assign(0, 1, 0);
        } else {
            direction.setLength(force);
        }

        // Use KnockbackComponent so the impulse is synced to the client
        KnockbackComponent knockback = new KnockbackComponent();
        knockback.setVelocity(direction);
        knockback.setVelocityType(ChangeVelocityType.Add);
        knockback.setDuration(0.05f);
        cmd.putComponent(targetRef, KnockbackComponent.getComponentType(), knockback);
    }

    public static void applyDirectionalImpulse(
        Ref<EntityStore> targetRef,
        Ref<EntityStore> towardRef,
        float force,
        CommandBuffer<EntityStore> cmd
    ) {
        TransformComponent targetTransform = cmd.getComponent(
            targetRef, TransformComponent.getComponentType());
        TransformComponent towardTransform = cmd.getComponent(
            towardRef, TransformComponent.getComponentType());
        if (targetTransform == null || towardTransform == null) return;

        Vector3d direction = towardTransform.getPosition().clone();
        direction.subtract(targetTransform.getPosition());
        double length = direction.length();
        if (length < 0.001) return;
        direction.setLength(force);

        KnockbackComponent knockback = new KnockbackComponent();
        knockback.setVelocity(direction);
        knockback.setVelocityType(ChangeVelocityType.Add);
        knockback.setDuration(0.05f);
        cmd.putComponent(targetRef, KnockbackComponent.getComponentType(), knockback);
    }

    public static boolean isSameRef(Ref<EntityStore> a, Ref<EntityStore> b) {
        return a.getIndex() == b.getIndex();
    }
}
