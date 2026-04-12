package fr.arnaud.nexus.item.weapon.enchantment.handlers;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap.Predictable;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.item.weapon.enchantment.*;

import java.util.List;

public final class PiercingEnchantmentHandler implements EnchantmentHandler {

    private static final float PIERCE_SEARCH_RADIUS = 8.0f;

    @Override
    public void onActivate(Ref<EntityStore> playerRef, int level, Store<EntityStore> store, CommandBuffer<EntityStore> cmd) {
    }

    @Override
    public void onDeactivate(Ref<EntityStore> playerRef, int level, Store<EntityStore> store, CommandBuffer<EntityStore> cmd) {
    }

    public void execute(
        Ref<EntityStore> attackerRef,
        Ref<EntityStore> primaryTargetRef,
        float originalDamage,
        int level,
        CommandBuffer<EntityStore> cmd
    ) {
        EnchantmentDefinition def = EnchantmentRegistry.get().getDefinition("Enchant_Piercing_" + level);
        if (def == null) return;
        EnchantmentLevelData data = def.getDataForLevel(level);
        if (data == null) return;

        float retention = data.get("DamageRetention", 0.5f);
        int remaining = (int) data.get("MaxTargets", 1);

        TransformComponent attackerTransform = cmd.getComponent(attackerRef, TransformComponent.getComponentType());
        TransformComponent targetTransform = cmd.getComponent(primaryTargetRef, TransformComponent.getComponentType());
        if (attackerTransform == null || targetTransform == null) return;

        Vector3d shotDirection = targetTransform.getPosition().clone();
        shotDirection.subtract(attackerTransform.getPosition());
        shotDirection.normalize();

        List<Ref<EntityStore>> candidates = EnchantmentSpatialUtil.getEntitiesInRadius(primaryTargetRef, PIERCE_SEARCH_RADIUS, cmd);

        for (Ref<EntityStore> candidate : candidates) {
            if (remaining <= 0) break;
            if (EnchantmentSpatialUtil.isSameRef(candidate, attackerRef)) continue;
            if (EnchantmentSpatialUtil.isSameRef(candidate, primaryTargetRef)) continue;
            if (!isBehindTarget(targetTransform.getPosition(), shotDirection, candidate, cmd)) continue;

            applyFlatDamage(candidate, originalDamage * retention, cmd);
            remaining--;
        }
    }

    private boolean isBehindTarget(Vector3d targetPos, Vector3d shotDirection, Ref<EntityStore> candidateRef, CommandBuffer<EntityStore> cmd) {
        TransformComponent t = cmd.getComponent(candidateRef, TransformComponent.getComponentType());
        if (t == null) return false;
        Vector3d toCandidate = t.getPosition().clone();
        toCandidate.subtract(targetPos);
        return toCandidate.dot(shotDirection) > 0;
    }

    private void applyFlatDamage(Ref<EntityStore> targetRef, float amount, CommandBuffer<EntityStore> cmd) {
        EntityStatMap stats = cmd.getComponent(targetRef, EntityStatMap.getComponentType());
        if (stats == null) return;
        stats.subtractStatValue(Predictable.ALL, TriggerStatRegistry.get().getHealthIndex(), amount);
        cmd.putComponent(targetRef, EntityStatMap.getComponentType(), stats);
    }
}
