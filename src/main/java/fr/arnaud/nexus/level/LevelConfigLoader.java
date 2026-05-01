package fr.arnaud.nexus.level;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.arnaud.nexus.core.Nexus;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public final class LevelConfigLoader {

    private static final String LEVELS_PATH = "/nexus/levels/";
    private static final Gson GSON = new Gson();

    private LevelConfigLoader() {
    }

    public static LevelConfig loadAndParseLevelConfig(String levelId) {
        String resourcePath = LEVELS_PATH + levelId + ".json";
        InputStream stream = Nexus.class.getResourceAsStream(resourcePath);

        if (stream == null) {
            Nexus.getInstance().getLogger().at(Level.SEVERE)
                 .log("Level config not found: " + resourcePath);
            return null;
        }

        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            return parseRoot(root);
        } catch (Exception e) {
            Nexus.getInstance().getLogger().at(Level.SEVERE)
                 .log("Failed to parse level config '" + levelId + "': " + e.getMessage());
            return null;
        }
    }


    private static LevelConfig parseRoot(JsonObject root) {
        String id = root.get("id").getAsString();
        String name = root.get("name").getAsString();
        float difficulty = root.get("difficulty").getAsFloat();
        String nextLevelId = root.has("nextLevelId") ? root.get("nextLevelId").getAsString() : null;
        String instanceTemplate = root.get("instanceTemplate").getAsString();

        LevelConfig.Position spawnPoint = parsePosition(root.getAsJsonObject("spawnPoint"));
        LevelConfig.Position finishPoint = parsePosition(root.getAsJsonObject("finishPoint"));

        List<LevelConfig.SpawnerConfig> spawners = new ArrayList<>();
        for (JsonElement el : root.getAsJsonArray("spawners")) {
            spawners.add(parseSpawner(el.getAsJsonObject()));
        }

        List<LevelConfig.IndependentChestConfig> independentChests = new ArrayList<>();
        JsonArray chestsArray = root.getAsJsonArray("independentChests");
        if (chestsArray != null) {
            for (JsonElement el : chestsArray) {
                independentChests.add(parseIndependentChest(el.getAsJsonObject()));
            }
        }


        return new LevelConfig(id, name, difficulty, spawnPoint, finishPoint, spawners, independentChests, nextLevelId, instanceTemplate);
    }

    private static LevelConfig.Position parsePosition(JsonObject obj) {
        return new LevelConfig.Position(
            obj.get("x").getAsDouble(),
            obj.get("y").getAsDouble(),
            obj.get("z").getAsDouble()
        );
    }

    private static LevelConfig.SpawnerConfig parseSpawner(JsonObject obj) {
        LevelConfig.Position pos = parsePosition(obj.getAsJsonObject("position"));
        float triggerRadius = obj.get("triggerRadius").getAsFloat();
        float spawnRadius = obj.get("spawnRadius").getAsFloat();

        List<LevelConfig.WaveConfig> waves = new ArrayList<>();
        JsonArray wavesArray = obj.getAsJsonArray("waves");
        if (wavesArray != null) {
            for (JsonElement el : wavesArray) {
                waves.add(parseWaveConfig(el.getAsJsonObject()));
            }
        }

        List<LevelConfig.MobEntry> mobs = new ArrayList<>();
        JsonArray mobsArray = obj.getAsJsonArray("mobs");
        if (mobsArray != null) {
            for (JsonElement el : mobsArray) {
                mobs.add(parseMobEntry(el.getAsJsonObject()));
            }
        }

        // "lootChest" is optional - absent means no chest spawns for this spawner
        LevelConfig.LootChestConfig lootChest = null;
        JsonObject lootChestObj = obj.getAsJsonObject("lootChest");
        if (lootChestObj != null) {
            lootChest = parseLootChest(lootChestObj);
        }

        return new LevelConfig.SpawnerConfig(pos, triggerRadius, spawnRadius, waves, mobs, lootChest);
    }

    private static LevelConfig.WaveConfig parseWaveConfig(JsonObject obj) {
        int wave = obj.get("wave").getAsInt();
        float value = obj.get("value").getAsFloat();

        String typeRaw = obj.get("type").getAsString().toUpperCase();
        LevelConfig.WaveType type = LevelConfig.WaveType.valueOf(typeRaw);

        float timeout = obj.has("timeout") ? obj.get("timeout").getAsFloat() : 0f;

        return new LevelConfig.WaveConfig(wave, type, value, timeout);
    }

    private static LevelConfig.MobEntry parseMobEntry(JsonObject obj) {
        String mobId = obj.get("mobId").getAsString();
        int minCount = obj.get("minCount").getAsInt();
        int maxCount = obj.get("maxCount").getAsInt();
        float spawnRate = obj.has("spawnRate") ? obj.get("spawnRate").getAsFloat() : 0f;
        int wave = obj.has("wave") ? obj.get("wave").getAsInt() : 0;
        int minEssence = obj.has("minEssence") ? obj.get("minEssence").getAsInt() : 0;
        int maxEssence = obj.has("maxEssence") ? obj.get("maxEssence").getAsInt() : 0;

        LevelConfig.MobLootConfig lootTable = null;
        JsonObject lootObj = obj.getAsJsonObject("lootTable");
        if (lootObj != null) {
            lootTable = parseMobLoot(lootObj);
        }

        return new LevelConfig.MobEntry(mobId, minCount, maxCount, spawnRate, wave,
            minEssence, maxEssence, lootTable);
    }

    private static LevelConfig.MobLootConfig parseMobLoot(JsonObject obj) {
        List<LevelConfig.MobLootItem> items = new ArrayList<>();
        JsonArray arr = obj.getAsJsonArray("items");
        if (arr != null) {
            for (JsonElement el : arr) {
                JsonObject itemObj = el.getAsJsonObject();
                items.add(new LevelConfig.MobLootItem(
                    itemObj.get("itemId").getAsString(),
                    itemObj.get("chance").getAsFloat()
                ));
            }
        }
        return new LevelConfig.MobLootConfig(items);
    }

    private static LevelConfig.LootChestConfig parseLootChest(JsonObject obj) {
        List<LevelConfig.LootChestItem> items = new ArrayList<>();
        JsonArray itemsArray = obj.getAsJsonArray("items");
        if (itemsArray != null) {
            for (JsonElement el : itemsArray) {
                JsonObject itemObj = el.getAsJsonObject();
                String itemId = itemObj.get("itemId").getAsString();
                float chance = itemObj.get("chance").getAsFloat();
                items.add(new LevelConfig.LootChestItem(itemId, chance));
            }
        }
        return new LevelConfig.LootChestConfig(items);
    }

    private static LevelConfig.IndependentChestConfig parseIndependentChest(JsonObject obj) {
        LevelConfig.Position pos = parsePosition(obj.getAsJsonObject("position"));
        float triggerRadius = obj.get("triggerRadius").getAsFloat();

        List<LevelConfig.LootChestItem> items = new ArrayList<>();
        JsonArray arr = obj.getAsJsonArray("items");
        if (arr != null) {
            for (JsonElement el : arr) {
                JsonObject itemObj = el.getAsJsonObject();
                items.add(new LevelConfig.LootChestItem(
                    itemObj.get("itemId").getAsString(),
                    itemObj.get("chance").getAsFloat()
                ));
            }
        }

        return new LevelConfig.IndependentChestConfig(pos, triggerRadius, items);
    }
}
