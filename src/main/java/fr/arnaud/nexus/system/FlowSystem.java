package fr.arnaud.nexus.system;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.component.AwakeningMarkerComponent;
import fr.arnaud.nexus.component.FlowComponent;
import fr.arnaud.nexus.component.LucidityComponent;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

/**
 * Ticking ECS system responsible for:
 *   1. Draining Lucidity while a player is in Unstable Sleep (co-op).
 *   2. Queuing the Awakening marker via CommandBuffer when lucidity hits zero.
 *
 * Flow generation itself is event-driven (damage dealt → DamageSystem).
 *
 * GDD refs (§ Bar de Flow / Taux de Génération, § Fiche Joueur / Sommeil instable).
 *
 * Registration in Nexus#registerSystems():
 *   getEntityStoreRegistry().registerSystem(new FlowSystem());
 */
public final class FlowSystem extends EntityTickingSystem<EntityStore> {

    // -------------------------------------------------------------------------
    // Constructor — EntityTickingSystem takes no component-type args;
    // entity filtering is handled via getQuery().
    // -------------------------------------------------------------------------

    public FlowSystem() {
        super();
    }

    /**
     * Only tick entities that have a FlowComponent attached.
     * LucidityComponent presence is checked defensively inside tick().
     */
    @NonNullDecl
    @Override
    public Query<EntityStore> getQuery() {
        return FlowComponent.getComponentType();
    }

    // -------------------------------------------------------------------------
    // Tick — signature matches EntityTickingSystem<ECS_TYPE>:
    //   tick(float deltaSeconds, int index, ArchetypeChunk, Store, CommandBuffer)
    // -------------------------------------------------------------------------

    @Override
    public void tick(
            float deltaSeconds,
            int index,
            @NonNullDecl ArchetypeChunk<EntityStore> chunk,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl CommandBuffer<EntityStore> commandBuffer
    ) {
        Ref<EntityStore> ref = chunk.getReferenceTo(index);

        FlowComponent flow = chunk.getComponent(index, FlowComponent.getComponentType());
        LucidityComponent lucidity = chunk.getComponent(index, LucidityComponent.getComponentType());

        if (flow == null || lucidity == null) return;

        // --- Unstable Sleep drain (co-op) ------------------------------------
        // GDD: "drain continu" while a partner is in Unstable Sleep.
        if (lucidity.isUnstableSleep()) {
            boolean depleted = lucidity.drain(
                    LucidityComponent.COOP_DRAIN_RATE_PER_SECOND * deltaSeconds
            );

            if (depleted && store.getComponent(ref, AwakeningMarkerComponent.getComponentType()) != null) {
                // Deferred structural change — safe inside a ticking system.
                commandBuffer.addComponent(
                        ref,
                        AwakeningMarkerComponent.getComponentType(),
                        new AwakeningMarkerComponent()
                );
            }
        }
    }

    // --- Placeholder: passive Flow decay / regen rules go here ----------
    // e.g., slow Flow regeneration in safe zones,
    //       post-Switch-Strike cooldown reset.
}
