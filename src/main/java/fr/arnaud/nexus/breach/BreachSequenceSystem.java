package fr.arnaud.nexus.breach;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.Invulnerable;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.Nexus;
import fr.arnaud.nexus.breach.BreachSequenceComponent.State;
import fr.arnaud.nexus.camera.CameraComponent;
import fr.arnaud.nexus.camera.CameraMode;
import fr.arnaud.nexus.camera.CameraSystem;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Drives the Breach Sequence FSM for each player.
 * <p>
 * ENTERING — camera transition via {@link CameraSystem#requestGlimpseEntry} + visual fade.
 * On GLIMPSE_ACTIVE: freezes boss, begins combo, advances to ACTIVE.
 * ACTIVE   — ticks aim window; {@link BreachDamageInterceptor} accumulates combo.
 * EXITING  — unfreezes boss, applies burst damage, launches player, restores visuals.
 */
public final class BreachSequenceSystem extends EntityTickingSystem<EntityStore> {

    private static final Logger LOGGER = Logger.getLogger(BreachSequenceSystem.class.getName());

    private static final String HEALTH_STAT_ID = "Health";
    private static final double LAUNCH_DISTANCE = 5.0;
    private static final double LAUNCH_VERTICAL = 4.0;

    @NonNullDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
            PlayerRef.getComponentType(),
            BreachSequenceComponent.getComponentType(),
            CameraComponent.getComponentType()
        );
    }

    @Override
    public void tick(float deltaSeconds, int index, @NonNullDecl ArchetypeChunk<EntityStore> chunk,
                     @NonNullDecl Store<EntityStore> store,
                     @NonNullDecl CommandBuffer<EntityStore> cmd) {
        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        BreachSequenceComponent breach = chunk.getComponent(index, BreachSequenceComponent.getComponentType());
        CameraComponent cam = chunk.getComponent(index, CameraComponent.getComponentType());

        if (breach == null || cam == null || breach.getState() == State.IDLE) return;

        switch (breach.getState()) {
            case ENTERING -> handleEntering(ref, breach, cam, store, cmd);
            case ACTIVE -> handleActive(deltaSeconds, ref, breach, store, cmd);
            case EXITING -> handleExiting(ref, breach, cam, store, cmd);
            default -> {
            }
        }

        persist(ref, breach, cmd);
    }

    // --- State handlers ---

    private void handleEntering(@NonNullDecl Ref<EntityStore> ref,
                                @NonNullDecl BreachSequenceComponent breach,
                                @NonNullDecl CameraComponent cam,
                                @NonNullDecl Store<EntityStore> store,
                                @NonNullDecl CommandBuffer<EntityStore> cmd) {
        if (cam.getMode() == CameraMode.ISO_RUN) {
            CameraSystem.requestGlimpseEntry(ref, store, cmd);
            BreachVisualSystem.beginFadeIn(ref, store, cmd);
            LOGGER.log(Level.INFO, "[Breach] ENTERING — camera and visual fade started.");
        }

        if (cam.getMode() == CameraMode.GLIMPSE_ACTIVE) {
            Ref<EntityStore> bossRef = breach.getBossRef();
            if (bossRef != null && bossRef.isValid()) {
                freezeBoss(bossRef, cmd);
                beginCombo(ref, store, cmd);
            } else {
                LOGGER.log(Level.WARNING, "[Breach] ENTERING — bossRef invalid.");
            }
            breach.onEnteringComplete();
            LOGGER.log(Level.INFO, "[Breach] ENTERING → ACTIVE — boss frozen, combo open.");
        }
    }

    private void handleActive(float deltaSeconds,
                              @NonNullDecl Ref<EntityStore> ref,
                              @NonNullDecl BreachSequenceComponent breach,
                              @NonNullDecl Store<EntityStore> store,
                              @NonNullDecl CommandBuffer<EntityStore> cmd) {
        boolean windowAlive = breach.tickAimWindow(deltaSeconds / BreachSequenceComponent.TIME_DILATION);
        if (!windowAlive) {
            LOGGER.log(Level.INFO, "[Breach] ACTIVE → EXITING — aim window expired.");
            breach.beginExit(false);
        }
    }

    private void handleExiting(@NonNullDecl Ref<EntityStore> ref,
                               @NonNullDecl BreachSequenceComponent breach,
                               @NonNullDecl CameraComponent cam,
                               @NonNullDecl Store<EntityStore> store,
                               @NonNullDecl CommandBuffer<EntityStore> cmd) {
        if (cam.getMode() == CameraMode.GLIMPSE_ACTIVE) {
            Ref<EntityStore> bossRef = breach.getBossRef();
            if (bossRef != null && bossRef.isValid()) {
                unfreezeBoss(bossRef, cmd);
                applyBurstAndLaunch(ref, bossRef, store, cmd);
            }
            CameraSystem.requestGlimpseExit(ref, store, cmd);
            BreachVisualSystem.beginFadeOut(ref, store, cmd);
            LOGGER.log(Level.INFO, "[Breach] EXITING — burst applied, camera exit started.");
        }

        if (cam.getMode() == CameraMode.ISO_RUN) {
            breach.onExitComplete();
            LOGGER.log(Level.INFO, "[Breach] EXITING → IDLE — sequence complete.");
        }
    }

    // --- Boss freeze / unfreeze ---

    private static void freezeBoss(@NonNullDecl Ref<EntityStore> bossRef,
                                   @NonNullDecl CommandBuffer<EntityStore> cmd) {
        cmd.run(s -> s.putComponent(bossRef, Invulnerable.getComponentType(), Invulnerable.INSTANCE));
        LOGGER.log(Level.INFO, "[Breach] Boss frozen.");
    }

    private static void unfreezeBoss(@NonNullDecl Ref<EntityStore> bossRef,
                                     @NonNullDecl CommandBuffer<EntityStore> cmd) {
        cmd.run(s -> s.tryRemoveComponent(bossRef, Invulnerable.getComponentType()));
        LOGGER.log(Level.INFO, "[Breach] Boss unfrozen.");
    }

    // --- Combo ---

    private static void beginCombo(@NonNullDecl Ref<EntityStore> ref,
                                   @NonNullDecl Store<EntityStore> store,
                                   @NonNullDecl CommandBuffer<EntityStore> cmd) {
        BreachComboComponent combo = store.getComponent(ref, BreachComboComponent.getComponentType());
        if (combo == null) return;
        combo.beginCombo();
        cmd.run(s -> s.putComponent(ref, BreachComboComponent.getComponentType(), combo));
        LOGGER.log(Level.INFO, "[Breach] Combo started.");
    }

    // --- Burst damage + player launch ---

    private static void applyBurstAndLaunch(@NonNullDecl Ref<EntityStore> playerRef,
                                            @NonNullDecl Ref<EntityStore> bossRef,
                                            @NonNullDecl Store<EntityStore> store,
                                            @NonNullDecl CommandBuffer<EntityStore> cmd) {
        BreachComboComponent combo = store.getComponent(playerRef, BreachComboComponent.getComponentType());
        if (combo == null) return;

        int hits = combo.getHitCount();
        float totalDamage = combo.consumeAccumulatedDamage();
        cmd.run(s -> s.putComponent(playerRef, BreachComboComponent.getComponentType(), combo));

        System.out.println("[Breach] Burst: " + totalDamage + " damage over " + hits + " hits.");
        LOGGER.log(Level.INFO, "[Breach] Burst: " + totalDamage + " damage over " + hits + " hits.");

        if (totalDamage > 0f) {
            EntityStatMap bossStats = store.getComponent(bossRef, EntityStatMap.getComponentType());
            if (bossStats != null) {
                int healthIndex = EntityStatType.getAssetMap().getIndex(HEALTH_STAT_ID);
                if (healthIndex != Integer.MIN_VALUE) {
                    bossStats.subtractStatValue(EntityStatMap.Predictable.ALL, healthIndex, totalDamage);
                    cmd.run(s -> s.putComponent(bossRef, EntityStatMap.getComponentType(), bossStats));
                    System.out.println("[Breach] Health stat subtracted: " + totalDamage);
                } else {
                    Nexus.get().getLogger().at(Level.WARNING).log("[Breach] Health stat '" + HEALTH_STAT_ID + "' not found.");
                }
            } else {
                System.out.println("[Breach] Boss EntityStatMap is null — cannot apply burst damage.");
            }
        } else {
            System.out.println("[Breach] No damage accumulated — player landed 0 hits during breach.");
        }

        launchPlayerFromBoss(playerRef, bossRef, store, cmd);
    }

    private static void launchPlayerFromBoss(@NonNullDecl Ref<EntityStore> playerRef,
                                             @NonNullDecl Ref<EntityStore> bossRef,
                                             @NonNullDecl Store<EntityStore> store,
                                             @NonNullDecl CommandBuffer<EntityStore> cmd) {
        TransformComponent playerTransform = store.getComponent(playerRef, TransformComponent.getComponentType());
        TransformComponent bossTransform = store.getComponent(bossRef, TransformComponent.getComponentType());
        if (playerTransform == null || bossTransform == null) {
            System.out.println("[Breach] Cannot launch player — transform missing.");
            return;
        }

        Vector3d playerPos = playerTransform.getPosition();
        Vector3d bossPos = bossTransform.getPosition();

        double dx = playerPos.getX() - bossPos.getX();
        double dz = playerPos.getZ() - bossPos.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        double normX = horizontalDist > 0.001 ? dx / horizontalDist : 1.0;
        double normZ = horizontalDist > 0.001 ? dz / horizontalDist : 0.0;

        Player player = store.getComponent(playerRef, Player.getComponentType());
        if (player == null) {
            System.out.println("[Breach] Cannot launch player — Player component missing.");
            return;
        }

        player.addLocationChange(playerRef, normX * LAUNCH_DISTANCE, LAUNCH_VERTICAL, normZ * LAUNCH_DISTANCE, store);
        System.out.println("[Breach] Player launched " + LAUNCH_DISTANCE + " blocks from boss.");
        LOGGER.log(Level.INFO, "[Breach] Player launched.");
    }

    // --- Helpers ---

    private void persist(@NonNullDecl Ref<EntityStore> ref,
                         @NonNullDecl BreachSequenceComponent breach,
                         @NonNullDecl CommandBuffer<EntityStore> cmd) {
        cmd.run(s -> s.putComponent(ref, BreachSequenceComponent.getComponentType(), breach));
    }

    public static boolean beginSequence(@NonNullDecl Ref<EntityStore> playerRef,
                                        @NonNullDecl Ref<EntityStore> bossRef,
                                        @NonNullDecl Store<EntityStore> store,
                                        @NonNullDecl CommandBuffer<EntityStore> cmd) {
        BreachSequenceComponent breach =
            store.getComponent(playerRef, BreachSequenceComponent.getComponentType());
        if (breach == null) return false;

        boolean started = breach.begin(bossRef);
        if (started) {
            cmd.run(s -> s.putComponent(playerRef, BreachSequenceComponent.getComponentType(), breach));
            LOGGER.log(Level.INFO, "[Breach] Sequence started. Boss ref: "
                + (bossRef != null && bossRef.isValid() ? "valid" : "INVALID"));
        }
        return started;
    }
}
