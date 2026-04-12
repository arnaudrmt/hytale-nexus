package fr.arnaud.nexus.item.weapon.enchantment.handlers;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap.Predictable;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.item.weapon.enchantment.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class BouncingProjectileEnchantmentHandler implements EnchantmentHandler {

    private static final float BOUNCE_SEARCH_RADIUS = 10.0f;

    @Override
    public void onActivate(Ref<EntityStore> playerRef, int level, Store<EntityStore> store, CommandBuffer<EntityStore> cmd) {
    }

    @Override
    public void onDeactivate(Ref<EntityStore> playerRef, int level, Store<EntityStore> store, CommandBuffer<EntityStore> cmd) {
    }

    public void execute(
        Ref<EntityStore> attackerRef,
        Ref<EntityStore> firstTargetRef,
        float originalDamage,
        int level,
        CommandBuffer<EntityStore> cmd
    ) {
        EnchantmentDefinition def = EnchantmentRegistry.get()
                                                       .getDefinition("Enchant_Bouncing_" + level);
        if (def == null) return;
        EnchantmentLevelData data = def.getDataForLevel(level);
        if (data == null) return;

        int bounces = (int) data.get("BounceCount", 1);
        float chance = data.get("BounceChance", 0.5f);
        float retention = data.get("DamageRetention", 0.7f);

        List<Ref<EntityStore>> alreadyHit = new ArrayList<>();
        alreadyHit.add(attackerRef);
        alreadyHit.add(firstTargetRef);

        Ref<EntityStore> currentSource = firstTargetRef;
        float currentDamage = originalDamage * retention;

        for (int bounce = 0; bounce < bounces; bounce++) {
            if (Math.random() > chance) break;
            Optional<Ref<EntityStore>> nextTarget =
                findNearestUnhit(currentSource, alreadyHit, cmd);
            if (nextTarget.isEmpty()) break;

            Ref<EntityStore> target = nextTarget.get();
            alreadyHit.add(target);
            applyFlatDamage(target, currentDamage, cmd);
            currentSource = target;
            currentDamage *= retention;
        }
    }

    private Optional<Ref<EntityStore>> findNearestUnhit(
        Ref<EntityStore> sourceRef,
        List<Ref<EntityStore>> alreadyHit,
        CommandBuffer<EntityStore> cmd
    ) {
        return EnchantmentSpatialUtil.getEntitiesInRadius(sourceRef, BOUNCE_SEARCH_RADIUS, cmd)
                                     .stream()
                                     .filter(ref -> alreadyHit.stream().noneMatch(hit -> EnchantmentSpatialUtil.isSameRef(hit, ref)))
                                     .min(Comparator.comparingDouble(ref ->
                                         EnchantmentSpatialUtil.distanceBetween(sourceRef, ref, cmd)));
    }

    private void applyFlatDamage(Ref<EntityStore> targetRef, float amount, CommandBuffer<EntityStore> cmd) {
        EntityStatMap stats = cmd.getComponent(targetRef, EntityStatMap.getComponentType());
        if (stats == null) return;
        stats.subtractStatValue(Predictable.ALL, TriggerStatRegistry.get().getHealthIndex(), amount);
        cmd.putComponent(targetRef, EntityStatMap.getComponentType(), stats);
    }
}
