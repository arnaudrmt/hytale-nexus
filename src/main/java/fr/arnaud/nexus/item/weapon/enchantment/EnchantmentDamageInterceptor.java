package fr.arnaud.nexus.item.weapon.enchantment;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.*;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.item.weapon.component.WeaponInstanceComponent;
import fr.arnaud.nexus.item.weapon.data.EnchantmentSlot;
import fr.arnaud.nexus.item.weapon.enchantment.event.NexusEnchantBus;
import fr.arnaud.nexus.item.weapon.enchantment.event.NexusEnchantEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class EnchantmentDamageInterceptor {

    private EnchantmentDamageInterceptor() {}

    // ── ON_HIT — applies damage multiplier then fires enchant procs ───────────

    /**
     * Intercepts outgoing damage from a player with a weapon.
     * Applies the weapon's total damage multiplier (base curve + DamageMultiplier
     * enchants) to the Damage object, then publishes ON_HIT to the enchant bus
     * for proc-based effects like Vampirism.
     */
    public static final class OnHitSystem extends DamageEventSystem {

        @Override
        @Nullable
        public SystemGroup<EntityStore> getGroup() {
            return DamageModule.get().getInspectDamageGroup();
        }

        @Override
        @Nonnull
        public Query<EntityStore> getQuery() {
            return WeaponInstanceComponent.getComponentType();
        }

        @Override
        public void handle(int index,
                           @Nonnull ArchetypeChunk<EntityStore> chunk,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull CommandBuffer<EntityStore> cmd,
                           @Nonnull Damage damage) {
            if (damage.isCancelled()) return;
            if (!(damage.getSource() instanceof Damage.EntitySource entitySource)) return;

            Ref<EntityStore> attackerRef = entitySource.getRef();
            if (!attackerRef.isValid()) return;

            WeaponInstanceComponent weapon = cmd.getComponent(
                attackerRef, WeaponInstanceComponent.getComponentType());
            if (weapon == null) return;

            // ── Apply damage multiplier ───────────────────────────────────────
            // Start from the weapon base curve, then stack enchant multipliers
            double totalMultiplier = weapon.damageMultiplierCurve;

            for (EnchantmentSlot slot : weapon.enchantmentSlots) {
                if (!slot.isUnlocked()) continue;
                EnchantmentDefinition def = EnchantmentRegistry.get().getDefinition(slot.chosen());
                if (def == null) continue;
                var statDef = def.getStat("DamageMultiplier");
                if (statDef == null) continue;
                // Curve: multiplier * current value
                totalMultiplier *= statDef.getValue(slot.currentLevel());
            }

            damage.setAmount((float) (damage.getAmount() * totalMultiplier));

            // ── Fire proc-based enchant events ────────────────────────────────
            Ref<EntityStore> targetRef = chunk.getReferenceTo(index);
            NexusEnchantBus.get().publish(new NexusEnchantEvent(
                NexusEnchantEvent.Type.ON_HIT,
                attackerRef, targetRef,
                damage.getAmount(), store,
                Nexus.get().getStatIndexResolver()));
        }
    }

    // ── ON_RECEIVE_HIT — player receives damage ───────────────────────────────

    /**
     * Fires ON_RECEIVE_HIT for the defender's enchants.
     * The entity in the chunk is the target. Only fires if they have a weapon
     * equipped (i.e. is a Nexus player).
     */
    public static final class OnReceiveHitSystem extends DamageEventSystem {

        @Override
        @Nullable
        public SystemGroup<EntityStore> getGroup() {
            return DamageModule.get().getInspectDamageGroup();
        }

        @Override
        @Nonnull
        public Query<EntityStore> getQuery() {
            return WeaponInstanceComponent.getComponentType();
        }

        @Override
        public void handle(int index,
                           @Nonnull ArchetypeChunk<EntityStore> chunk,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull CommandBuffer<EntityStore> cmd,
                           @Nonnull Damage damage) {
            if (damage.isCancelled()) return;

            Ref<EntityStore> defenderRef = chunk.getReferenceTo(index);
            if (cmd.getComponent(defenderRef, WeaponInstanceComponent.getComponentType()) == null)
                return;

            NexusEnchantBus.get().publish(new NexusEnchantEvent(
                NexusEnchantEvent.Type.ON_RECEIVE_HIT,
                defenderRef, defenderRef,
                damage.getAmount(), store,
                Nexus.get().getStatIndexResolver()));
        }
    }

    // ── ON_KILL — player kills an entity ─────────────────────────────────────

    /**
     * Fires ON_KILL for the killer's enchants via the death pipeline.
     */
    public static final class OnKillSystem extends DeathSystems.OnDeathSystem {

        @Override
        @Nonnull
        public Query<EntityStore> getQuery() {
            // Any entity can die; we filter by killer having a weapon inside
            return WeaponInstanceComponent.getComponentType();
        }

        @Override
        public void onComponentAdded(@Nonnull Ref<EntityStore> deadRef,
                                     @Nonnull DeathComponent deathComponent,
                                     @Nonnull Store<EntityStore> store,
                                     @Nonnull CommandBuffer<EntityStore> cmd) {
            Damage deathInfo = deathComponent.getDeathInfo();
            if (deathInfo == null) return;
            if (!(deathInfo.getSource() instanceof Damage.EntitySource entitySource)) return;

            Ref<EntityStore> killerRef = entitySource.getRef();
            if (!killerRef.isValid()) return;

            if (cmd.getComponent(killerRef, WeaponInstanceComponent.getComponentType()) == null)
                return;

            NexusEnchantBus.get().publish(new NexusEnchantEvent(
                NexusEnchantEvent.Type.ON_KILL,
                killerRef, deadRef,
                0, store,
                Nexus.get().getStatIndexResolver()));
        }
    }
}
