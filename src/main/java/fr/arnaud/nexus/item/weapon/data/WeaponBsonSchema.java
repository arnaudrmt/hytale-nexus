package fr.arnaud.nexus.item.weapon.data;

import com.hypixel.hytale.server.core.Message;
import org.bson.*;

import java.util.ArrayList;
import java.util.List;

public final class WeaponBsonSchema {

    private static final String KEY_LEVEL = "nexus_level";
    private static final String KEY_WEAPON_TAG = "nexus_weapon_tag";
    private static final String KEY_QUALITY_VALUE = "nexus_quality_value";

    private static final String KEY_NAME = "nexus_key_name";
    private static final String KEY_DESCRIPTION = "nexus_key_description";

    private static final String KEY_ENCHANT_SLOTS = "nexus_enchant_slots";
    private static final String KEY_SLOT_INDEX = "slot";
    private static final String KEY_CHOICE_A = "choice_a";
    private static final String KEY_CHOICE_B = "choice_b";
    private static final String KEY_CHOSEN = "chosen";
    private static final String KEY_CURRENT_LEVEL = "current_level";

    private WeaponBsonSchema() {
    }

    public static void writeLevel(BsonDocument doc, int level) {
        doc.put(KEY_LEVEL, new BsonInt32(level));
    }

    public static int readLevel(BsonDocument doc) {
        BsonValue value = doc.get(KEY_LEVEL);
        if (value == null || !value.isInt32()) return 1;
        return value.asInt32().getValue();
    }

    public static void writeQualityValue(BsonDocument doc, int quality) {
        doc.put(KEY_QUALITY_VALUE, new BsonInt32(quality));
    }

    public static int readQualityValue(BsonDocument doc) {
        BsonValue value = doc.get(KEY_QUALITY_VALUE);
        if (value == null || !value.isInt32()) return 1;
        return value.asInt32().getValue();
    }

    public static void writeName(BsonDocument doc, String name) {
        doc.put(KEY_NAME, new BsonString(name));
    }

    public static String readName(BsonDocument doc) {
        BsonValue value = doc.get(KEY_NAME);
        if (value == null) return Message.translation("nexus.unknown").getRawText();
        return value.asString().getValue();
    }

    public static void writeDescription(BsonDocument doc, String description) {
        doc.put(KEY_DESCRIPTION, new BsonString(description));
    }

    public static String readDescription(BsonDocument doc) {
        BsonValue value = doc.get(KEY_DESCRIPTION);
        if (value == null) return Message.translation("nexus.unknown").getRawText();
        return value.asString().getValue();
    }

    public static void writeWeaponTag(BsonDocument doc, WeaponTag tag) {
        doc.put(KEY_WEAPON_TAG, new BsonString(tag.name()));
    }

    public static WeaponTag readWeaponTag(BsonDocument doc) {
        BsonValue value = doc.get(KEY_WEAPON_TAG);
        if (value == null || !value.isString()) return WeaponTag.ANY;
        try {
            return WeaponTag.valueOf(value.asString().getValue());
        } catch (IllegalArgumentException e) {
            return WeaponTag.ANY;
        }
    }

    public static void writeEnchantmentSlots(BsonDocument doc, List<EnchantmentSlot> slots) {
        BsonArray array = new BsonArray();
        for (EnchantmentSlot slot : slots) {
            BsonDocument entry = new BsonDocument();
            entry.put(KEY_SLOT_INDEX, new BsonInt32(slot.slotIndex()));
            entry.put(KEY_CHOICE_A, new BsonString(slot.choiceA()));
            entry.put(KEY_CHOICE_B, new BsonString(slot.choiceB()));
            if (slot.chosen() != null) {
                entry.put(KEY_CHOSEN, new BsonString(slot.chosen()));
                entry.put(KEY_CURRENT_LEVEL, new BsonInt32(slot.currentLevel()));
            }
            array.add(entry);
        }
        doc.put(KEY_ENCHANT_SLOTS, array);
    }

    public static List<EnchantmentSlot> readEnchantmentSlots(BsonDocument doc) {
        BsonValue raw = doc.get(KEY_ENCHANT_SLOTS);
        List<EnchantmentSlot> slots = new ArrayList<>();
        if (raw == null || !raw.isArray()) return slots;
        for (BsonValue entry : raw.asArray()) {
            if (!entry.isDocument()) continue;
            BsonDocument slotDoc = entry.asDocument();
            int index = slotDoc.getInt32(KEY_SLOT_INDEX).getValue();
            String choiceA = slotDoc.getString(KEY_CHOICE_A).getValue();
            String choiceB = slotDoc.getString(KEY_CHOICE_B).getValue();
            String chosen = slotDoc.containsKey(KEY_CHOSEN)
                ? slotDoc.getString(KEY_CHOSEN).getValue() : null;
            int currentLevel = slotDoc.containsKey(KEY_CURRENT_LEVEL)
                ? slotDoc.getInt32(KEY_CURRENT_LEVEL).getValue() : 1;
            slots.add(new EnchantmentSlot(index, choiceA, choiceB,
                chosen, currentLevel));
        }
        return slots;
    }

    public static boolean isNexusWeapon(BsonDocument doc) {
        return doc != null && doc.containsKey("nexus_quality_value");
    }
}
