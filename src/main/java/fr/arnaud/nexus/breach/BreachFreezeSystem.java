package fr.arnaud.nexus.breach;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

/**
 * Enforces physical stillness on any NPC carrying {@link FrozenComponent}.
 * <p>
 * NPCs do not use the ECS {@code Velocity} component for locomotion — their
 * movement is computed entirely inside {@link com.hypixel.hytale.server.npc.role.Role}
 * by the active {@link com.hypixel.hytale.server.npc.movement.controllers.MotionController}.
 * Zeroing the ECS Velocity component has no effect on NPC movement.
 * <p>
 * The correct override is {@code role.forceVelocity(Vector3d.ZERO, null, true)},
 * which injects directly into the MotionController and supersedes whatever steering
 * the AI instruction tree produced for this tick.
 */
public final class BreachFreezeSystem extends EntityTickingSystem<EntityStore> {

    @NonNullDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
            FrozenComponent.getComponentType(),
            NPCEntity.getComponentType()
        );
    }

    @Override
    public void tick(float deltaSeconds, int index, ArchetypeChunk<EntityStore> chunk,
                     @NonNullDecl Store<EntityStore> store,
                     @NonNullDecl CommandBuffer<EntityStore> cmd) {
        NPCEntity npc = chunk.getComponent(index, NPCEntity.getComponentType());
        if (npc == null || npc.getRole() == null) return;

        npc.getRole().forceVelocity(Vector3d.ZERO, null, true);
    }
}
