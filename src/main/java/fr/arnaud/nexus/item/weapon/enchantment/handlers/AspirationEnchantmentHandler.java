package fr.arnaud.nexus.item.weapon.enchantment.handlers;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentDefinition;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentHandler;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentLevelData;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentRegistry;

import java.util.List;

public final class AspirationEnchantmentHandler implements EnchantmentHandler {

    @Override
    public void onActivate(Ref<EntityStore> playerRef, int level, Store<EntityStore> store, CommandBuffer<EntityStore> cmd) {
    }

    @Override
    public void onDeactivate(Ref<EntityStore> playerRef, int level, Store<EntityStore> store, CommandBuffer<EntityStore> cmd) {
    }

    public void execute(Ref<EntityStore> attackerRef, int level, CommandBuffer<EntityStore> cmd) {
        EnchantmentDefinition def = EnchantmentRegistry.get().getDefinition("Enchant_Aspiration_" + level);
        if (def == null) return;
        EnchantmentLevelData data = def.getDataForLevel(level);
        if (data == null) return;

        float radius = data.get("PullRadius", 4.0f);
        float force = data.get("PullForce", 4.0f);

        List<Ref<EntityStore>> nearby = EnchantmentSpatialUtil.getEntitiesInRadius(attackerRef, radius, cmd);
        for (Ref<EntityStore> nearbyRef : nearby) {
            if (EnchantmentSpatialUtil.isSameRef(nearbyRef, attackerRef)) continue;
            EnchantmentSpatialUtil.applyDirectionalImpulse(nearbyRef, attackerRef, force, cmd);
        }
    }
}
