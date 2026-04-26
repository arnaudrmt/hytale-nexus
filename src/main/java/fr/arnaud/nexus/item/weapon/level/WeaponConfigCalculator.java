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
        float increment = getWeaponStatCurves(doc).damageCurve();
        return base + (increment * (currentLevel - 1));
    }

    public static float calculateHealthBoost(BsonDocument doc) {
        int currentLevel = WeaponBsonSchema.readLevel(doc);
        WeaponStatCurves curves = getWeaponStatCurves(doc);
        float raw = curves.healthBase() + (curves.healthFlat() * currentLevel);
        return curves.healthCap() > 0 ? Math.min(raw, curves.healthCap()) : raw;
    }


    public static float calculateMovementSpeedBoost(BsonDocument doc) {
        int currentLevel = WeaponBsonSchema.readLevel(doc);
        WeaponStatCurves curves = getWeaponStatCurves(doc);
        float raw = curves.speedBase() + (curves.speedFlat() * currentLevel);
        return curves.speedCap() > 0 ? Math.min(raw, curves.speedCap()) : raw;
    }

    public static WeaponStatCurves getWeaponStatCurves(BsonDocument doc) {
        int quality = WeaponBsonSchema.readQuality(doc);
        return WeaponStatRegistry.get().getCurves(quality);
    }
}
