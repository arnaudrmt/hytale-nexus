package fr.arnaud.nexus.spawner;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.asset.type.gameplay.DeathConfig;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.feature.ressource.PlayerStatsManager;
import fr.arnaud.nexus.item.weapon.component.WeaponInstanceComponent;
import fr.arnaud.nexus.item.weapon.data.EnchantmentSlot;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentDefinition;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentRegistry;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentStatDefinition;

import java.util.concurrent.ThreadLocalRandom;

public class SpawnerMobDeathSystem extends DeathSystems.OnDeathSystem {

    // Only trigger on entities that have BOTH a SpawnerTag and a DeathComponent
    private final Query<EntityStore> query = Query.and(SpawnerTagComponent.getComponentType());

    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Override
    public void onComponentAdded(Ref<EntityStore> ref, DeathComponent component,
                                 Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {
        SpawnerTagComponent tag = store.getComponent(ref, SpawnerTagComponent.getComponentType());
        if (tag == null) return;

        component.setItemsLossMode(DeathConfig.ItemsLossMode.NONE);
        Nexus.get().getMobSpawnerManager().onMobDied(tag.getSpawnerId());

        Damage deathInfo = component.getDeathInfo();
        if (deathInfo == null) return;

        Damage.Source source = deathInfo.getSource();
        if (!(source instanceof Damage.EntitySource entitySource)) return;

        Ref<EntityStore> killerRef = entitySource.getRef();
        if (!killerRef.isValid()) return;

        // ProjectileSource.getRef() already returns the shooter, so no extra handling needed
        int essence = tag.getMinEssence() == tag.getMaxEssence()
            ? tag.getMinEssence()
            : tag.getMinEssence() + ThreadLocalRandom.current()
                                                     .nextInt(tag.getMaxEssence() - tag.getMinEssence() + 1);

        PlayerStatsManager psm = Nexus.get().getPlayerStatsManager();
        psm.addEssenceDust(killerRef, store, essence);

        // Soul Harvest — add bonus essence based on enchant multiplier
        float soulHarvestBonus = getSoulHarvestBonus(killerRef, store, essence);
        if (soulHarvestBonus > 0f) {
            psm.addEssenceDust(killerRef, store, soulHarvestBonus);
        }
    }

    /**
     * Returns the Soul Harvest essence bonus for the killer.
     * Bonus = baseEssence * multiplierAtLevel
     * e.g. level 1 (x0.50): base 10 → bonus 5 → total 15
     */
    private float getSoulHarvestBonus(Ref<EntityStore> killerRef,
                                      Store<EntityStore> store,
                                      int baseEssence) {
        WeaponInstanceComponent weapon = store.getComponent(
            killerRef, WeaponInstanceComponent.getComponentType());
        if (weapon == null) return 0f;

        for (EnchantmentSlot slot : weapon.enchantmentSlots) {
            if (!slot.isUnlocked()) continue;
            if (!"Enchant_SoulHarvest".equals(slot.chosen())) continue;

            EnchantmentDefinition def = EnchantmentRegistry.get()
                                                           .getDefinition("Enchant_SoulHarvest");
            if (def == null) return 0f;

            EnchantmentStatDefinition stat = def.getStat("SoulHarvestMultiplier");
            if (stat == null) return 0f;

            float multiplier = (float) stat.getValue(slot.currentLevel());
            return baseEssence * multiplier;
        }
        return 0f;
    }
}
