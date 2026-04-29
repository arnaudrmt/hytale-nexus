package fr.arnaud.nexus.item.weapon.enchantment;

import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.item.weapon.data.WeaponTag;
import org.bson.BsonDocument;
import org.bson.BsonValue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;

public final class EnchantmentRegistry {

    private static final EnchantmentRegistry INSTANCE = new EnchantmentRegistry();

    private static final String ENCHANTMENTS_PATH = "/nexus/enchantments/";
    private static final String ENCHANTMENTS_INDEX = ENCHANTMENTS_PATH + "index.json";

    private final Map<String, EnchantmentDefinition> definitionsById = new HashMap<>();

    private EnchantmentRegistry() {
    }

    public static EnchantmentRegistry get() {
        return INSTANCE;
    }

    public void loadAllEnchantments() {
        definitionsById.clear();
        for (String fileName : readIndex()) {
            loadFile(ENCHANTMENTS_PATH + fileName);
        }
        Nexus.get().getLogger().at(Level.INFO)
             .log("Loaded " + definitionsById.size() + " enchantment definitions.");
    }

    private List<String> readIndex() {
        try (InputStream is = Nexus.class.getResourceAsStream(ENCHANTMENTS_INDEX)) {
            if (is == null) {
                Nexus.get().getLogger().at(Level.SEVERE)
                     .log("Enchantment index not found: " + ENCHANTMENTS_INDEX);
                return Collections.emptyList();
            }
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            BsonDocument root = BsonDocument.parse(json);
            List<String> names = new ArrayList<>();
            for (BsonValue v : root.getArray("files")) {
                names.add(v.asString().getValue());
            }
            return names;
        } catch (Exception e) {
            Nexus.get().getLogger().at(Level.SEVERE)
                 .log("Failed to read enchantment index", e);
            return Collections.emptyList();
        }
    }

    private void loadFile(String path) {
        try (InputStream is = Nexus.class.getResourceAsStream(path)) {
            if (is == null) {
                Nexus.get().getLogger().at(Level.WARNING)
                     .log("Enchantment file not found: " + path);
                return;
            }
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            EnchantmentDefinition def = parse(BsonDocument.parse(json));
            if (def != null) definitionsById.put(def.getId(), def);
        } catch (Exception e) {
            Nexus.get().getLogger().at(Level.SEVERE)
                 .log("Failed to load enchantment: " + path, e);
        }
    }

    private EnchantmentDefinition parse(BsonDocument doc) {
        String id = doc.getString("Id").getValue();
        String name = doc.getString("Name").getValue();
        String description = doc.getString("Description").getValue();
        String icon = doc.getString("Icon").getValue();
        WeaponTag tag = parseTag(doc.getString("CompatibleTag").getValue());
        int baseCost = doc.getInt32("BaseCost").getValue();
        double costCurve = doc.getNumber("CostCurve").doubleValue();

        List<EnchantmentStatDefinition> stats = new ArrayList<>();
        int maxLevel = 0;

        for (BsonValue entry : doc.getArray("Stats")) {
            BsonDocument statDoc = entry.asDocument();

            String statId = statDoc.getString("Id").getValue();
            String displayName = statDoc.getString("DisplayName").getValue();
            String typeRaw = statDoc.getString("Type").getValue();
            EnchantmentStatDefinition.StatType statType =
                "Curve".equalsIgnoreCase(typeRaw)
                    ? EnchantmentStatDefinition.StatType.CURVE
                    : EnchantmentStatDefinition.StatType.FLAT;

            Map<Integer, Double> values = new LinkedHashMap<>();
            BsonDocument valuesDoc = statDoc.getDocument("Values");
            for (String levelKey : valuesDoc.keySet()) {
                int level = Integer.parseInt(levelKey);
                double value = valuesDoc.getNumber(levelKey).doubleValue();
                values.put(level, value);
                maxLevel = Math.max(maxLevel, level);
            }

            stats.add(new EnchantmentStatDefinition(statId, displayName, statType, values));
        }

        return new EnchantmentDefinition(
            id, name, description, icon, tag, baseCost, costCurve, maxLevel, stats);
    }

    private WeaponTag parseTag(String raw) {
        try {
            return WeaponTag.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            return WeaponTag.ANY;
        }
    }

    public EnchantmentDefinition getDefinition(String id) {
        return definitionsById.get(id);
    }

    public Collection<EnchantmentDefinition> getAllDefinitions() {
        return Collections.unmodifiableCollection(definitionsById.values());
    }
}
