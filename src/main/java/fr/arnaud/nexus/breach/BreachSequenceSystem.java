package fr.arnaud.nexus.breach;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.camera.CameraComponent;
import fr.arnaud.nexus.camera.CameraMode;
import fr.arnaud.nexus.camera.CameraSystem;
import fr.arnaud.nexus.handler.FlowHandler;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Drives the per-player Breach sequence FSM.
 *
 * <p>Phase transitions:
 * <pre>
 *   ENTRY  → polls CameraMode until GLIMPSE_ACTIVE, then advances to ACTIVE
 *   ACTIVE → 5-second (real-time) combo window; {@link BreachDamageInterceptor} accumulates hits
 *   EXIT   → burst damage, unfreeze boss, restore time + day, camera exit, drain flow, unlock slot
 * </pre>
 * EXIT runs exactly once — the component is removed at the end of that tick.
 */
public final class BreachSequenceSystem extends EntityTickingSystem<EntityStore> {

    private static final Logger LOGGER = Logger.getLogger(BreachSequenceSystem.class.getName());

    private final FlowHandler flowHandler;

    public BreachSequenceSystem(@NonNullDecl FlowHandler flowHandler) {
        this.flowHandler = flowHandler;
    }

    @NonNullDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
            PlayerRef.getComponentType(),
            BreachSequenceComponent.getComponentType()
        );
    }

    @Override
    public void tick(float deltaSeconds, int index, ArchetypeChunk<EntityStore> chunk,
                     @NonNullDecl Store<EntityStore> store,
                     @NonNullDecl CommandBuffer<EntityStore> cmd) {
        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        BreachSequenceComponent sequence =
            chunk.getComponent(index, BreachSequenceComponent.getComponentType());

        if (sequence == null) return;

        switch (sequence.getPhase()) {
            case ENTRY -> tickEntry(ref, sequence, store, cmd);
            case ACTIVE -> tickActive(ref, sequence, deltaSeconds, cmd);
            case EXIT -> tickExit(ref, sequence, store, cmd);
        }
    }

    private void tickEntry(@NonNullDecl Ref<EntityStore> ref,
                           @NonNullDecl BreachSequenceComponent sequence,
                           @NonNullDecl Store<EntityStore> store,
                           @NonNullDecl CommandBuffer<EntityStore> cmd) {
        CameraComponent cam = store.getComponent(ref, CameraComponent.getComponentType());
        if (cam == null) {
            LOGGER.log(Level.WARNING, "[Breach] CameraComponent missing during ENTRY — aborting.");
            abort(ref, sequence, store, cmd);
            return;
        }

        if (cam.getMode() == CameraMode.GLIMPSE_ACTIVE) {
            sequence.advanceToActive();
            persist(ref, sequence, cmd);
        }
    }

    private void tickActive(@NonNullDecl Ref<EntityStore> ref,
                            @NonNullDecl BreachSequenceComponent sequence,
                            float deltaSeconds,
                            @NonNullDecl CommandBuffer<EntityStore> cmd) {
        if (!sequence.tickWindow(deltaSeconds)) {
            sequence.advanceToExit();
        }
        persist(ref, sequence, cmd);
    }

    private void tickExit(@NonNullDecl Ref<EntityStore> ref,
                          @NonNullDecl BreachSequenceComponent sequence,
                          @NonNullDecl Store<EntityStore> store,
                          @NonNullDecl CommandBuffer<EntityStore> cmd) {
        applyBurstDamage(ref, sequence, cmd);
        unfreezeBoss(sequence, cmd);
        restoreTimeDilation(store);
        restoreDayTime(sequence, store);
        CameraSystem.requestGlimpseExit(ref, store, cmd);
        flowHandler.drainFlow(ref, store);
        cmd.run(s -> s.removeComponentIfExists(ref, BreachSequenceComponent.getComponentType()));
    }

    /**
     * Burst damage is queued through the CommandBuffer so it fires after the
     * current tick's processing lock is released. Calling store.invoke() directly
     * inside a ticking system would trigger assertWriteProcessing() and crash the world.
     */
    private void applyBurstDamage(@NonNullDecl Ref<EntityStore> ref,
                                  @NonNullDecl BreachSequenceComponent sequence,
                                  @NonNullDecl CommandBuffer<EntityStore> cmd) {
        Ref<EntityStore> bossRef = sequence.getBossRef();
        if (bossRef == null || !bossRef.isValid()) {
            LOGGER.log(Level.WARNING, "[Breach] Boss ref invalid at EXIT — no burst damage.");
            return;
        }
        if (sequence.getPendingDamage() <= 0f) return;

        // TODO: replace "Physical" with a dedicated "Breach_Burst" DamageCause asset.
        Damage burst = new Damage(
            new Damage.EntitySource(ref),
            DamageCause.getAssetMap().getAsset("Physical"),
            sequence.getPendingDamage()
        );
        cmd.run(s -> s.invoke(bossRef, burst));
    }

    private void unfreezeBoss(@NonNullDecl BreachSequenceComponent sequence,
                              @NonNullDecl CommandBuffer<EntityStore> cmd) {
        Ref<EntityStore> bossRef = sequence.getBossRef();
        if (bossRef == null || !bossRef.isValid()) return;
        cmd.run(s -> s.removeComponentIfExists(bossRef, FrozenComponent.getComponentType()));
    }

    private void restoreTimeDilation(@NonNullDecl Store<EntityStore> store) {
        World.setTimeDilation(BreachSequenceComponent.TIME_DILATION_NORMAL, store);
    }

    private void restoreDayTime(@NonNullDecl BreachSequenceComponent sequence,
                                @NonNullDecl Store<EntityStore> store) {
        World world = store.getExternalData().getWorld();
        WorldTimeResource timeResource = store.getResource(WorldTimeResource.getResourceType());
        if (timeResource == null || world == null) {
            LOGGER.log(Level.WARNING, "[Breach] Could not restore day time — resource or world missing.");
            return;
        }
        timeResource.setDayTime(sequence.getSavedDayTime(), world, store);
    }

    private void abort(@NonNullDecl Ref<EntityStore> ref,
                       @NonNullDecl BreachSequenceComponent sequence,
                       @NonNullDecl Store<EntityStore> store,
                       @NonNullDecl CommandBuffer<EntityStore> cmd) {
        unfreezeBoss(sequence, cmd);
        restoreTimeDilation(store);
        CameraSystem.requestGlimpseExit(ref, store, cmd);
        cmd.run(s -> s.removeComponentIfExists(ref, BreachSequenceComponent.getComponentType()));
    }

    private void persist(@NonNullDecl Ref<EntityStore> ref,
                         @NonNullDecl BreachSequenceComponent sequence,
                         @NonNullDecl CommandBuffer<EntityStore> cmd) {
        cmd.run(s -> s.putComponent(ref, BreachSequenceComponent.getComponentType(), sequence));
    }
}
