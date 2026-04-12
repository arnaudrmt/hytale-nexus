package fr.arnaud.nexus.item.weapon.enchantment.handlers;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap.Predictable;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.item.weapon.enchantment.*;

import java.util.List;

public final class CleaveEnchantmentHandler implements EnchantmentHandler {

    @Override
    public void onActivate(Ref<EntityStore> playerRef, int level, Store<EntityStore> store, CommandBuffer<EntityStore> cmd) {
    }

    @Override
    public void onDeactivate(Ref<EntityStore> playerRef, int level, Store<EntityStore> store, CommandBuffer<EntityStore> cmd) {
    }

    public void execute(Ref<EntityStore> attackerRef, int level, CommandBuffer<EntityStore> cmd) {
        EnchantmentDefinition def = EnchantmentRegistry.get().getDefinition("Enchant_Cleave_" + level);
        if (def == null) return;
        EnchantmentLevelData data = def.getDataForLevel(level);
        if (data == null) return;

        float radius = data.get("Radius", 3.0f);
        float damageFraction = data.get("DamageFraction", 0.4f);
        float attackerDamage = resolveAttackerDamage(attackerRef, cmd);
        float cleaveDamage = attackerDamage * damageFraction;

        List<Ref<EntityStore>> nearby = EnchantmentSpatialUtil.getEntitiesInRadius(attackerRef, radius, cmd);
        for (Ref<EntityStore> nearbyRef : nearby) {
            if (EnchantmentSpatialUtil.isSameRef(nearbyRef, attackerRef)) continue;
            applyFlatDamage(nearbyRef, cleaveDamage, cmd);
        }
    }

    private float resolveAttackerDamage(Ref<EntityStore> attackerRef, CommandBuffer<EntityStore> cmd) {
        EntityStatMap stats = cmd.getComponent(attackerRef, EntityStatMap.getComponentType());
        if (stats == null) return 0f;
        EntityStatValue dmg = stats.get(TriggerStatRegistry.get().getWeaponDamageScaleIndex());
        return dmg != null ? dmg.get() : 0f;
    }

    private void applyFlatDamage(Ref<EntityStore> targetRef, float amount, CommandBuffer<EntityStore> cmd) {
        EntityStatMap stats = cmd.getComponent(targetRef, EntityStatMap.getComponentType());
        if (stats == null) return;
        stats.subtractStatValue(Predictable.ALL, TriggerStatRegistry.get().getHealthIndex(), amount);
        cmd.putComponent(targetRef, EntityStatMap.getComponentType(), stats);
    }
}
