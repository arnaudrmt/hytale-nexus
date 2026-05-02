package fr.arnaud.nexus.item.weapon.generator;

import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemQuality;
import fr.arnaud.nexus.item.weapon.data.EnchantmentSlot;
import fr.arnaud.nexus.item.weapon.data.WeaponBsonSchema;
import fr.arnaud.nexus.item.weapon.data.WeaponTag;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentDefinition;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentRegistry;
import org.bson.BsonDocument;
import org.bson.BsonString;

import java.util.*;

public final class WeaponGenerator {

    private final Random random = new Random();

    public WeaponGenerator() {
    }

    public BsonDocument generateWeapon(Item item) {
        ItemQuality quality = ItemQuality.getAssetMap().getAsset(item.getQualityIndex());
        if (quality == null) return null;

        WeaponTag tag = resolveWeaponTag(item);

        BsonDocument doc = new BsonDocument();
        doc.put("archetype_id", new BsonString(item.getId()));

        WeaponBsonSchema.writeLevel(doc, 1);
        WeaponBsonSchema.writeWeaponTag(doc, tag);
        WeaponBsonSchema.writeQualityValue(doc, quality.getQualityValue());
        WeaponBsonSchema.writeName(doc, item.getTranslationKey());
        WeaponBsonSchema.writeDescription(doc, item.getDescriptionTranslationKey());
        WeaponBsonSchema.writeEnchantmentSlots(doc, rollEnchantmentSlots(tag, quality.getQualityValue()));

        return doc;
    }

    private WeaponTag resolveWeaponTag(Item item) {
        return Arrays.stream(WeaponTag.values())
                     .filter(t -> item.getId().toUpperCase().contains(t.name()))
                     .findFirst()
                     .orElse(WeaponTag.MELEE);
    }

    private List<EnchantmentSlot> rollEnchantmentSlots(WeaponTag tag, int slotCount) {
        List<EnchantmentSlot> slots = new ArrayList<>();
        List<EnchantmentDefinition> pool = new ArrayList<>(EnchantmentRegistry.getInstance().getPoolForWeaponTag(tag));
        Set<String> usedIds = new HashSet<>();

        Collections.shuffle(pool, random);

        for (int slotIndex = 0; slotIndex < slotCount; slotIndex++) {
            List<EnchantmentDefinition> available = pool.stream()
                                                        .filter(def -> !usedIds.contains(def.id()))
                                                        .toList();

            if (available.size() < 2) break;

            EnchantmentDefinition choiceA = available.get(random.nextInt(available.size()));
            usedIds.add(choiceA.id());

            List<EnchantmentDefinition> remainingAfterA = available.stream()
                                                                   .filter(def -> !def.id().equals(choiceA.id()))
                                                                   .toList();

            EnchantmentDefinition choiceB = remainingAfterA.get(random.nextInt(remainingAfterA.size()));
            usedIds.add(choiceB.id());

            slots.add(new EnchantmentSlot(slotIndex, choiceA.id(), choiceB.id(), null, 1));
        }
        return slots;
    }
}
