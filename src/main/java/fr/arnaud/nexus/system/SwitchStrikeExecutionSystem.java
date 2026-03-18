package fr.arnaud.nexus.system;

import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import fr.arnaud.nexus.component.RunSessionComponent;
import fr.arnaud.nexus.component.SwitchStrikeComponent;
import fr.arnaud.nexus.component.SwitchStrikeComponent.State;
import fr.arnaud.nexus.handler.FlowHandler;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Ticks the Switch Strike window and executes Phase C when conditions are met.
 * <p>
 * Mini-boss detection reads the NPC's role name via {@link NPCEntity#getNPCTypeId()},
 * which returns the {@code RoleName} field stored on the entity at spawn time.
 * Add future Sentinel role IDs (filenames without extension) to {@link #BOSS_ROLE_IDS}.
 * <p>
 * Branching logic:
 * - Mini-boss AND melee→ranged → Faille sequence (stubbed, falls back to normal).
 * - Everything else → normal path: drain flow, record strike, Phase C fires naturally.
 */
public final class SwitchStrikeExecutionSystem extends EntityTickingSystem<EntityStore> {

    private static final Logger LOGGER = Logger.getLogger(SwitchStrikeExecutionSystem.class.getName());

    /**
     * Role IDs (filenames without extension) that qualify as mini-bosses.
     * Extend this set as new Sentinel NPCs are added to the project.
     */
    private static final Set<String> BOSS_ROLE_IDS = Set.of(
        "Nexus_TestBoss_NPC_Role"
    );

    private final FlowHandler flowHandler;

    public SwitchStrikeExecutionSystem(@NonNullDecl FlowHandler flowHandler) {
        this.flowHandler = flowHandler;
    }

    public void onAssetsLoaded(
        LoadedAssetsEvent<String, EntityStatType,
            IndexedLookupTableAssetMap<String, EntityStatType>> event) {
        LOGGER.log(Level.INFO, "[SwitchStrike] SwitchStrikeExecutionSystem assets loaded.");
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

        if (switchStrike == null || switchStrike.getState() != State.WINDOW_OPEN) return;

        boolean windowAlive = switchStrike.tickWindow(deltaSeconds);

        if (switchStrike.hasPendingSwap()) {
            LOGGER.log(Level.INFO,
                "[SwitchStrike] STEP 2 — Swap detected. meleeToRanged=" + switchStrike.wasSwapMeleeToRanged()
                    + ". Window had " + String.format("%.2f", switchStrike.getWindowTimer()) + "s remaining.");
            handleSwap(ref, switchStrike, store, cmd);
        } else if (!windowAlive) {
            LOGGER.log(Level.INFO, "[SwitchStrike] STEP 2 FAIL — Window expired. No swap detected in time.");
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
        if (!flowHandler.isFull(ref, store)) {
            LOGGER.log(Level.INFO,
                "[SwitchStrike] STEP 3 FAIL — Flow check failed. Filled segments: "
                    + flowHandler.getFilledSegments(ref, store) + " / "
                    + flowHandler.getMaxSegments(ref, store) + ".");
            switchStrike.closeWindow();
            persist(ref, switchStrike, cmd);
            return;
        }

        LOGGER.log(Level.INFO, "[SwitchStrike] STEP 3 OK — Flow check passed. Full flow confirmed.");

        boolean isMiniBoss = isBossEntity(switchStrike.getAbilityTarget(), store);
        boolean isMeleeToRanged = switchStrike.wasSwapMeleeToRanged();

        LOGGER.log(Level.INFO,
            "[SwitchStrike] STEP 4 — Routing. isMiniBoss=" + isMiniBoss
                + ", isMeleeToRanged=" + isMeleeToRanged + ".");

        if (isMiniBoss && isMeleeToRanged) {
            LOGGER.log(Level.INFO, "[SwitchStrike] STEP 4 → Faille sequence branch (mini-boss + melee→ranged).");
            executeFailleSequence(ref, switchStrike, store, cmd);
        } else {
            LOGGER.log(Level.INFO, "[SwitchStrike] STEP 4 → Normal Switch Strike branch.");
            executeNormalSwitchStrike(ref, switchStrike, store, cmd);
        }
    }

    private void executeNormalSwitchStrike(@NonNullDecl Ref<EntityStore> ref,
                                           @NonNullDecl SwitchStrikeComponent switchStrike,
                                           @NonNullDecl Store<EntityStore> store,
                                           @NonNullDecl CommandBuffer<EntityStore> cmd) {
        flowHandler.drainFlow(ref, store);
        LOGGER.log(Level.INFO, "[SwitchStrike] STEP 5 OK — Flow drained. Switch Strike confirmed.");

        recordSwitchStrike(ref, store, cmd);

        switchStrike.closeWindow();
        persist(ref, switchStrike, cmd);
    }

    private void recordSwitchStrike(@NonNullDecl Ref<EntityStore> ref,
                                    @NonNullDecl Store<EntityStore> store,
                                    @NonNullDecl CommandBuffer<EntityStore> cmd) {
        RunSessionComponent session =
            store.getComponent(ref, RunSessionComponent.getComponentType());
        if (session == null) {
            LOGGER.log(Level.WARNING,
                "[SwitchStrike] STEP 6 WARN — RunSessionComponent missing, strike not recorded.");
            return;
        }
        session.incrementSwitchStrikes();
        cmd.run(s -> s.putComponent(ref, RunSessionComponent.getComponentType(), session));
        LOGGER.log(Level.INFO,
            "[SwitchStrike] STEP 6 OK — Strike recorded in RunSession. "
                + "Phase C: player's next attack fires naturally.");
    }

    /**
     * Returns true if the target entity is a registered mini-boss role.
     * <p>
     * Reads the role name via {@link NPCEntity#getNPCTypeId()}, which returns
     * the {@code RoleName} string stored on the entity at spawn time (see
     * {@link NPCEntity} CODEC field "RoleName"). The check is O(1) against
     * the immutable {@link #BOSS_ROLE_IDS} hash set. Returns false safely for
     * players, non-NPC entities, and invalid refs.
     */
    private boolean isBossEntity(@Nullable Ref<EntityStore> target,
                                 @NonNullDecl Store<EntityStore> store) {
        System.out.println(target == null);
        if (target == null || !target.isValid()) return false;

        NPCEntity npc = store.getComponent(target, NPCEntity.getComponentType());
        if (npc == null) return false;

        String roleId = npc.getNPCTypeId();
        System.out.println(roleId);
        boolean isBoss = roleId != null && BOSS_ROLE_IDS.contains(roleId);
        LOGGER.log(Level.INFO,
            "[SwitchStrike] Boss check: roleId=" + roleId + ", isBoss=" + isBoss);
        return isBoss;
    }

    /**
     * TODO: implement the Faille sequence —
     *   1. Freeze time (set global time scale to 0.3)
     *   2. Transition camera ISO → first-person (CameraSystem.beginGlimpse)
     *   3. Spawn weak-point entities on the mini-boss
     *   4. Open a 3-second aim window for the player
     *   5. On weak-point hit or timeout: restore time, drain flow
     * <p>
     * Falls back to the normal path until implemented.
     */
    private void executeFailleSequence(@NonNullDecl Ref<EntityStore> ref,
                                       @NonNullDecl SwitchStrikeComponent switchStrike,
                                       @NonNullDecl Store<EntityStore> store,
                                       @NonNullDecl CommandBuffer<EntityStore> cmd) {
        LOGGER.log(Level.WARNING,
            "[SwitchStrike] Faille sequence not yet implemented — falling back to normal Switch Strike.");
        executeNormalSwitchStrike(ref, switchStrike, store, cmd);
    }

    private void persist(@NonNullDecl Ref<EntityStore> ref,
                         @NonNullDecl SwitchStrikeComponent switchStrike,
                         @NonNullDecl CommandBuffer<EntityStore> cmd) {
        cmd.run(s -> s.putComponent(ref, SwitchStrikeComponent.getComponentType(), switchStrike));
    }
}
