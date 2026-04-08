package fr.arnaud.nexus.weapon.enchantment.handlers;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
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
        TransformComponent transform = cmd.getComponent(centerRef, TransformComponent.getComponentType());
        if (transform == null) return List.of();

        SpatialResource<Ref<EntityStore>, EntityStore> spatial =
            cmd.getResource(EntityModule.get().getPlayerSpatialResourceType());

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

    public static void applyRadialImpulse(
        Ref<EntityStore> sourceRef,
        Ref<EntityStore> targetRef,
        float force,
        CommandBuffer<EntityStore> cmd
    ) {
        TransformComponent sourceTransform = cmd.getComponent(sourceRef, TransformComponent.getComponentType());
        TransformComponent targetTransform = cmd.getComponent(targetRef, TransformComponent.getComponentType());
        Velocity targetVelocity = cmd.getComponent(targetRef, Velocity.getComponentType());
        if (sourceTransform == null || targetTransform == null || targetVelocity == null) return;

        Vector3d direction = targetTransform.getPosition().clone();
        direction.subtract(sourceTransform.getPosition());
        double length = direction.length();
        if (length < 0.001) return;
        direction.setLength(force);

        targetVelocity.addForce(direction);
    }

    public static void applyDirectionalImpulse(
        Ref<EntityStore> targetRef,
        Ref<EntityStore> towardRef,
        float force,
        CommandBuffer<EntityStore> cmd
    ) {
        TransformComponent targetTransform = cmd.getComponent(targetRef, TransformComponent.getComponentType());
        TransformComponent towardTransform = cmd.getComponent(towardRef, TransformComponent.getComponentType());
        Velocity targetVelocity = cmd.getComponent(targetRef, Velocity.getComponentType());
        if (targetTransform == null || towardTransform == null || targetVelocity == null) return;

        Vector3d direction = towardTransform.getPosition().clone();
        direction.subtract(targetTransform.getPosition());
        double length = direction.length();
        if (length < 0.001) return;
        direction.setLength(force);

        targetVelocity.addForce(direction);
    }

    public static boolean isSameRef(Ref<EntityStore> a, Ref<EntityStore> b) {
        return a.getIndex() == b.getIndex();
    }
}
