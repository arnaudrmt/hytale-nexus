package fr.arnaud.nexus.item.weapon.enchantment.impl;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentDefinition;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentRegistry;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentStatDefinition;
import fr.arnaud.nexus.item.weapon.enchantment.event.EnchantEffectHandler;
import fr.arnaud.nexus.item.weapon.enchantment.event.NexusEnchantEvent;
import fr.arnaud.nexus.item.weapon.enchantment.handlers.EnchantmentSpatialUtil;

import java.util.List;

public final class EnchantCleave implements EnchantEffectHandler {

    public static final EnchantCleave INSTANCE = new EnchantCleave();
    private static final String ENCHANT_ID = "Enchant_Cleave";

    private EnchantCleave() {
    }

    @Override
    public void onHit(NexusEnchantEvent event, int enchantLevel) {
        EnchantmentDefinition def = EnchantmentRegistry.get().getDefinition(ENCHANT_ID);
        if (def == null) return;

        EnchantmentStatDefinition radiusStat = def.getStat("CleaveRadius");
        EnchantmentStatDefinition damageStat = def.getStat("CleaveDamagePercent");
        if (radiusStat == null || damageStat == null) return;

        float radius = (float) radiusStat.getValue(enchantLevel);
        float damagePercent = (float) damageStat.getValue(enchantLevel);
        float cleaveDamage = (float) (event.damage() * damagePercent);

        List<Ref<EntityStore>> nearby = EnchantmentSpatialUtil.getEntitiesInRadius(
            event.attacker(), radius, event.cmd());

        World world = event.store().getExternalData().getWorld();
        DamageCause cause = DamageCause.getAssetMap().getAsset("Physical");
        Ref<EntityStore> attacker = event.attacker();
        Ref<EntityStore> originalTarget = event.target();

        for (Ref<EntityStore> nearbyRef : nearby) {
            if (EnchantmentSpatialUtil.isSameRef(nearbyRef, attacker)) continue;
            if (EnchantmentSpatialUtil.isSameRef(nearbyRef, originalTarget)) continue;
            if (!nearbyRef.isValid()) continue;

            final Ref<EntityStore> capturedRef = nearbyRef;

            // Defer to next world tick so we're outside the current store processing cycle
            world.execute(() -> {
                if (!capturedRef.isValid()) return;
                event.store().invoke(capturedRef,
                    new Damage(new Damage.EntitySource(attacker), cause, cleaveDamage));
            });
        }
    }
}
