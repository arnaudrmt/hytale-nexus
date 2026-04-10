package fr.arnaud.nexus.spawner.loot;

import fr.arnaud.nexus.level.LevelConfig.LootChestConfig;
import fr.arnaud.nexus.level.LevelConfig.LootChestItem;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Resolves a {@link LootChestConfig} into a concrete list of item IDs to place in a chest.
 *
 * <p>Each item is rolled independently. If every roll fails the fallback rule guarantees
 * at least the highest-chance item so the chest is never empty.
 */
public final class LootRoller {

    private LootRoller() {
    }

    public static List<String> roll(LootChestConfig config) {
        List<LootChestItem> items = config.getItems();
        if (items.isEmpty()) return List.of();

        List<String> result = new ArrayList<>(items.size());
        LootChestItem highestChanceItem = items.get(0);

        for (LootChestItem item : items) {
            if (item.getChance() > highestChanceItem.getChance()) {
                highestChanceItem = item;
            }
            if (ThreadLocalRandom.current().nextFloat() < item.getChance()) {
                result.add(item.getItemId());
            }
        }

        if (result.isEmpty()) {
            result.add(highestChanceItem.getItemId());
        }

        return result;
    }
}
