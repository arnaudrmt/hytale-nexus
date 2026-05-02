package fr.arnaud.nexus.spawner.loot;

import fr.arnaud.nexus.level.LevelConfig.LootEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class LootRoller {

    private LootRoller() {
    }

    public static List<String> roll(List<LootEntry> items) {
        if (items.isEmpty()) return List.of();

        List<String> result = new ArrayList<>(items.size());
        LootEntry highestChanceItem = items.getFirst();

        for (LootEntry item : items) {
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
