package fr.arnaud.nexus.feature.combat;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class HeadTrackingSystem extends EntityTickingSystem<EntityStore> {

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
            Player.getComponentType(),
            HeadLockComponent.getComponentType(),
            HeadRotation.getComponentType(),
            TransformComponent.getComponentType()
        );
    }

    @Override
    public void tick(float dt, int index, ArchetypeChunk<EntityStore> chunk,
                     Store<EntityStore> store, CommandBuffer<EntityStore> cmd) {
        HeadLockComponent lock = chunk.getComponent(index, HeadLockComponent.getComponentType());
        if (!lock.isActive()) return;

        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        HeadRotation head = chunk.getComponent(index, HeadRotation.getComponentType());

        if (lock.getLockedEntityNetworkId() != -1) {
            Ref<EntityStore> targetRef = store.getExternalData()
                                              .getRefFromNetworkId(lock.getLockedEntityNetworkId());

            if (targetRef == null || !targetRef.isValid()) {
                lock.unlock();
                cmd.run(s -> s.putComponent(ref, HeadLockComponent.getComponentType(), lock));
                return;
            }

            TransformComponent playerTransform = chunk.getComponent(index, TransformComponent.getComponentType());
            TransformComponent targetTransform = store.getComponent(targetRef, TransformComponent.getComponentType());
            if (playerTransform == null || targetTransform == null) return;

            Vector3f rotation = computeAimRotation(
                playerTransform.getPosition(), targetTransform.getPosition());
            head.teleportRotation(rotation);

            double dx = targetTransform.getPosition().getX() - playerTransform.getPosition().getX();
            double dz = targetTransform.getPosition().getZ() - playerTransform.getPosition().getZ();

            lock.lockOnEntity(rotation, lock.getRemainingTimeSec(), lock.getLockedEntityNetworkId());
        } else {
            head.teleportRotation(lock.getTargetRotation());
        }

        if (!lock.tick(dt)) {
            // Timer expired — write the unock
            cmd.run(s -> {
                s.putComponent(ref, HeadRotation.getComponentType(), head);
                s.putComponent(ref, HeadLockComponent.getComponentType(), lock);
            });
            return;
        }

        cmd.run(s -> {
            s.putComponent(ref, HeadRotation.getComponentType(), head);
            s.putComponent(ref, HeadLockComponent.getComponentType(), lock);
        });
    }

    private static Vector3f computeAimRotation(Vector3d from, Vector3d to) {
        double dx = to.getX() - from.getX();
        double dy = (to.getY() + 1.6) - (from.getY() + 1.6);
        double dz = to.getZ() - from.getZ();

        float yaw = (float) Math.atan2(-dx, -dz);
        float pitch = (float) Math.atan2(dy, Math.sqrt(dx * dx + dz * dz));
        return new Vector3f(pitch, yaw, 0f);
    }
}
