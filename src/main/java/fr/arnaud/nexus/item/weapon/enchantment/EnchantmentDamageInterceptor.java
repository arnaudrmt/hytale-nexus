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
import fr.arnaud.nexus.item.weapon.enchantment.impl.EnchantGambler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ThreadLocalRandom;

public final class EnchantmentDamageInterceptor {

    private EnchantmentDamageInterceptor() {
    }

    // ── ON_HIT ────────────────────────────────────────────────────────────────

    public static final class OnHitSystem extends DamageEventSystem {

        @Override
        @Nullable
        public SystemGroup<EntityStore> getGroup() {
            return DamageModule.get().getFilterDamageGroup();
        }

        @Override
        @Nonnull
        public Query<EntityStore> getQuery() {
            return Query.any();
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

            // Base damage multiplier + sharpness
            double totalMultiplier = weapon.damageMultiplierCurve;
            for (EnchantmentSlot slot : weapon.enchantmentSlots) {
                if (!slot.isUnlocked()) continue;
                EnchantmentDefinition def = EnchantmentRegistry.get().getDefinition(slot.chosen());
                if (def == null) continue;

                // Sharpness — additive damage delta
                var dmgStat = def.getStat("DamageMultiplier");
                if (dmgStat != null) {
                    totalMultiplier += dmgStat.getValue(slot.currentLevel()) - 1.0;
                }

                // Gambler — modifies multiplier here while we still own the Damage object
                if ("Enchant_Gambler".equals(slot.chosen())) {
                    totalMultiplier = applyGamblerRoll(def, slot.currentLevel(), totalMultiplier);
                }

                // Critical Strike — doubles multiplier on crit
                if ("Enchant_CriticalStrike".equals(slot.chosen())) {
                    var critStat = def.getStat("CritChance");
                    if (critStat != null) {
                        float critChance = (float) critStat.getValue(slot.currentLevel());
                        if (ThreadLocalRandom.current().nextFloat() < critChance) {
                            totalMultiplier *= 2.0;
                        }
                    }
                }
            }

            damage.setAmount((float) (damage.getAmount() * totalMultiplier));

            // Publish ON_HIT for proc enchants (Fire, Freeze, Poison, Cleave, Vampirism etc.)
            Ref<EntityStore> targetRef = chunk.getReferenceTo(index);
            NexusEnchantBus.get().publish(new NexusEnchantEvent(
                NexusEnchantEvent.Type.ON_HIT,
                attackerRef, targetRef,
                damage.getAmount(), store, cmd,
                Nexus.get().getStatIndexResolver()));
        }

        private static double applyGamblerRoll(EnchantmentDefinition def,
                                               int level,
                                               double currentMultiplier) {
            var doubleStat = def.getStat(EnchantGambler.STAT_DOUBLE);
            var halveStat = def.getStat(EnchantGambler.STAT_HALVE);
            if (doubleStat == null || halveStat == null) return currentMultiplier;

            float doubleChance = (float) doubleStat.getValue(level);
            float halveChance = (float) halveStat.getValue(level);
            float roll = ThreadLocalRandom.current().nextFloat();

            if (roll < doubleChance) return currentMultiplier * 2.0;
            if (roll < doubleChance + halveChance) return currentMultiplier * 0.5;
            return currentMultiplier;
        }
    }

    // ── ON_RECEIVE_HIT ────────────────────────────────────────────────────────

    public static final class OnReceiveHitSystem extends DamageEventSystem {

        @Override
        @Nullable
        public SystemGroup<EntityStore> getGroup() {
            return DamageModule.get().getFilterDamageGroup();
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
            WeaponInstanceComponent weapon = cmd.getComponent(
                defenderRef, WeaponInstanceComponent.getComponentType());
            if (weapon == null) return;

            // Resilience — reduce damage directly on the Damage object
            for (EnchantmentSlot slot : weapon.enchantmentSlots) {
                if (!slot.isUnlocked()) continue;
                if (!"Enchant_Resilience".equals(slot.chosen())) continue;
                EnchantmentDefinition def = EnchantmentRegistry.get().getDefinition(slot.chosen());
                if (def == null) continue;
                var stat = def.getStat("DamageReduction");
                if (stat == null) continue;
                float reduction = (float) stat.getValue(slot.currentLevel());
                damage.setAmount(damage.getAmount() * (1f - reduction));
            }

            // Publish ON_RECEIVE_HIT for other defensive enchants
            NexusEnchantBus.get().publish(new NexusEnchantEvent(
                NexusEnchantEvent.Type.ON_RECEIVE_HIT,
                defenderRef, defenderRef,
                damage.getAmount(), store, cmd,
                Nexus.get().getStatIndexResolver()));
        }
    }

    // ── ON_KILL ───────────────────────────────────────────────────────────────

    public static final class OnKillSystem extends DeathSystems.OnDeathSystem {

        @Override
        @Nonnull
        public Query<EntityStore> getQuery() {
            return Query.any();
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
                0, store, cmd,
                Nexus.get().getStatIndexResolver()));
        }
    }
}
