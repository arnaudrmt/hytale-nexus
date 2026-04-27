package fr.arnaud.nexus.item.weapon.system;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.feature.ressource.PlayerStatsManager;
import fr.arnaud.nexus.item.weapon.component.WeaponInstanceComponent;
import fr.arnaud.nexus.item.weapon.data.EnchantmentSlot;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentDefinition;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentRegistry;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentStatDefinition;

import javax.annotation.Nonnull;

/**
 * Applies and removes permanent passive stat bonuses (health, stamina, speed)
 * granted by a weapon's base curves and its unlocked enchantments.
 * <p>
 * All three stats use named modifier / set patterns so calling apply() repeatedly
 * always replaces the previous value rather than stacking.
 * <p>
 * Damage is NOT handled here — applied per-hit in OnHitSystem.
 */
public final class WeaponPassiveApplicator {

    private WeaponPassiveApplicator() {
    }

    public static void apply(@Nonnull Ref<EntityStore> ref,
                             @Nonnull Store<EntityStore> store,
                             @Nonnull WeaponInstanceComponent weapon) {
        PlayerStatsManager psm = Nexus.get().getPlayerStatsManager();
        if (!psm.isReady()) return;

        float totalHealth = (float) weapon.healthBoostCurve;
        float totalSpeed = (float) weapon.movementSpeedCurve;
        float totalStamina = 0f;

        for (EnchantmentSlot slot : weapon.enchantmentSlots) {
            if (!slot.isUnlocked()) continue;
            EnchantmentDefinition def = EnchantmentRegistry.get().getDefinition(slot.chosen());
            if (def == null) continue;
            int level = slot.currentLevel();

            for (EnchantmentStatDefinition statDef : def.getStats()) {
                switch (statDef.getId()) {
                    case "HealthBoost" -> totalHealth += (float) statDef.getValue(level);
                    case "StaminaBoost" -> totalStamina += (float) statDef.getValue(level);
                    case "Swiftness" -> totalSpeed += (float) statDef.getValue(level);
                }
            }
        }

        psm.setMaxHealthBonus(ref, store, totalHealth);
        psm.setMaxStaminaBonus(ref, store, totalStamina);
        psm.setMovementSpeedBonus(ref, store, totalSpeed);
    }

    public static void remove(@Nonnull Ref<EntityStore> ref,
                              @Nonnull Store<EntityStore> store,
                              @Nonnull WeaponInstanceComponent weapon) {
        PlayerStatsManager psm = Nexus.get().getPlayerStatsManager();
        if (!psm.isReady()) return;

        psm.setMaxHealthBonus(ref, store, 0f);
        psm.setMaxStaminaBonus(ref, store, 0f);
        psm.setMovementSpeedBonus(ref, store, 0f);
    }
}
