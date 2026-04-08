package fr.arnaud.nexus.weapon.enchantment.handlers;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap.Predictable;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.weapon.enchantment.EnchantmentHandler;
import fr.arnaud.nexus.weapon.enchantment.TriggerStatRegistry;

import java.util.List;

public final class ShockwaveEnchantmentHandler implements EnchantmentHandler {

    private static final float[] SHOCKWAVE_RADIUS_BY_LEVEL = {0f, 4.0f, 5.5f, 7.0f};
    private static final float[] SHOCKWAVE_DAMAGE_BY_LEVEL = {0f, 8.0f, 14.0f, 22.0f};
    private static final float[] SHOCKWAVE_FORCE_BY_LEVEL = {0f, 5.0f, 7.0f, 10.0f};

    @Override
    public void onActivate(Ref<EntityStore> playerRef, int level, Store<EntityStore> store, CommandBuffer<EntityStore> cmd) {
    }

    @Override
    public void onDeactivate(Ref<EntityStore> playerRef, int level, Store<EntityStore> store, CommandBuffer<EntityStore> cmd) {
    }

    public void execute(Ref<EntityStore> attackerRef, int level, CommandBuffer<EntityStore> cmd) {
        float radius = SHOCKWAVE_RADIUS_BY_LEVEL[level];
        float damage = SHOCKWAVE_DAMAGE_BY_LEVEL[level];
        float force = SHOCKWAVE_FORCE_BY_LEVEL[level];

        List<Ref<EntityStore>> nearby = EnchantmentSpatialUtil.getEntitiesInRadius(attackerRef, radius, cmd);
        for (Ref<EntityStore> nearbyRef : nearby) {
            if (EnchantmentSpatialUtil.isSameRef(nearbyRef, attackerRef)) continue;
            applyFlatDamage(nearbyRef, damage, cmd);
            EnchantmentSpatialUtil.applyRadialImpulse(attackerRef, nearbyRef, force, cmd);
        }
    }

    private void applyFlatDamage(Ref<EntityStore> targetRef, float amount, CommandBuffer<EntityStore> cmd) {
        EntityStatMap stats = cmd.getComponent(targetRef, EntityStatMap.getComponentType());
        if (stats == null) return;
        stats.subtractStatValue(Predictable.ALL, TriggerStatRegistry.get().getHealthIndex(), amount);
        cmd.putComponent(targetRef, EntityStatMap.getComponentType(), stats);
    }
}
