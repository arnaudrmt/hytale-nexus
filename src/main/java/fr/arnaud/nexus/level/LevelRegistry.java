package fr.arnaud.nexus.level;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.math.WorldPosition;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;

public final class LevelRegistry {

    private static final LevelRegistry INSTANCE = new LevelRegistry();

    private static final String LEVELS_RESOURCE_PATH = "/nexus/levels/";
    private static final String LEVELS_INDEX = LEVELS_RESOURCE_PATH + "index.json";
    private static final Gson GSON = new Gson();

    private final Map<String, LevelConfig> levelsById = new LinkedHashMap<>();
    private final List<String> orderedLevelIds = new ArrayList<>();
    private LevelDefaults defaults = LevelDefaults.fallback();

    private LevelRegistry() {
    }

    public static LevelRegistry getInstance() {
        return INSTANCE;
    }

    public void loadAllLevels() {
        levelsById.clear();
        orderedLevelIds.clear();

        IndexParseResult index = readIndex();
        this.defaults = index.defaults();

        for (String levelId : index.levelIds()) {
            LevelConfig config = parseLevel(levelId);
            if (config != null) {
                levelsById.put(levelId, config);
                orderedLevelIds.add(levelId);
            }
        }

        Nexus.getInstance().getLogger().at(Level.INFO)
             .log("Loaded " + levelsById.size() + " level configs.");
    }

    public String getFirstLevelId() {
        if (orderedLevelIds.isEmpty()) throw new IllegalStateException("LevelRegistry not loaded.");
        return orderedLevelIds.getFirst();
    }

    @Nullable
    public String getNextLevelId(String levelId) {
        int index = orderedLevelIds.indexOf(levelId);
        if (index < 0 || index >= orderedLevelIds.size() - 1) return null;
        return orderedLevelIds.get(index + 1);
    }

    @Nullable
    public LevelConfig getLevel(String levelId) {
        return levelsById.get(levelId);
    }

    public Collection<LevelConfig> getAllLevels() {
        return Collections.unmodifiableCollection(levelsById.values());
    }

    public LevelDefaults getDefaults() {
        return defaults;
    }

    private record IndexParseResult(List<String> levelIds, LevelDefaults defaults) {
    }

    private IndexParseResult readIndex() {
        try (InputStream is = Nexus.class.getResourceAsStream(LEVELS_INDEX)) {
            if (is == null) {
                Nexus.getInstance().getLogger().at(Level.SEVERE).log("Level index not found: " + LEVELS_INDEX);
                return new IndexParseResult(Collections.emptyList(), LevelDefaults.fallback());
            }

            JsonObject root = GSON.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JsonObject.class);

            List<String> ids = new ArrayList<>();
            for (JsonElement el : root.getAsJsonArray("levels")) {
                ids.add(el.getAsString());
            }

            LevelDefaults parsedDefaults = parseDefaults(root.getAsJsonObject("defaults"));
            return new IndexParseResult(ids, parsedDefaults);

        } catch (Exception e) {
            Nexus.getInstance().getLogger().at(Level.SEVERE).log("Failed to read level index: " + e.getMessage());
            return new IndexParseResult(Collections.emptyList(), LevelDefaults.fallback());
        }
    }

    private static LevelDefaults parseDefaults(@Nullable JsonObject obj) {
        LevelDefaults fallback = LevelDefaults.fallback();
        if (obj == null) return fallback;

        return new LevelDefaults(
            getFloatOr(obj, "activationRadius", fallback.activationRadius()),
            getFloatOr(obj, "spawnRadius", fallback.spawnRadius()),
            getFloatOr(obj, "spawnStaggerInterval", fallback.spawnStaggerInterval()),
            getFloatOr(obj, "waveTimeoutSeconds", fallback.waveTimeoutSeconds()),
            getFloatOr(obj, "killWaveThreshold", fallback.killWaveThreshold()),
            getFloatOr(obj, "timeWaveInterval", fallback.timeWaveInterval())
        );
    }

    @Nullable
    private LevelConfig parseLevel(String levelId) {
        String resourcePath = LEVELS_RESOURCE_PATH + levelId + ".json";
        try (InputStream stream = Nexus.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                Nexus.getInstance().getLogger().at(Level.SEVERE).log("Level config not found: " + resourcePath);
                return null;
            }
            JsonObject root = GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class);
            return parseRoot(root);
        } catch (Exception e) {
            Nexus.getInstance().getLogger().at(Level.SEVERE)
                 .log("Failed to parse level config '" + levelId + "': " + e.getMessage());
            return null;
        }
    }

    private LevelConfig parseRoot(JsonObject root) {
        String id = root.get("id").getAsString();
        String name_key = root.get("name_key").getAsString();
        String worldTemplate = root.get("instanceTemplate").getAsString();

        WorldPosition spawnPoint = parsePosition(root.getAsJsonObject("spawnPoint"));
        WorldPosition finishPoint = parsePosition(root.getAsJsonObject("finishPoint"));

        List<LevelConfig.Spawner> spawners = new ArrayList<>();
        for (JsonElement el : root.getAsJsonArray("spawners")) {
            spawners.add(parseSpawner(el.getAsJsonObject()));
        }

        List<LevelConfig.StandaloneChest> standaloneChests = new ArrayList<>();
        JsonArray chestsArray = root.getAsJsonArray("standaloneChests");
        if (chestsArray != null) {
            for (JsonElement el : chestsArray) {
                standaloneChests.add(parseStandaloneChest(el.getAsJsonObject()));
            }
        }

        return new LevelConfig(id, name_key, spawnPoint, finishPoint, spawners, standaloneChests, worldTemplate);
    }

    private static WorldPosition parsePosition(JsonObject obj) {
        return new WorldPosition(
            obj.get("x").getAsDouble(),
            obj.get("y").getAsDouble(),
            obj.get("z").getAsDouble()
        );
    }

    private LevelConfig.Spawner parseSpawner(JsonObject obj) {
        WorldPosition pos = parsePosition(obj.getAsJsonObject("position"));
        float activationRadius = getFloatOr(obj, "activationRadius", defaults.activationRadius());
        float spawnRadius = getFloatOr(obj, "spawnRadius", defaults.spawnRadius());

        List<LevelConfig.Wave> waves = new ArrayList<>();
        JsonArray wavesArray = obj.getAsJsonArray("waves");
        if (wavesArray != null) {
            for (JsonElement el : wavesArray) waves.add(parseWave(el.getAsJsonObject()));
        }

        List<LevelConfig.MobEntry> mobs = new ArrayList<>();
        JsonArray mobsArray = obj.getAsJsonArray("mobs");
        if (mobsArray != null) {
            for (JsonElement el : mobsArray) mobs.add(parseMobEntry(el.getAsJsonObject()));
        }

        LevelConfig.LootChest lootChest = null;
        JsonObject lootChestObj = obj.getAsJsonObject("lootChest");
        if (lootChestObj != null) lootChest = parseLootChest(lootChestObj);

        return new LevelConfig.Spawner(pos, activationRadius, spawnRadius, waves, mobs, lootChest);
    }

    private LevelConfig.Wave parseWave(JsonObject obj) {
        int wave = obj.get("wave").getAsInt();
        LevelConfig.WaveType type = LevelConfig.WaveType.valueOf(obj.get("type").getAsString().toUpperCase());

        float value = switch (type) {
            case KILL -> getFloatOr(obj, "value", defaults.killWaveThreshold());
            case TIME -> getFloatOr(obj, "value", defaults.timeWaveInterval());
        };

        float timeout = getFloatOr(obj, "timeout", defaults.waveTimeoutSeconds());
        return new LevelConfig.Wave(wave, type, value, timeout);
    }

    private LevelConfig.MobEntry parseMobEntry(JsonObject obj) {
        String mobId = obj.get("mobId").getAsString();
        int minCount = obj.get("minCount").getAsInt();
        int maxCount = obj.get("maxCount").getAsInt();
        float spawnStaggerInterval = getFloatOr(obj, "spawnStaggerInterval", defaults.spawnStaggerInterval());
        int wave = obj.has("wave") ? obj.get("wave").getAsInt() : 0;
        int minEssence = obj.has("minEssence") ? obj.get("minEssence").getAsInt() : 0;
        int maxEssence = obj.has("maxEssence") ? obj.get("maxEssence").getAsInt() : 0;

        LevelConfig.MobLoot lootTable = null;
        JsonObject lootObj = obj.getAsJsonObject("lootTable");
        if (lootObj != null) lootTable = parseMobLoot(lootObj);

        return new LevelConfig.MobEntry(mobId, minCount, maxCount, spawnStaggerInterval,
            wave, minEssence, maxEssence, lootTable);
    }

    private static LevelConfig.MobLoot parseMobLoot(JsonObject obj) {
        List<LevelConfig.MobLootEntry> items = new ArrayList<>();
        JsonArray arr = obj.getAsJsonArray("items");
        if (arr != null) {
            for (JsonElement el : arr) {
                JsonObject itemObj = el.getAsJsonObject();
                items.add(new LevelConfig.MobLootEntry(
                    itemObj.get("itemId").getAsString(),
                    itemObj.get("chance").getAsFloat()
                ));
            }
        }
        return new LevelConfig.MobLoot(items);
    }

    private static LevelConfig.LootChest parseLootChest(JsonObject obj) {
        List<LevelConfig.LootEntry> items = new ArrayList<>();
        JsonArray itemsArray = obj.getAsJsonArray("items");
        if (itemsArray != null) {
            for (JsonElement el : itemsArray) {
                JsonObject itemObj = el.getAsJsonObject();
                items.add(new LevelConfig.LootEntry(
                    itemObj.get("itemId").getAsString(),
                    itemObj.get("chance").getAsFloat()
                ));
            }
        }
        return new LevelConfig.LootChest(items);
    }

    private static LevelConfig.StandaloneChest parseStandaloneChest(JsonObject obj) {
        WorldPosition pos = parsePosition(obj.getAsJsonObject("position"));
        float activationRadius = obj.get("activationRadius").getAsFloat();

        List<LevelConfig.LootEntry> items = new ArrayList<>();
        JsonArray arr = obj.getAsJsonArray("items");
        if (arr != null) {
            for (JsonElement el : arr) {
                JsonObject itemObj = el.getAsJsonObject();
                items.add(new LevelConfig.LootEntry(
                    itemObj.get("itemId").getAsString(),
                    itemObj.get("chance").getAsFloat()
                ));
            }
        }
        return new LevelConfig.StandaloneChest(pos, activationRadius, items);
    }

    private static float getFloatOr(JsonObject obj, String key, float fallback) {
        return obj.has(key) ? obj.get(key).getAsFloat() : fallback;
    }
}
