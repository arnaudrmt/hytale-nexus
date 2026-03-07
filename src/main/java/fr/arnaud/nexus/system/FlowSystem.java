package fr.arnaud.nexus.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.component.FlowComponent;
import fr.arnaud.nexus.component.LucidityComponent;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

/**
 * System that processes Flow and Lucidity state each tick.
 * <p>
 * Handles continuous Lucidity drain for players in "Unstable Sleep"
 * and monitors resource thresholds.
 */
public final class FlowSystem extends EntityTickingSystem<EntityStore> {

    public FlowSystem() {
        super();
    }

    @NonNullDecl
    @Override
    public Query<EntityStore> getQuery() {
        return FlowComponent.getComponentType();
    }

    @Override
    public void tick(float deltaSeconds, int index, @NonNullDecl ArchetypeChunk<EntityStore> chunk,
                     @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer) {

        FlowComponent flow = chunk.getComponent(index, FlowComponent.getComponentType());
        LucidityComponent lucidity = chunk.getComponent(index, LucidityComponent.getComponentType());

        if (flow == null || lucidity == null) return;

        handleUnstableSleepDrain(lucidity, deltaSeconds);
    }

    /**
     * Applies passive Lucidity drain if the player is in "Unstable Sleep".
     */
    private void handleUnstableSleepDrain(LucidityComponent lucidity, float deltaSeconds) {
        if (lucidity.isUnstableSleep()) {
            lucidity.drain(LucidityComponent.COOP_DRAIN_RATE_PER_SECOND * deltaSeconds);
        }
    }
}
