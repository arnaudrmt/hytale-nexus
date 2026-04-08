package fr.arnaud.nexus.weapon.enchantment.handlers;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap.Predictable;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.weapon.enchantment.EnchantmentHandler;
import fr.arnaud.nexus.weapon.enchantment.TriggerStatRegistry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class BouncingProjectileEnchantmentHandler implements EnchantmentHandler {

    private static final float BOUNCE_SEARCH_RADIUS = 10.0f;
    private static final int[] BOUNCE_COUNT_BY_LEVEL = {0, 1, 2, 3};
    private static final float[] BOUNCE_RETENTION_BY_LEVEL = {0f, 0.70f, 0.75f, 0.80f};

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
        int bounces = BOUNCE_COUNT_BY_LEVEL[level];
        float retention = BOUNCE_RETENTION_BY_LEVEL[level];

        List<Ref<EntityStore>> alreadyHit = new ArrayList<>();
        alreadyHit.add(attackerRef);
        alreadyHit.add(firstTargetRef);

        Ref<EntityStore> currentSource = firstTargetRef;
        float currentDamage = originalDamage * retention;

        for (int bounce = 0; bounce < bounces; bounce++) {
            Optional<Ref<EntityStore>> nextTarget = findNearestUnhit(currentSource, alreadyHit, cmd);
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
