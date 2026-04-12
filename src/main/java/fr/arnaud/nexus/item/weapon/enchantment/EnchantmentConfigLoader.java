package fr.arnaud.nexus.item.weapon.enchantment;

import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.item.weapon.data.WeaponTag;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonValue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public final class EnchantmentConfigLoader {

    private static final String[] ENCHANTMENT_FILES = {
        "Enchant_Cleave",
        "Enchant_Shockwave",
        "Enchant_Aspiration",
        "Enchant_ChainShot",
        "Enchant_Piercing",
        "Enchant_Bouncing"
    };

    private static final java.util.Map<String, String> BEHAVIOR_MAP = java.util.Map.of(
        "Enchant_Cleave", "cleave",
        "Enchant_Shockwave", "shockwave",
        "Enchant_Aspiration", "aspiration",
        "Enchant_ChainShot", "chain_projectile",
        "Enchant_Piercing", "piercing",
        "Enchant_Bouncing", "bouncing_projectile"
    );

    private EnchantmentConfigLoader() {
    }

    public static void load() {
        for (String baseName : ENCHANTMENT_FILES) {
            loadEnchantment(baseName);
        }
    }

    private static void loadEnchantment(String baseName) {
        String path = "/nexus/enchantments/" + baseName + ".json";
        try (InputStream is = Nexus.class.getResourceAsStream(path)) {
            if (is == null) {
                Nexus.get().getLogger().at(Level.WARNING).log("Enchantment config not found: " + path);
                return;
            }

            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            BsonDocument doc = BsonDocument.parse(json);

            String baseFamily = doc.getString("Id").getValue();
            WeaponTag tag = WeaponTag.valueOf(doc.getString("CompatibleTag").getValue());

            float baseCost = (float) doc.get("BaseCost").asNumber().doubleValue();
            float costCurve = (float) doc.get("CostCurve").asNumber().doubleValue();

            String behaviorId = BEHAVIOR_MAP.get(baseFamily);

            BsonArray levels = doc.getArray("Levels");
            List<EnchantmentLevelData> levelDataList = new ArrayList<>();
            for (BsonValue levelEntry : levels) {
                levelDataList.add(EnchantmentLevelData.fromBson(
                    levelEntry.asDocument()));
            }

            EnchantmentRegistry registry = EnchantmentRegistry.get();

            for (EnchantmentLevelData levelData : levelDataList) {
                int lvl = levelData.getLevel();
                String enchantId = baseFamily + "_" + lvl;

                EnchantmentDefinition definition = new EnchantmentDefinition(
                    enchantId,
                    baseFamily,
                    lvl,
                    tag,
                    behaviorId,
                    baseCost,
                    costCurve,
                    levelDataList
                );
                registry.registerDefinition(definition);
            }

            Nexus.get().getLogger().at(Level.INFO).log("Loaded enchantment config: " + baseFamily
                + " (" + levelDataList.size() + " levels)");

        } catch (Exception e) {
            Nexus.get().getLogger().at(Level.SEVERE).log("Failed to load enchantment config " + baseName, e);
        }
    }
}
