package fr.arnaud.nexus.item.weapon.enchantment;

import org.bson.BsonDocument;
import org.bson.BsonValue;

import java.util.HashMap;
import java.util.Map;

public final class EnchantmentLevelData {

    private final int level;
    private final Map<String, Float> params;

    public EnchantmentLevelData(int level, Map<String, Float> params) {
        this.level = level;
        this.params = params;
    }

    public int getLevel() {
        return level;
    }

    public float get(String key, float defaultValue) {
        return params.getOrDefault(key, defaultValue);
    }

    public boolean has(String key) {
        return params.containsKey(key);
    }

    public static EnchantmentLevelData fromBson(BsonDocument doc) {

        int level = doc.get("Level").asNumber().intValue();

        Map<String, Float> params = new HashMap<>();
        for (Map.Entry<String, BsonValue> entry : doc.entrySet()) {
            String key = entry.getKey();
            if (key.equals("Level")) continue;

            BsonValue val = entry.getValue();

            if (val.isNumber()) {
                params.put(entry.getKey(), (float) val.asNumber().doubleValue());
            }
        }
        return new EnchantmentLevelData(level, params);
    }
}
