package fr.arnaud.nexus.ability.strike;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.ability.core.ActiveCoreComponent;
import fr.arnaud.nexus.ability.core.CoreAbility;
import fr.arnaud.nexus.camera.PlayerCameraSystem;
import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.feature.resource.PlayerStatsManager;
import fr.arnaud.nexus.item.weapon.enchantment.impl.EnchantEffectUtil;
import fr.arnaud.nexus.session.RunSessionComponent;
import fr.arnaud.nexus.util.EntityUtil;
import fr.arnaud.nexus.util.KnockbackUtil;
import fr.arnaud.nexus.util.SpatialUtil;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.List;

public final class StrikeSystem extends EntityTickingSystem<EntityStore> {

    private static final float TIME_DILATION_COMBAT = 0.5f;
    private static final float TIME_DILATION_NORMAL = 1.0f;

    private static final float SHOCKWAVE_RADIUS = 7.0f;

    private static final float SHOCKWAVE_FORCE = 2.0f;
    private static final int SHOCKWAVE_SOUND = SoundEvent.getAssetMap().getIndex("SFX_Bomb_Fire_Goblin_Death");


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

        tickShockwavePending(ref, deltaSeconds, store, cmd);

        if (strike.getState() == StrikeComponent.State.IDLE) {
            StrikePendingComponent pending = store.getComponent(ref, StrikePendingComponent.getComponentType());
            if (pending != null) {
                ActiveCoreComponent activeCore = store.getComponent(ref, ActiveCoreComponent.getComponentType());
                if (activeCore != null && activeCore.hasEquipped(CoreAbility.SWITCH_STRIKE)) {
                    strike.openHitWindow();
                }
                cmd.run(s -> s.removeComponentIfExists(ref, StrikePendingComponent.getComponentType()));
            }
        }

        if (strike.getState() == StrikeComponent.State.SWITCH_WINDOW) {
            StrikeSwapConfirmedComponent swapConfirmed = store.getComponent(ref, StrikeSwapConfirmedComponent.getComponentType());
            if (swapConfirmed != null) {
                cmd.run(s -> s.removeComponentIfExists(ref, StrikeSwapConfirmedComponent.getComponentType()));

                PlayerStatsManager psm = Nexus.getInstance().getPlayerStatsManager();
                float cost = psm.getMaxStamina(ref, store) * StrikeComponent.STAMINA_COST_RATIO;
                psm.removeStamina(ref, store, cost);

                enterCombo(ref, strike, store, cmd);
                return;
            }
        }

        switch (strike.getState()) {
            case HIT_WINDOW -> tickHitWindow(ref, strike, deltaSeconds, cmd);
            case SWITCH_WINDOW -> tickSwitchWindow(ref, strike, deltaSeconds, cmd);
            case COMBO -> tickCombo(ref, strike, deltaSeconds, store, cmd);
            default -> {
            }
        }
    }

    private void tickHitWindow(@NonNullDecl Ref<EntityStore> ref,
                               @NonNullDecl StrikeComponent strike,
                               float deltaSeconds,
                               @NonNullDecl CommandBuffer<EntityStore> cmd) {
        if (strike.hasHitTargets()) {
            strike.openSwitchWindow();
            persist(ref, strike, cmd);
            return;
        }

        if (!strike.tickTimer(deltaSeconds)) {
            strike.reset();
            persist(ref, strike, cmd);
        } else {
            persist(ref, strike, cmd);
        }
    }

    private void tickSwitchWindow(@NonNullDecl Ref<EntityStore> ref,
                                  @NonNullDecl StrikeComponent strike,
                                  float deltaSeconds,
                                  @NonNullDecl CommandBuffer<EntityStore> cmd) {
        if (!strike.tickTimer(deltaSeconds)) {
            strike.reset();
        } else {
            persist(ref, strike, cmd);
        }
    }

    private void enterCombo(@NonNullDecl Ref<EntityStore> ref,
                            @NonNullDecl StrikeComponent strike,
                            @NonNullDecl Store<EntityStore> store,
                            @NonNullDecl CommandBuffer<EntityStore> cmd) {
        strike.getTargetCombos().forEach((targetRef, _) -> {
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
        cmd.run(s -> s.putComponent(ref, StrikeShockwavePendingComponent.getComponentType(), new StrikeShockwavePendingComponent()));
        strike.reset();
        persist(ref, strike, cmd);
    }

    private void tickShockwavePending(@NonNullDecl Ref<EntityStore> ref,
                                      float deltaSeconds,
                                      @NonNullDecl Store<EntityStore> store,
                                      @NonNullDecl CommandBuffer<EntityStore> cmd) {
        StrikeShockwavePendingComponent pending = store.getComponent(ref, StrikeShockwavePendingComponent.getComponentType());
        if (pending == null) return;

        pending.remainingSeconds -= deltaSeconds;
        if (pending.remainingSeconds > 0f) {
            cmd.run(s -> s.putComponent(ref, StrikeShockwavePendingComponent.getComponentType(), pending));
            return;
        }

        cmd.run(s -> s.removeComponentIfExists(ref, StrikeShockwavePendingComponent.getComponentType()));
        fireShockwave(ref, store, cmd);
    }

    private void fireShockwave(@NonNullDecl Ref<EntityStore> ref,
                               @NonNullDecl Store<EntityStore> store,
                               @NonNullDecl CommandBuffer<EntityStore> cmd) {
        StrikeComponent strike = store.getComponent(ref, StrikeComponent.getComponentType());
        float largestCombo = strike == null ? 0f : strike.getTargetCombos().values().stream()
                                                         .map(c -> c.accumulatedDamage)
                                                         .max(Float::compareTo)
                                                         .orElse(0f);
        float shockwaveDamage = largestCombo * 0.10f;

        TransformComponent origin = store.getComponent(ref, TransformComponent.getComponentType());
        if (origin == null) return;

        ParticleUtil.spawnParticleEffect(
            "Explosion_Big",
            new Vector3d((float) origin.getPosition().getX(), (float) origin.getPosition().getY(), (float) origin.getPosition().getZ()),
            store
        );

        SoundUtil.playSoundEvent3dToPlayer(ref, SHOCKWAVE_SOUND, SoundCategory.UI, origin.getPosition(), store);

        List<Ref<EntityStore>> nearby = SpatialUtil.getEntitiesInRadius(ref, SHOCKWAVE_RADIUS, cmd);
        for (Ref<EntityStore> targetRef : nearby) {
            if (!targetRef.isValid() || EntityUtil.isSameEntityRef(targetRef, ref)) continue;

            KnockbackUtil.applyRadialKnockbackImpulse(ref, targetRef, SHOCKWAVE_FORCE, cmd);

            DamageCause damageCause = DamageCause.getAssetMap().getAsset("Physical");
            if (damageCause == null) return;

            if (shockwaveDamage > 0f) {
                Damage hit = new Damage(
                    new Damage.EntitySource(ref),
                    damageCause,
                    shockwaveDamage
                );
                cmd.run(s -> s.invoke(targetRef, hit));
            }
        }
    }

    private void applyBurstDamageToAllTargets(@NonNullDecl Ref<EntityStore> ref,
                                              @NonNullDecl StrikeComponent strike,
                                              @NonNullDecl CommandBuffer<EntityStore> cmd) {
        strike.getTargetCombos().forEach((targetRef, combo) -> {
            if (!targetRef.isValid() || combo.accumulatedDamage <= 0f) return;

            DamageCause damageCause = DamageCause.getAssetMap().getAsset("Physical");
            if (damageCause == null) return;

            Damage burst = new Damage(
                new Damage.EntitySource(ref),
                damageCause,
                combo.accumulatedDamage
            );
            cmd.run(s -> s.invoke(targetRef, burst));

            RunSessionComponent session = ref.getStore().getComponent(ref, RunSessionComponent.getComponentType());
            if (session != null) {
                session.addDamageDealt(burst.getAmount());
            }
        });
    }

    private void persist(@NonNullDecl Ref<EntityStore> ref,
                         @NonNullDecl StrikeComponent strike,
                         @NonNullDecl CommandBuffer<EntityStore> cmd) {
        cmd.run(s -> s.putComponent(ref, StrikeComponent.getComponentType(), strike));
    }
}
