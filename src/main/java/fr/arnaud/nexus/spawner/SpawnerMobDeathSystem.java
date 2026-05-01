package fr.arnaud.nexus.spawner;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.feature.resource.PlayerStatsManager;
import fr.arnaud.nexus.item.weapon.component.WeaponInstanceComponent;
import fr.arnaud.nexus.item.weapon.data.EnchantmentSlot;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentDefinition;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentRegistry;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentStatDefinition;
import fr.arnaud.nexus.session.RunSessionComponent;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;

public class SpawnerMobDeathSystem extends DeathSystems.OnDeathSystem {

    @Override
    public Query<EntityStore> getQuery() {
        return SpawnerTagComponent.getComponentType();
    }

    @Override
    public void onComponentAdded(@NotNull Ref<EntityStore> ref, @NotNull DeathComponent component,
                                 Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> commandBuffer) {
        SpawnerTagComponent tag = store.getComponent(ref, SpawnerTagComponent.getComponentType());
        if (tag == null) return;

        Nexus.getInstance().getMobSpawnerManager().onMobDied(tag.getSpawnerId());

        Damage deathInfo = component.getDeathInfo();
        if (deathInfo == null) return;

        Damage.Source source = deathInfo.getSource();
        if (!(source instanceof Damage.EntitySource entitySource)) return;

        Ref<EntityStore> killerRef = entitySource.getRef();
        if (!killerRef.isValid()) return;

        int essence = tag.getMinEssence() == tag.getMaxEssence()
            ? tag.getMinEssence()
            : tag.getMinEssence() + ThreadLocalRandom.current()
                                                     .nextInt(tag.getMaxEssence() - tag.getMinEssence() + 1);

        PlayerStatsManager psm = Nexus.getInstance().getPlayerStatsManager();
        psm.addEssenceDust(killerRef, store, essence);

        float soulHarvestBonus = getSoulHarvestBonus(killerRef, store, essence);
        if (soulHarvestBonus > 0f) {
            psm.addEssenceDust(killerRef, store, soulHarvestBonus);
        }

        RunSessionComponent session = store.getComponent(killerRef, RunSessionComponent.getComponentType());
        if (session != null) {
            session.incrementKillCount();
        }
    }

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

            EnchantmentStatDefinition stat = def.getEnchantmentStatById("SoulHarvestMultiplier");
            if (stat == null) return 0f;

            float multiplier = (float) stat.getStatValueForLevel(slot.currentLevel());
            return baseEssence * multiplier;
        }
        return 0f;
    }
}
