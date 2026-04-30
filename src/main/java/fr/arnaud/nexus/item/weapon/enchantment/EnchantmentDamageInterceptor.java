package fr.arnaud.nexus.item.weapon.enchantment;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.*;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.item.weapon.component.WeaponInstanceComponent;
import fr.arnaud.nexus.item.weapon.data.EnchantmentSlot;
import fr.arnaud.nexus.item.weapon.enchantment.event.NexusEnchantBus;
import fr.arnaud.nexus.item.weapon.enchantment.event.NexusEnchantEvent;
import fr.arnaud.nexus.item.weapon.enchantment.impl.EnchantBloodlust;
import fr.arnaud.nexus.item.weapon.enchantment.impl.EnchantGambler;
import fr.arnaud.nexus.session.RunSessionComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ThreadLocalRandom;

public final class EnchantmentDamageInterceptor {

    private EnchantmentDamageInterceptor() {
    }

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

            double totalMultiplier = weapon.damageMultiplierCurve;

            for (EnchantmentSlot slot : weapon.enchantmentSlots) {
                if (!slot.isUnlocked()) continue;
                EnchantmentDefinition def = EnchantmentRegistry.get().getDefinition(slot.chosen());
                if (def == null) continue;
                int level = slot.currentLevel();

                var dmgStat = def.getEnchantmentStatById("DamageMultiplier");
                if (dmgStat != null) {
                    totalMultiplier += dmgStat.getStatValueForLevel(level) - 1.0;
                }

                if ("Enchant_Gambler".equals(slot.chosen())) {
                    totalMultiplier = applyGamblerRoll(def, level, totalMultiplier);
                }

                if ("Enchant_CriticalStrike".equals(slot.chosen())) {
                    var critStat = def.getEnchantmentStatById("CritChance");
                    if (critStat != null && ThreadLocalRandom.current().nextFloat()
                        < (float) critStat.getStatValueForLevel(level)) {
                        totalMultiplier *= 2.0;
                    }
                }

                if ("Enchant_Bloodlust".equals(slot.chosen())) {
                    totalMultiplier += EnchantBloodlust.getBonusMultiplier(attackerRef, level);
                }
            }

            float damageToDeal = (float) (damage.getAmount() * totalMultiplier);

            damage.setAmount(damageToDeal);

            RunSessionComponent session = store.getComponent(attackerRef, RunSessionComponent.getComponentType());
            if (session != null) {
                session.addDamageDealt(damageToDeal);
            }

            Ref<EntityStore> targetRef = chunk.getReferenceTo(index);
            NexusEnchantBus.get().publish(new NexusEnchantEvent(
                NexusEnchantEvent.Type.ON_HIT,
                attackerRef, targetRef,
                damage.getAmount(), store, cmd));
        }

        private static double applyGamblerRoll(EnchantmentDefinition def,
                                               int level, double current) {
            var doubleStat = def.getEnchantmentStatById(EnchantGambler.STAT_DOUBLE);
            var halveStat = def.getEnchantmentStatById(EnchantGambler.STAT_HALVE);
            if (doubleStat == null || halveStat == null) return current;
            float dc = (float) doubleStat.getStatValueForLevel(level);
            float hc = (float) halveStat.getStatValueForLevel(level);
            float roll = ThreadLocalRandom.current().nextFloat();
            if (roll < dc) return current * 2.0;
            if (roll < dc + hc) return current * 0.5;
            return current;
        }
    }

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

            for (EnchantmentSlot slot : weapon.enchantmentSlots) {
                if (!slot.isUnlocked()) continue;
                EnchantmentDefinition def = EnchantmentRegistry.get().getDefinition(slot.chosen());
                if (def == null) continue;
                int level = slot.currentLevel();

                switch (slot.chosen()) {

                    case "Enchant_Resilience" -> {
                        var stat = def.getEnchantmentStatById("DamageReduction");
                        if (stat != null) {
                            float reduction = (float) stat.getStatValueForLevel(level);
                            damage.setAmount(damage.getAmount() * (1f - reduction));
                        }
                    }

                    case "Enchant_Warding" -> {
                        var pctStat = def.getEnchantmentStatById("WardingPercent");
                        var capStat = def.getEnchantmentStatById("WardingCap");
                        if (pctStat != null && capStat != null) {
                            float pct = (float) pctStat.getStatValueForLevel(level);
                            float cap = (float) capStat.getStatValueForLevel(level);
                            float reduction = Math.min(damage.getAmount() * pct, cap);
                            damage.setAmount(Math.max(0f, damage.getAmount() - reduction));
                        }
                    }

                    case "Enchant_Fortitude" -> {
                        var stat = def.getEnchantmentStatById("FortitudeChance");
                        if (stat != null) {
                            float chance = (float) stat.getStatValueForLevel(level);
                            if (ThreadLocalRandom.current().nextFloat() < chance) {
                                damage.setAmount(0f);
                                damage.setCancelled(true);
                                return;
                            }
                        }
                    }

                    case "Enchant_Thorns" -> {
                        var stat = def.getEnchantmentStatById("ThornsPercent");
                        if (stat != null
                            && damage.getSource() instanceof Damage.EntitySource src
                            && src.getRef().isValid()) {
                            float reflected = damage.getAmount() * (float) stat.getStatValueForLevel(level);
                            Ref<EntityStore> attackerRef = src.getRef();
                            DamageCause cause = DamageCause.getAssetMap().getAsset("Physical");
                            World world = store.getExternalData().getWorld();
                            world.execute(() -> {
                                if (!attackerRef.isValid()) return;
                                store.invoke(attackerRef,
                                    new Damage(new Damage.EntitySource(defenderRef),
                                        cause, reflected));
                            });
                        }
                    }
                }
            }

            NexusEnchantBus.get().publish(new NexusEnchantEvent(
                NexusEnchantEvent.Type.ON_RECEIVE_HIT,
                defenderRef, defenderRef,
                damage.getAmount(), store, cmd));
        }
    }

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
                0, store, cmd));
        }
    }
}
