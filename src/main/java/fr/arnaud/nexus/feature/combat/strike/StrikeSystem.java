package fr.arnaud.nexus.feature.combat.strike;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.ability.ActiveCoreComponent;
import fr.arnaud.nexus.ability.CoreAbility;
import fr.arnaud.nexus.camera.PlayerCameraSystem;
import fr.arnaud.nexus.item.weapon.enchantment.impl.EnchantEffectUtil;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

/**
 * Drives the Strike FSM tick-by-tick.
 *
 * <pre>
 *   HIT_WINDOW (2s) → if hits collected: freeze all targets, slow time, enter COMBO
 *                   → if no hits: reset to IDLE
 *   COMBO (5s)      → StrikeHitInterceptor accumulates per-target damage
 *                   → on expiry: burst damage each target, restore time, exit camera
 * </pre>
 */
public final class StrikeSystem extends EntityTickingSystem<EntityStore> {

    private static final float TIME_DILATION_COMBAT = 0.3f;
    private static final float TIME_DILATION_NORMAL = 1.0f;

    @NonNullDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
            PlayerRef.getComponentType(),
            StrikeComponent.getComponentType(),
            ActiveCoreComponent.getComponentType()
        );
    }

    @Override
    public void tick(float deltaSeconds, int index, @NonNullDecl ArchetypeChunk<EntityStore> chunk,
                     @NonNullDecl Store<EntityStore> store,
                     @NonNullDecl CommandBuffer<EntityStore> cmd) {
        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        StrikeComponent strike = store.getComponent(ref, StrikeComponent.getComponentType());
        if (strike == null) return;

        // Consume pending marker if present — safe because marker was put via store.putComponent
        // on the world thread before this tick, so store.getComponent sees it even if chunk is stale
        if (strike.getState() == StrikeComponent.State.IDLE) {
            StrikePendingComponent pending = store.getComponent(ref, StrikePendingComponent.getComponentType());
            if (pending != null) {
                // Only open window if the Strike core is equipped
                ActiveCoreComponent activeCore = store.getComponent(ref, ActiveCoreComponent.getComponentType());
                if (activeCore != null && activeCore.hasEquipped(CoreAbility.SWITCH_STRIKE)) {
                    strike.openHitWindow();
                }
                // Always consume the marker regardless — prevents stale markers if core is unequipped
                cmd.run(s -> s.removeComponentIfExists(ref, StrikePendingComponent.getComponentType()));
            }
        }

        switch (strike.getState()) {
            case HIT_WINDOW -> tickHitWindow(ref, strike, deltaSeconds, store, cmd);
            case COMBO -> tickCombo(ref, strike, deltaSeconds, store, cmd);
            default -> {
            }
        }
    }

    // ── HIT_WINDOW ────────────────────────────────────────────────────────────

    private void tickHitWindow(@NonNullDecl Ref<EntityStore> ref,
                               @NonNullDecl StrikeComponent strike,
                               float deltaSeconds,
                               @NonNullDecl Store<EntityStore> store,
                               @NonNullDecl CommandBuffer<EntityStore> cmd) {
        if (strike.hasHitTargets()) {
            enterCombo(ref, strike, store, cmd);
            return;
        }

        if (!strike.tickTimer(deltaSeconds)) {
            strike.reset();
            persist(ref, strike, cmd);
        } else {
            persist(ref, strike, cmd);
        }
    }

    private void enterCombo(@NonNullDecl Ref<EntityStore> ref,
                            @NonNullDecl StrikeComponent strike,
                            @NonNullDecl Store<EntityStore> store,
                            @NonNullDecl CommandBuffer<EntityStore> cmd) {
        strike.getTargetCombos().forEach((targetRef, combo) -> {
            if (targetRef.isValid()) {
                EnchantEffectUtil.applyEffect(targetRef, cmd, "Freeze", StrikeComponent.COMBO_WINDOW_SECONDS);
            }
        });

        Ref<EntityStore> focusRef = strike.getCameraFocusRef();
        if (focusRef != null && focusRef.isValid()) {
            PlayerCameraSystem.requestGlimpseEntry(ref, store, cmd);
        }

        World.setTimeDilation(TIME_DILATION_COMBAT, store);
        strike.openComboWindow();
        persist(ref, strike, cmd);
    }

    private void tickCombo(@NonNullDecl Ref<EntityStore> ref,
                           @NonNullDecl StrikeComponent strike,
                           float deltaSeconds,
                           @NonNullDecl Store<EntityStore> store,
                           @NonNullDecl CommandBuffer<EntityStore> cmd) {
        if (!strike.tickTimer(deltaSeconds)) {
            exitCombo(ref, strike, store, cmd);
        } else {
            persist(ref, strike, cmd);
        }
    }

    private void exitCombo(@NonNullDecl Ref<EntityStore> ref,
                           @NonNullDecl StrikeComponent strike,
                           @NonNullDecl Store<EntityStore> store,
                           @NonNullDecl CommandBuffer<EntityStore> cmd) {
        applyBurstDamageToAllTargets(ref, strike, cmd);
        World.setTimeDilation(TIME_DILATION_NORMAL, store);
        PlayerCameraSystem.requestGlimpseExit(ref, store, cmd);
        strike.reset();
        persist(ref, strike, cmd);
    }

    private void applyBurstDamageToAllTargets(@NonNullDecl Ref<EntityStore> ref,
                                              @NonNullDecl StrikeComponent strike,
                                              @NonNullDecl CommandBuffer<EntityStore> cmd) {
        strike.getTargetCombos().forEach((targetRef, combo) -> {
            if (!targetRef.isValid() || combo.accumulatedDamage <= 0f) return;

            Damage burst = new Damage(
                new Damage.EntitySource(ref),
                DamageCause.getAssetMap().getAsset("Physical"),
                combo.accumulatedDamage
            );
            cmd.run(s -> s.invoke(targetRef, burst));
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void persist(@NonNullDecl Ref<EntityStore> ref,
                         @NonNullDecl StrikeComponent strike,
                         @NonNullDecl CommandBuffer<EntityStore> cmd) {
        cmd.run(s -> s.putComponent(ref, StrikeComponent.getComponentType(), strike));
    }
}
