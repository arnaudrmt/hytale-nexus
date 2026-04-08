package fr.arnaud.nexus.weapon.enchantment.handlers;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.weapon.enchantment.EnchantmentHandler;

import java.util.List;

public final class AspirationEnchantmentHandler implements EnchantmentHandler {

    private static final float[] PULL_RADIUS_BY_LEVEL = {0f, 4.0f, 6.0f, 8.0f};
    private static final float[] PULL_FORCE_BY_LEVEL = {0f, 4.0f, 6.0f, 9.0f};

    @Override
    public void onActivate(Ref<EntityStore> playerRef, int level, Store<EntityStore> store, CommandBuffer<EntityStore> cmd) {
    }

    @Override
    public void onDeactivate(Ref<EntityStore> playerRef, int level, Store<EntityStore> store, CommandBuffer<EntityStore> cmd) {
    }

    public void execute(Ref<EntityStore> attackerRef, int level, CommandBuffer<EntityStore> cmd) {
        float radius = PULL_RADIUS_BY_LEVEL[level];
        float force = PULL_FORCE_BY_LEVEL[level];

        List<Ref<EntityStore>> nearby = EnchantmentSpatialUtil.getEntitiesInRadius(attackerRef, radius, cmd);
        for (Ref<EntityStore> nearbyRef : nearby) {
            if (EnchantmentSpatialUtil.isSameRef(nearbyRef, attackerRef)) continue;
            EnchantmentSpatialUtil.applyDirectionalImpulse(nearbyRef, attackerRef, force, cmd);
        }
    }
}
