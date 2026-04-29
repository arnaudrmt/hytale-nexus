package fr.arnaud.nexus.util;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.server.core.entity.knockback.KnockbackComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class KnockbackUtil {

    public static void applyRadialKnockbackImpulse(
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

        Vector3d direction = targetTransform.getPosition().clone();
        direction.subtract(sourceTransform.getPosition());
        double length = direction.length();
        if (length < 0.001) {
            direction.assign(0, 1, 0);
        } else {
            direction.setLength(force);
        }

        KnockbackComponent knockback = new KnockbackComponent();
        knockback.setVelocity(direction);
        knockback.setVelocityType(ChangeVelocityType.Add);
        knockback.setDuration(0.05f);
        cmd.putComponent(targetRef, KnockbackComponent.getComponentType(), knockback);
    }

    public static void applyDirectionalKnockbackImpulse(
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
}
