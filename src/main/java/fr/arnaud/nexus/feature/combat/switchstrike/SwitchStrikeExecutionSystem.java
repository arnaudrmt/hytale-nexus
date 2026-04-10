package fr.arnaud.nexus.feature.combat.switchstrike;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.camera.PlayerCameraSystem;
import fr.arnaud.nexus.component.RunSessionComponent;
import fr.arnaud.nexus.feature.breach.BreachSequenceComponent;
import fr.arnaud.nexus.feature.breach.BreachSequenceSystem;
import fr.arnaud.nexus.feature.breach.FrozenTargetComponent;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Drives the Switch Strike FSM from PENDING_OPEN through WINDOW_OPEN and into
 * either the Breach sequence or the normal Switch Strike outcome.
 */
public final class SwitchStrikeExecutionSystem extends EntityTickingSystem<EntityStore> {

    private static final Logger LOGGER = Logger.getLogger(SwitchStrikeExecutionSystem.class.getName());

    public SwitchStrikeExecutionSystem() {
    }

    @NonNullDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
            PlayerRef.getComponentType(),
            SwitchStrikeComponent.getComponentType(),
            EntityStatMap.getComponentType()
        );
    }

    @Override
    public void tick(float deltaSeconds, int index, ArchetypeChunk<EntityStore> chunk,
                     @NonNullDecl Store<EntityStore> store,
                     @NonNullDecl CommandBuffer<EntityStore> cmd) {
        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        SwitchStrikeComponent switchStrike =
            chunk.getComponent(index, SwitchStrikeComponent.getComponentType());

        if (switchStrike == null) return;

        switchStrike.tickBossTimer(deltaSeconds);

        switch (switchStrike.getState()) {
            case PENDING_OPEN -> commitWindow(ref, switchStrike, cmd);
            case WINDOW_OPEN -> tickOpenWindow(ref, switchStrike, deltaSeconds, store, cmd);
            default -> persist(ref, switchStrike, cmd);
        }
    }

    private void commitWindow(@NonNullDecl Ref<EntityStore> ref,
                              @NonNullDecl SwitchStrikeComponent switchStrike,
                              @NonNullDecl CommandBuffer<EntityStore> cmd) {
        switchStrike.commitOpenWindow();
        persist(ref, switchStrike, cmd);
    }

    private void tickOpenWindow(@NonNullDecl Ref<EntityStore> ref,
                                @NonNullDecl SwitchStrikeComponent switchStrike,
                                float deltaSeconds,
                                @NonNullDecl Store<EntityStore> store,
                                @NonNullDecl CommandBuffer<EntityStore> cmd) {
        boolean windowAlive = switchStrike.tickWindow(deltaSeconds);

        if (switchStrike.hasPendingSwap()) {
            handleSwap(ref, switchStrike, store, cmd);
        } else if (!windowAlive) {
            switchStrike.closeWindow();
            persist(ref, switchStrike, cmd);
        } else {
            persist(ref, switchStrike, cmd);
        }
    }

    private void handleSwap(@NonNullDecl Ref<EntityStore> ref,
                            @NonNullDecl SwitchStrikeComponent switchStrike,
                            @NonNullDecl Store<EntityStore> store,
                            @NonNullDecl CommandBuffer<EntityStore> cmd) {

        // TODO: Check Stamina
        if (/*flowHandler.getFilledSegments(ref, store) < 1*/ true) {
            switchStrike.closeWindow();
            persist(ref, switchStrike, cmd);
            return;
        }

        if (switchStrike.hasBossHitInWindow()) {
            executeBreachSequence(ref, switchStrike, store, cmd);
        } else {
            executeNormalSwitchStrike(ref, switchStrike, store, cmd);
        }
    }

    private void executeNormalSwitchStrike(@NonNullDecl Ref<EntityStore> ref,
                                           @NonNullDecl SwitchStrikeComponent switchStrike,
                                           @NonNullDecl Store<EntityStore> store,
                                           @NonNullDecl CommandBuffer<EntityStore> cmd) {
        // TODO: Drain Stamina
        recordSwitchStrike(ref, store, cmd);
        switchStrike.closeWindow();
        persist(ref, switchStrike, cmd);
    }

    /**
     * Launches the Breach sequence. Flow is intentionally NOT drained here —
     * {@link BreachSequenceSystem} drains it on exit so
     * the player retains full flow during the combo window.
     * The slot lock is NOT released here for the same reason.
     */
    private void executeBreachSequence(@NonNullDecl Ref<EntityStore> ref,
                                       @NonNullDecl SwitchStrikeComponent switchStrike,
                                       @NonNullDecl Store<EntityStore> store,
                                       @NonNullDecl CommandBuffer<EntityStore> cmd) {
        Ref<EntityStore> bossRef = switchStrike.getLastBossRef();
        if (bossRef == null || !bossRef.isValid()) {
            LOGGER.log(Level.WARNING, "[SwitchStrike] Breach launch aborted — boss ref invalid. Falling back.");
            executeNormalSwitchStrike(ref, switchStrike, store, cmd);
            return;
        }

        WorldTimeResource timeResource = store.getResource(WorldTimeResource.getResourceType());
        float savedDayTime = timeResource != null ? timeResource.getDayProgress() : 0f;

        cmd.run(s -> s.putComponent(ref, BreachSequenceComponent.getComponentType(),
            BreachSequenceComponent.forBoss(bossRef, savedDayTime)));
        cmd.run(s -> s.putComponent(bossRef, FrozenTargetComponent.getComponentType(), new FrozenTargetComponent()));

        PlayerCameraSystem.requestGlimpseEntry(ref, store, cmd);
        World.setTimeDilation(BreachSequenceComponent.TIME_DILATION_SLOW, store);

        if (timeResource != null) {
            World world = store.getExternalData().getWorld();
            if (world != null) timeResource.setDayTime(0f, world, store);
        }

        switchStrike.closeWindow();
        persist(ref, switchStrike, cmd);
    }

    private void recordSwitchStrike(@NonNullDecl Ref<EntityStore> ref,
                                    @NonNullDecl Store<EntityStore> store,
                                    @NonNullDecl CommandBuffer<EntityStore> cmd) {
        RunSessionComponent session = store.getComponent(ref, RunSessionComponent.getComponentType());
        if (session == null) return;
        session.incrementSwitchStrikes();
        cmd.run(s -> s.putComponent(ref, RunSessionComponent.getComponentType(), session));
    }

    private void persist(@NonNullDecl Ref<EntityStore> ref,
                         @NonNullDecl SwitchStrikeComponent switchStrike,
                         @NonNullDecl CommandBuffer<EntityStore> cmd) {
        cmd.run(s -> s.putComponent(ref, SwitchStrikeComponent.getComponentType(), switchStrike));
    }
}
