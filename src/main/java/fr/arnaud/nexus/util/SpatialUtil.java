package fr.arnaud.nexus.util;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.List;

public class SpatialUtil {

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

    public static double calculateDistanceBetweenTwoPoints(
        Ref<EntityStore> a,
        Ref<EntityStore> b,
        CommandBuffer<EntityStore> cmd
    ) {
        TransformComponent ta = cmd.getComponent(a, TransformComponent.getComponentType());
        TransformComponent tb = cmd.getComponent(b, TransformComponent.getComponentType());
        if (ta == null || tb == null) return Double.MAX_VALUE;
        return ta.getPosition().distanceTo(tb.getPosition());
    }
}
