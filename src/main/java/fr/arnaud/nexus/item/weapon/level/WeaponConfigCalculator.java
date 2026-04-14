package fr.arnaud.nexus.item.weapon.level;

import fr.arnaud.nexus.item.weapon.data.WeaponBsonSchema;
import fr.arnaud.nexus.item.weapon.stats.WeaponStatCurves;
import fr.arnaud.nexus.item.weapon.stats.WeaponStatRegistry;
import org.bson.BsonDocument;

public final class WeaponConfigCalculator {


    private WeaponConfigCalculator() {
    }

    public static float calculateUpgradeCost(BsonDocument doc) {
        int currentLevel = WeaponBsonSchema.readLevel(doc);
        float baseCost = getWeaponStatCurves(doc).levelCostBase();
        float curve = getWeaponStatCurves(doc).levelCostCurve();
        return baseCost * (float) Math.pow(curve, currentLevel - 1);
    }

    public static float calculateDamageMultiplier(BsonDocument doc) {
        int currentLevel = WeaponBsonSchema.readLevel(doc);
        float base = getWeaponStatCurves(doc).damageBase();
        float curve = getWeaponStatCurves(doc).damageCurve();
        return base * (float) Math.pow(curve, currentLevel - 1);
    }

    public static float calculateHealthBoost(BsonDocument doc) {
        int currentLevel = WeaponBsonSchema.readLevel(doc);
        float base = getWeaponStatCurves(doc).healthBase();
        float flat = getWeaponStatCurves(doc).healthFlat();
        return base + (flat * currentLevel);
    }

    public static float calculateMovementSpeedBoost(BsonDocument doc) {
        int currentLevel = WeaponBsonSchema.readLevel(doc);
        float base = getWeaponStatCurves(doc).speedBase();
        float flat = getWeaponStatCurves(doc).speedFlat();
        return base + (flat * currentLevel);
    }

    public static WeaponStatCurves getWeaponStatCurves(BsonDocument doc) {
        int quality = WeaponBsonSchema.readQuality(doc);
        return WeaponStatRegistry.get().getCurves(quality);
    }
}
