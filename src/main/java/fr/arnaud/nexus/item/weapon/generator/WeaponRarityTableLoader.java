package fr.arnaud.nexus.item.weapon.generator;

import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.item.weapon.data.WeaponRarity;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonValue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Level;

public final class WeaponRarityTableLoader {

    public record RarityData(int enchantmentSlots, float damageMultiplier, int upgradeCost) {
    }

    private static final Map<WeaponRarity, RarityData> TABLE = new EnumMap<>(WeaponRarity.class);

    private WeaponRarityTableLoader() {
    }

    public static void load() {
        try (InputStream is = Nexus.class.getResourceAsStream(
            "/nexus/WeaponRarityTable.json")) {
            if (is == null) {
                Nexus.get().getLogger().at(Level.SEVERE).log(
                    "WeaponRarityTable.json not found in resources.");
                return;
            }
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            BsonDocument root = BsonDocument.parse(json);
            BsonArray rarities = root.getArray("Rarities");

            for (BsonValue entry : rarities) {
                BsonDocument doc = entry.asDocument();
                WeaponRarity rarity = WeaponRarity.valueOf(
                    doc.getString("Rarity").getValue());
                int slots = doc.getInt32("EnchantmentSlots").getValue();
                float multiplier = (float) doc.getDouble("DamageMultiplier").getValue();
                int cost = doc.getInt32("UpgradeCost").getValue();
                TABLE.put(rarity, new RarityData(slots, multiplier, cost));
            }

            Nexus.get().getLogger().at(Level.INFO).log(
                "WeaponRarityTable loaded: " + TABLE.size() + " entries.");
        } catch (Exception e) {
            Nexus.get().getLogger().at(Level.SEVERE).log(
                "Failed to load WeaponRarityTable: " + e.getMessage());
        }
    }

    public static RarityData get(WeaponRarity rarity) {
        return TABLE.getOrDefault(rarity, new RarityData(0, 1.0f, 0));
    }

    public static int getEnchantmentSlots(WeaponRarity rarity) {
        return get(rarity).enchantmentSlots();
    }

    public static float getDamageMultiplier(WeaponRarity rarity) {
        return get(rarity).damageMultiplier();
    }

    public static int getUpgradeCost(WeaponRarity rarity) {
        return get(rarity).upgradeCost();
    }
}
