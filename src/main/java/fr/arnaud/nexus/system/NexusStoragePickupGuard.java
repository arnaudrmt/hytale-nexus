package fr.arnaud.nexus.system;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.OrderPriority;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.component.spatial.SpatialStructure;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.entity.item.PickupItemComponent;
import com.hypixel.hytale.server.core.modules.entity.item.PreventPickup;
import com.hypixel.hytale.server.core.modules.entity.system.PlayerSpatialSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.util.InventoryUtils;

import javax.annotation.Nonnull;
import java.util.Set;

/**
 * Runs before PlayerItemEntityPickupSystem.
 * Adds PreventPickup to item entities when the nearest player's
 * storage is full (27 slots), removes it when space is available again.
 */
public final class NexusStoragePickupGuard extends EntityTickingSystem<EntityStore> {

    @Nonnull
    private final ResourceType<EntityStore, SpatialResource<Ref<EntityStore>, EntityStore>>
        playerSpatialResource;

    @Nonnull
    private final Set<Dependency<EntityStore>> dependencies = Set.of(
        new SystemDependency(Order.AFTER, PlayerSpatialSystem.class, OrderPriority.CLOSEST)
    );

    @Nonnull
    private final Query<EntityStore> query;

    public NexusStoragePickupGuard(
        @Nonnull ResourceType<EntityStore, SpatialResource<Ref<EntityStore>, EntityStore>>
            playerSpatialResource) {

        this.playerSpatialResource = playerSpatialResource;
        this.query = Query.and(
            ItemComponent.getComponentType(),
            TransformComponent.getComponentType(),
            Query.not(PickupItemComponent.getComponentType())
        );
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return dependencies;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Override
    public boolean isParallel(int archetypeChunkSize, int taskCount) {
        return false;
    }

    @Override
    public void tick(float dt, int index,
                     @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                     @Nonnull Store<EntityStore> store,
                     @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        Ref<EntityStore> itemRef = archetypeChunk.getReferenceTo(index);
        TransformComponent transform = archetypeChunk.getComponent(
            index, TransformComponent.getComponentType());
        if (transform == null) {
            return;
        }

        Vector3d itemPos = transform.getPosition();
        SpatialResource<Ref<EntityStore>, EntityStore> spatial =
            store.getResource(playerSpatialResource);
        SpatialStructure<Ref<EntityStore>> structure = spatial.getSpatialStructure();

        Ref<EntityStore> nearestPlayer = structure.closest(itemPos);
        if (nearestPlayer == null) {
            return;
        }

        int usedSlots = InventoryUtils.getStorageUsedSlots(nearestPlayer, store);
        boolean storageFull = usedSlots >= InventoryUtils.MAX_STORAGE_SLOTS;
        boolean hasPreventPickup = store.getComponent(
            itemRef, PreventPickup.getComponentType()) != null;

        if (storageFull && !hasPreventPickup) {
            commandBuffer.run(s -> s.putComponent(
                itemRef, PreventPickup.getComponentType(), PreventPickup.INSTANCE));
        } else if (!storageFull && hasPreventPickup) {
            commandBuffer.run(s -> s.removeComponent(
                itemRef, PreventPickup.getComponentType()));
        }
    }
}
