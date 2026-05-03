package fr.arnaud.nexus.item.weapon.stats;

import fr.arnaud.nexus.core.Nexus;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonValue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public final class WeaponStatConfigLoader {

    private static final String CONFIG_PATH = "/nexus/weapons/WeaponBaseStats.json";

    public WeaponStatConfigLoader() {
    }

    public static void load() {
        try (InputStream is = Nexus.class.getResourceAsStream(CONFIG_PATH)) {
            if (is == null) {
                Nexus.getInstance().getLogger().at(Level.SEVERE).log("Weapon config not found: " + CONFIG_PATH);
                return;
            }

            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            BsonDocument root = BsonDocument.parse(json);
            BsonArray qualities = root.getArray("Qualities");

            WeaponStatRegistry registry = WeaponStatRegistry.getInstance();
            for (BsonValue entry : qualities) {
                BsonDocument qDoc = entry.asDocument();
                int qualityValue = qDoc.getInt32("Quality").getValue();
                registry.registerCurves(qualityValue, parseCurves(qDoc));
            }

            Nexus.getInstance().getLogger().at(Level.INFO)
                 .log("Loaded " + qualities.size() + " weapon quality stat configs.");

        } catch (Exception e) {
            Nexus.getInstance().getLogger().at(Level.SEVERE).log("Failed to load weapon stats config", e);
        }
    }

    private static WeaponStatCurves parseCurves(BsonDocument qDoc) {
        return new WeaponStatCurves(
            (float) qDoc.getNumber("LevelCostBase").doubleValue(),
            (float) qDoc.getNumber("LevelCostRate").doubleValue(),
            (float) qDoc.getNumber("DamageMultiplierBase").doubleValue(),
            (float) qDoc.getNumber("DamageMultiplierPerLevel").doubleValue(),
            (float) qDoc.getNumber("HealthBonusBase").doubleValue(),
            (float) qDoc.getNumber("HealthBonusPerLevel").doubleValue(),
            (float) qDoc.getNumber("HealthBonusCap").doubleValue(),
            (float) qDoc.getNumber("MovementSpeedBase").doubleValue(),
            (float) qDoc.getNumber("MovementSpeedPerLevel").doubleValue(),
            (float) qDoc.getNumber("MovementSpeedCap").doubleValue()
        );
    }
}
