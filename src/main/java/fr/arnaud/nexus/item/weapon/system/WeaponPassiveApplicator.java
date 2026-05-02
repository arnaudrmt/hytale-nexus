package fr.arnaud.nexus.item.weapon.system;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.feature.resource.PlayerStatsManager;
import fr.arnaud.nexus.item.weapon.component.WeaponInstanceComponent;
import fr.arnaud.nexus.item.weapon.data.EnchantmentSlot;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentDefinition;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentRegistry;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentStatDefinition;
import fr.arnaud.nexus.item.weapon.enchantment.impl.EnchantHealthBoost;
import fr.arnaud.nexus.item.weapon.enchantment.impl.EnchantStaminaBoost;
import fr.arnaud.nexus.item.weapon.enchantment.impl.EnchantSwiftness;

import javax.annotation.Nonnull;

/**
 * Applies and removes permanent passive stat bonuses (health, stamina, speed).
 */
public final class WeaponPassiveApplicator {

    private WeaponPassiveApplicator() {
    }

    public static void apply(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                             @Nonnull WeaponInstanceComponent weapon) {
        PlayerStatsManager psm = Nexus.getInstance().getPlayerStatsManager();
        if (!psm.isReady()) return;

        float totalHealth = (float) weapon.healthBonus;
        float totalSpeed = (float) weapon.movementSpeedBonus;
        float totalStamina = 0f;

        for (EnchantmentSlot slot : weapon.enchantmentSlots) {
            if (!slot.isUnlocked()) continue;
            EnchantmentDefinition def = EnchantmentRegistry.getInstance().getDefinition(slot.chosen());
            if (def == null) continue;
            int level = slot.currentLevel();

            for (EnchantmentStatDefinition statDef : def.stats()) {
                switch (statDef.getId()) {
                    case EnchantHealthBoost.STAT_HEALTH_BOOST ->
                        totalHealth += (float) statDef.getStatValueForLevel(level);
                    case EnchantStaminaBoost.STAT_STAMINA_BOOST ->
                        totalStamina += (float) statDef.getStatValueForLevel(level);
                    case EnchantSwiftness.STAT_SWIFTNESS_BOOST ->
                        totalSpeed += (float) statDef.getStatValueForLevel(level);
                }
            }
        }

        psm.setMaxHealthBonus(ref, store, totalHealth);
        psm.setMaxStaminaBonus(ref, store, totalStamina);
        psm.setMovementSpeedBonus(ref, store, totalSpeed);
    }

    public static void remove(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        PlayerStatsManager psm = Nexus.getInstance().getPlayerStatsManager();
        if (!psm.isReady()) return;

        psm.setMaxHealthBonus(ref, store, 0f);
        psm.setMaxStaminaBonus(ref, store, 0f);
        psm.setMovementSpeedBonus(ref, store, 0f);
    }
}
