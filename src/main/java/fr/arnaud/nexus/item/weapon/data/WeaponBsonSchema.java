package fr.arnaud.nexus.item.weapon.data;

import org.bson.*;

import java.util.ArrayList;
import java.util.List;

public final class WeaponBsonSchema {

    private static final String KEY_RARITY = "nexus_rarity";
    private static final String KEY_CHOSEN_BASE = "chosen_base";
    private static final String KEY_CURRENT_LEVEL = "current_level";
    private static final String KEY_DAMAGE_MULTIPLIER = "nexus_damage_multiplier";
    private static final String KEY_WEAPON_TAG = "nexus_weapon_tag";
    private static final String KEY_ENCHANT_SLOTS = "nexus_enchant_slots";
    private static final String KEY_SLOT_INDEX = "slot";
    private static final String KEY_CHOICE_A = "choice_a";
    private static final String KEY_CHOICE_B = "choice_b";
    private static final String KEY_CHOSEN = "chosen";

    private WeaponBsonSchema() {
    }

    public static void writeRarity(BsonDocument doc, WeaponRarity rarity) {
        doc.put(KEY_RARITY, new BsonString(rarity.name()));
        doc.put(KEY_DAMAGE_MULTIPLIER, new BsonDouble(rarity.getDamageMultiplier()));
    }

    public static WeaponRarity readRarity(BsonDocument doc) {
        BsonValue value = doc.get(KEY_RARITY);
        if (value == null || !value.isString()) return WeaponRarity.COMMON;
        return WeaponRarity.fromBsonString(value.asString().getValue());
    }

    public static float readDamageMultiplier(BsonDocument doc) {
        BsonValue value = doc.get(KEY_DAMAGE_MULTIPLIER);
        if (value == null || !value.isDouble()) return 1.0f;
        return (float) value.asDouble().getValue();
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
            entry.put(KEY_SLOT_INDEX, new org.bson.BsonInt32(slot.slotIndex()));
            entry.put(KEY_CHOICE_A, new BsonString(slot.choiceA()));
            entry.put(KEY_CHOICE_B, new BsonString(slot.choiceB()));
            if (slot.chosen() != null) {
                entry.put(KEY_CHOSEN, new BsonString(slot.chosen()));
                entry.put(KEY_CHOSEN_BASE, new BsonString(slot.chosenBase()));
                entry.put(KEY_CURRENT_LEVEL, new org.bson.BsonInt32(slot.currentLevel()));
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
            String chosenBase = slotDoc.containsKey(KEY_CHOSEN_BASE)
                ? slotDoc.getString(KEY_CHOSEN_BASE).getValue()
                : (chosen != null ? stripLevelSuffix(chosen) : null);
            int currentLevel = slotDoc.containsKey(KEY_CURRENT_LEVEL)
                ? slotDoc.getInt32(KEY_CURRENT_LEVEL).getValue() : 1;
            slots.add(new EnchantmentSlot(index, choiceA, choiceB,
                chosen, chosenBase, currentLevel));
        }
        return slots;
    }

    private static String stripLevelSuffix(String enchantId) {
        int lastUnderscore = enchantId.lastIndexOf('_');
        if (lastUnderscore < 0) return enchantId;
        String suffix = enchantId.substring(lastUnderscore + 1);
        try {
            Integer.parseInt(suffix);
            return enchantId.substring(0, lastUnderscore);
        } catch (NumberFormatException e) {
            return enchantId;
        }
    }
}
