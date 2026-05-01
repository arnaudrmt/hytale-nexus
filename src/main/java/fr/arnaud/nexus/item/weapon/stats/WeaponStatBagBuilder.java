package fr.arnaud.nexus.item.weapon.stats;

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

    public static WeaponStatBag buildWeaponStatsFromBson(BsonDocument doc, int forLevel) {
        int quality = WeaponBsonSchema.readQuality(doc);
        WeaponStatCurves curves = WeaponStatRegistry.get().getCurves(quality);
        if (curves == null) return WeaponStatBag.empty();

        double damageMultiplier = computeFlat(curves.damageBase(), curves.damageCurve(), forLevel);
        double healthBoost = computeFlat(curves.healthBase(), curves.healthCurve(), forLevel);
        double movementSpeed = computeFlat(curves.speedBase(), curves.speedCurve(), forLevel);

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

    public static double computeFlat(double base, double flat, int level) {
        return base + flat * (level - 1);
    }

    private static double safeCompute(EnchantmentDefinition def, String statKey,
                                      int level, double base) {
        EnchantmentStatDefinition stat = def.getEnchantmentStatById(statKey);
        if (stat == null) return 0.0;
        return stat.computeStateValueForLevel(level, base);
    }
}
