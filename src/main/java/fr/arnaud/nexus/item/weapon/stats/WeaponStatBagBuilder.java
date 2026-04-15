package fr.arnaud.nexus.item.weapon.stats;

import fr.arnaud.nexus.item.weapon.component.WeaponInstanceComponent;
import fr.arnaud.nexus.item.weapon.data.EnchantmentSlot;
import fr.arnaud.nexus.item.weapon.data.WeaponBsonSchema;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentDefinition;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentRegistry;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentStatDefinition;
import org.bson.BsonDocument;

import java.util.List;

public final class WeaponStatBagBuilder {

    private static final String KEY_DAMAGE_BONUS = "DamageBonus";
    private static final String KEY_HEALTH_BOOST = "HealthBoost";
    private static final String KEY_MOVEMENT_SPEED = "MovementSpeedBoost";

    private WeaponStatBagBuilder() {
    }

    /**
     * Builds a WeaponStatBag from a live WeaponInstanceComponent.
     * Used at runtime when the component is guaranteed to exist.
     */
    public static WeaponStatBag build(WeaponInstanceComponent instance) {
        double damageMultiplier = instance.damageMultiplierCurve;
        double healthBoost = instance.healthBoostCurve;
        double movementSpeed = instance.movementSpeedCurve;

        for (EnchantmentSlot slot : instance.enchantmentSlots) {
            if (!slot.isUnlocked()) continue;
            EnchantmentDefinition def = EnchantmentRegistry.get().getDefinition(slot.chosen());
            if (def == null) continue;
            int level = slot.currentLevel();
            damageMultiplier += safeCompute(def, KEY_DAMAGE_BONUS, level, 1.0);
            healthBoost += safeCompute(def, KEY_HEALTH_BOOST, level, 100.0);
            movementSpeed += safeCompute(def, KEY_MOVEMENT_SPEED, level, 1.0);
        }

        return new WeaponStatBag(damageMultiplier, healthBoost, movementSpeed);
    }

    /**
     * Builds a WeaponStatBag directly from a weapon BSON document.
     * Used in the UI where only the document is available.
     *
     * @param doc      the weapon's BSON document
     * @param forLevel the weapon level to compute stats for (pass current or next)
     */
    public static WeaponStatBag buildFromBson(BsonDocument doc, int forLevel) {
        int quality = WeaponBsonSchema.readQuality(doc);
        WeaponStatCurves curves = WeaponStatRegistry.get().getCurves(quality);
        if (curves == null) return WeaponStatBag.empty();

        double damageMultiplier = computeCurve(curves.damageBase(), curves.damageCurve(), forLevel);
        double healthBoost = computeFlat(curves.healthBase(), curves.healthFlat(), forLevel);
        double movementSpeed = computeFlat(curves.speedBase(), curves.speedFlat(), forLevel);

        List<EnchantmentSlot> slots = WeaponBsonSchema.readEnchantmentSlots(doc);
        for (EnchantmentSlot slot : slots) {
            if (!slot.isUnlocked()) continue;
            EnchantmentDefinition def = EnchantmentRegistry.get().getDefinition(slot.chosen());
            if (def == null) continue;
            int enchantLevel = slot.currentLevel();
            damageMultiplier += safeCompute(def, KEY_DAMAGE_BONUS, enchantLevel, 1.0);
            healthBoost += safeCompute(def, KEY_HEALTH_BOOST, enchantLevel, 100.0);
            movementSpeed += safeCompute(def, KEY_MOVEMENT_SPEED, enchantLevel, 1.0);
        }

        return new WeaponStatBag(damageMultiplier, healthBoost, movementSpeed);
    }

    // ── Stat curve formulas ───────────────────────────────────────────────────

    /**
     * Exponential curve: base * curve^(level-1)
     * Used for multiplicative stats like damage.
     */
    public static double computeCurve(double base, double curve, int level) {
        if (level <= 1) return base;
        return base * Math.pow(curve, level - 1);
    }

    /**
     * Flat-per-level: base + flat * (level-1)
     * Used for additive stats like health and speed.
     */
    public static double computeFlat(double base, double flat, int level) {
        return base + flat * (level - 1);
    }

    // ── Null-safe enchant stat lookup ─────────────────────────────────────────

    private static double safeCompute(EnchantmentDefinition def, String statKey,
                                      int level, double base) {
        EnchantmentStatDefinition stat = def.getStat(statKey);
        if (stat == null) return 0.0;
        return stat.compute(level, base);
    }
}
