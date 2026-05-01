package fr.arnaud.nexus.spawner.loot;

import fr.arnaud.nexus.level.LevelConfig.LootChestConfig;
import fr.arnaud.nexus.level.LevelConfig.LootChestItem;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class LootRoller {

    private LootRoller() {
    }

    public static List<String> roll(LootChestConfig config) {
        List<LootChestItem> items = config.getItems();
        if (items.isEmpty()) return List.of();

        List<String> result = new ArrayList<>(items.size());
        LootChestItem highestChanceItem = items.getFirst();

        for (LootChestItem item : items) {
            if (item.chance() > highestChanceItem.chance()) {
                highestChanceItem = item;
            }
            if (ThreadLocalRandom.current().nextFloat() < item.chance()) {
                result.add(item.itemId());
            }
        }

        if (result.isEmpty()) {
            result.add(highestChanceItem.itemId());
        }

        return result;
    }
}
