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
 * Applies and removes permanent passive stat bonuses (health and speed)
 * granted by a weapon's base curves and its unlocked enchantments.
 * <p>
 * Damage is NOT handled here — it is applied per-hit in
 * {@link fr.arnaud.nexus.item.weapon.enchantment.EnchantmentDamageInterceptor.OnHitSystem}
 * by multiplying the incoming Damage amount directly before distribution.
 * <p>
 * Called from {@link WeaponEquipSystem} on equip and unequip.
 */
public final class WeaponPassiveApplicator {

    private WeaponPassiveApplicator() {
    }

    // ── Apply ─────────────────────────────────────────────────────────────────

    /**
     * Computes the total health and speed bonuses from the weapon base curves
     * plus all unlocked enchant passive stats, then applies them via
     * {@link PlayerStatsManager}. Safe to call repeatedly — named modifiers
     * replace rather than stack.
     */
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
                    // Damage and proc stats handled at runtime — not passive
                }
            }
        }

        // setMaxHealthBonus uses a named modifier so re-equipping always replaces, never stacks
        psm.setMaxHealthBonus(ref, store, totalHealth);
        psm.setMaxStaminaBonus(ref, store, totalStamina);
        // Base speed is tracked separately — add the total bonus on top
        psm.addMovementSpeed(ref, store, totalSpeed);
    }

    // ── Remove ────────────────────────────────────────────────────────────────

    /**
     * Removes all passive stat bonuses applied by this weapon.
     * Must be called before equipping a new weapon.
     */
    public static void remove(@Nonnull Ref<EntityStore> ref,
                              @Nonnull Store<EntityStore> store,
                              @Nonnull WeaponInstanceComponent weapon) {
        PlayerStatsManager psm = Nexus.get().getPlayerStatsManager();
        if (!psm.isReady()) return;

        // Reverse the exact speed that was added
        float totalSpeed = (float) weapon.movementSpeedCurve;
        for (EnchantmentSlot slot : weapon.enchantmentSlots) {
            if (!slot.isUnlocked()) continue;
            EnchantmentDefinition def = EnchantmentRegistry.get().getDefinition(slot.chosen());
            if (def == null) continue;
            int level = slot.currentLevel();
            for (EnchantmentStatDefinition statDef : def.getStats()) {
                if (statDef.getId().equals("Swiftness")) {
                    totalSpeed += (float) statDef.getValue(level);
                }
            }
        }
        psm.addMovementSpeed(ref, store, -totalSpeed);
    }
}
