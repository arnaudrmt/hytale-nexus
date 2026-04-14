package fr.arnaud.nexus.item.weapon.generator;

import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemQuality;
import fr.arnaud.nexus.item.weapon.data.EnchantmentSlot;
import fr.arnaud.nexus.item.weapon.data.WeaponBsonSchema;
import fr.arnaud.nexus.item.weapon.data.WeaponTag;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentDefinition;
import org.bson.BsonDocument;
import org.bson.BsonString;

import java.util.*;

public final class WeaponGenerator {

    private final EnchantmentPoolService poolService;
    private final Random random = new Random();

    public WeaponGenerator(EnchantmentPoolService poolService) {
        this.poolService = poolService;
    }

    public BsonDocument generateWeapon(Item item) {

        ItemQuality quality = ItemQuality.getAssetMap().getAsset(item.getQualityIndex());
        if (quality == null) return null;

        WeaponTag tag = resolveTag(item);

        BsonDocument doc = new BsonDocument();
        doc.put("archetype_id", new BsonString(item.getId()));

        WeaponBsonSchema.writeLevel(doc, 1);
        WeaponBsonSchema.writeWeaponTag(doc, tag);
        WeaponBsonSchema.writeQuality(doc, quality.getQualityValue());
        WeaponBsonSchema.writeEnchantmentSlots(doc, rollEnchantmentSlots(tag, quality.getQualityValue()));

        return doc;
    }

    private WeaponTag resolveTag(Item item) {
        return Arrays.stream(WeaponTag.values())
                     .filter(t -> item.getId().toUpperCase().contains(t.name()))
                     .findFirst()
                     .orElse(WeaponTag.MELEE);
    }

    /**
     * Rolls two distinct enchantment choices per slot.
     * No enchantment id may appear more than once across all slots in either choice position.
     * slotCount is driven by quality value (Common=0, Rare=1, Epic=2, Legendary=3).
     */
    private List<EnchantmentSlot> rollEnchantmentSlots(WeaponTag tag, int slotCount) {
        List<EnchantmentSlot> slots = new ArrayList<>();
        List<EnchantmentDefinition> pool = new ArrayList<>(poolService.getPoolForTag(tag));
        Collections.shuffle(pool, random);

        Set<String> usedIds = new HashSet<>();

        for (int i = 0; i < slotCount; i++) {
            List<EnchantmentDefinition> available = pool.stream()
                                                        .filter(def -> !usedIds.contains(def.getId()))
                                                        .toList();

            if (available.size() < 2) break;

            EnchantmentDefinition choiceA = available.get(random.nextInt(available.size()));
            usedIds.add(choiceA.getId());

            List<EnchantmentDefinition> remainingAfterA = available.stream()
                                                                   .filter(def -> !def.getId().equals(choiceA.getId()))
                                                                   .toList();

            EnchantmentDefinition choiceB = available.get(random.nextInt(available.size()));
            usedIds.add(choiceB.getId());

            slots.add(new EnchantmentSlot(
                i,
                choiceA.getId(),
                choiceB.getId(),
                null,
                1
            ));
        }
        return slots;
    }
}
