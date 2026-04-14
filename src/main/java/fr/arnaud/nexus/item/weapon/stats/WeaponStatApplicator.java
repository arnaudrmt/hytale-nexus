package fr.arnaud.nexus.item.weapon.stats;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.item.weapon.system.StatIndexResolver;

public final class WeaponStatApplicator {

    private static final String MODIFIER_KEY_DAMAGE = "nexus_weapon_damage_multiplier";
    private static final String MODIFIER_KEY_HEALTH = "nexus_weapon_health_boost";
    private static final String MODIFIER_KEY_MOVEMENT = "nexus_weapon_movement_speed";

    private final StatIndexResolver statResolver;

    public WeaponStatApplicator(StatIndexResolver statResolver) {
        this.statResolver = statResolver;
    }

    public void apply(Ref<EntityStore> playerRef, WeaponStatBag bag, Store<EntityStore> store) {
        if (!statResolver.isReady() || !playerRef.isValid()) return;

        EntityStatMap stats = store.getComponent(playerRef, EntityStatMap.getComponentType());
        if (stats == null) return;

        stats.putModifier(
            EntityStatMap.Predictable.NONE,
            statResolver.getWeaponDamageIndex(),
            MODIFIER_KEY_DAMAGE,
            new StaticModifier(Modifier.ModifierTarget.MAX, StaticModifier.CalculationType.MULTIPLICATIVE, (float) bag.damageMultiplier)
        );

        stats.putModifier(
            EntityStatMap.Predictable.NONE,
            statResolver.getHealthIndex(),
            MODIFIER_KEY_HEALTH,
            new StaticModifier(Modifier.ModifierTarget.MAX, StaticModifier.CalculationType.ADDITIVE, (float) bag.healthBoost)
        );

        stats.putModifier(
            EntityStatMap.Predictable.NONE,
            statResolver.getMovementSpeedIndex(),
            MODIFIER_KEY_MOVEMENT,
            new StaticModifier(Modifier.ModifierTarget.MAX, StaticModifier.CalculationType.ADDITIVE, (float) bag.movementSpeedBoost)
        );
    }

    public void remove(Ref<EntityStore> playerRef, Store<EntityStore> store) {
        if (!statResolver.isReady() || !playerRef.isValid()) return;

        EntityStatMap stats = store.getComponent(playerRef, EntityStatMap.getComponentType());
        if (stats == null) return;

        stats.removeModifier(EntityStatMap.Predictable.NONE, statResolver.getWeaponDamageIndex(), MODIFIER_KEY_DAMAGE);
        stats.removeModifier(EntityStatMap.Predictable.NONE, statResolver.getHealthIndex(), MODIFIER_KEY_HEALTH);
        stats.removeModifier(EntityStatMap.Predictable.NONE, statResolver.getMovementSpeedIndex(), MODIFIER_KEY_MOVEMENT);
    }
}
