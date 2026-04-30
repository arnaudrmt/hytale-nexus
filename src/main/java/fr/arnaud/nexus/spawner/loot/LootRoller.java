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
