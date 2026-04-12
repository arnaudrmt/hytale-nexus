package fr.arnaud.nexus.item.weapon.generator;

import fr.arnaud.nexus.item.weapon.data.EnchantmentSlot;
import fr.arnaud.nexus.item.weapon.data.WeaponBsonSchema;
import fr.arnaud.nexus.item.weapon.data.WeaponRarity;
import fr.arnaud.nexus.item.weapon.data.WeaponTag;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentDefinition;
import org.bson.BsonDocument;
import org.bson.BsonString;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class WeaponGenerator {

    private final EnchantmentPoolService poolService;
    private final Random random = new Random();

    public WeaponGenerator(EnchantmentPoolService poolService) {
        this.poolService = poolService;
    }

    public BsonDocument generate(String archetypeId, WeaponTag tag, WeaponRarity rarity) {
        BsonDocument doc = new BsonDocument();
        doc.put("archetype_id", new BsonString(archetypeId));
        WeaponBsonSchema.writeRarity(doc, rarity);
        WeaponBsonSchema.writeWeaponTag(doc, tag);
        WeaponBsonSchema.writeEnchantmentSlots(doc, rollEnchantmentSlots(tag, rarity));
        return doc;
    }

    public BsonDocument generateWithRandomRarity(String archetypeId, WeaponTag tag) {
        return generate(archetypeId, tag, rollRarity());
    }

    private List<EnchantmentSlot> rollEnchantmentSlots(WeaponTag tag, WeaponRarity rarity) {
        List<EnchantmentSlot> slots = new ArrayList<>();
        int slotCount = WeaponRarityTableLoader.getEnchantmentSlots(rarity);
        for (int i = 0; i < slotCount; i++) {
            List<EnchantmentDefinition> pool = poolService.getPoolForTag(tag, 1);
            if (pool.size() < 2) continue;
            EnchantmentDefinition choiceA = pool.get(random.nextInt(pool.size()));
            EnchantmentDefinition choiceB = drawDistinct(pool, choiceA);
            slots.add(new EnchantmentSlot(
                i,
                choiceA.getBaseFamily(),
                choiceB.getBaseFamily(),
                null,
                null,
                0
            ));
        }
        return slots;
    }

    private EnchantmentDefinition drawDistinct(List<EnchantmentDefinition> pool, EnchantmentDefinition exclude) {
        EnchantmentDefinition pick;
        do {
            pick = pool.get(random.nextInt(pool.size()));
        } while (pick.getBaseFamily().equals(exclude.getBaseFamily()));
        return pick;
    }

    private WeaponRarity rollRarity() {
        int roll = random.nextInt(100);
        if (roll < 50) return WeaponRarity.COMMON;
        if (roll < 75) return WeaponRarity.RARE;
        if (roll < 90) return WeaponRarity.EPIC;
        return WeaponRarity.LEGENDARY;
    }
}
