package fr.arnaud.nexus.item.weapon.stats;

import fr.arnaud.nexus.core.Nexus;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonValue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public final class WeaponStatConfigLoader {

    public static void load() {
        String path = "/nexus/weapons/WeaponBaseStats.json";
        try (InputStream is = Nexus.class.getResourceAsStream(path)) {
            if (is == null) {
                Nexus.get().getLogger().at(Level.SEVERE).log("Weapon config not found: " + path);
                return;
            }

            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            BsonDocument root = BsonDocument.parse(json);
            BsonArray qualities = root.getArray("Qualities");

            WeaponStatRegistry registry = WeaponStatRegistry.get();

            for (BsonValue value : qualities) {
                BsonDocument qDoc = value.asDocument();
                int qualityValue = qDoc.getInt32("Quality").getValue();

                WeaponStatCurves curves = new WeaponStatCurves(
                    (float) qDoc.getNumber("LevelCostBase").doubleValue(),
                    (float) qDoc.getNumber("LevelCostCurve").doubleValue(),
                    (float) qDoc.getNumber("DamageMultiplierBase").doubleValue(),
                    (float) qDoc.getNumber("DamageMultiplierCurve").doubleValue(),
                    (float) qDoc.getNumber("HealthBoostBase").doubleValue(),
                    (float) qDoc.getNumber("HealthBoostFlat").doubleValue(),
                    (float) qDoc.getNumber("MovementSpeedBase").doubleValue(),
                    (float) qDoc.getNumber("MovementSpeedFlat").doubleValue()
                );

                registry.registerCurves(qualityValue, curves);
            }

            Nexus.get().getLogger().at(Level.SEVERE).log("Loaded " + qualities.size() + " weapon quality stats.");

        } catch (Exception e) {
            Nexus.get().getLogger().at(Level.SEVERE).log("Failed to load weapon stats config", e);
        }
    }
}
