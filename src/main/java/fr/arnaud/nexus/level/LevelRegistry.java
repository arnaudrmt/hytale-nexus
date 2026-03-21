package fr.arnaud.nexus.level;

import com.hypixel.hytale.math.vector.Vector3i;

import java.util.EnumMap;
import java.util.Map;

/**
 * Immutable registry of all Nexus level definitions.
 * <p>
 * Schematic origins are set relative to the terrain valley floor of each
 * generated world. Adjust the Y coordinate once the worldgen is iterated and
 * the exact floor height is confirmed in-editor. The assumed valley floor is
 * Y=102 (2 blocks above the Base height of 100 in each WorldStructure).
 * <p>
 * Schematic names follow PrefabLoader dot-notation:
 * {@code "Server.Prefabs.MyFile"} resolves to
 * {@code <assetRoot>/Server/Prefabs/MyFile.prefab.json}.
 */
public final class LevelRegistry {

    private static final Map<LevelId, LevelDefinition> DEFINITIONS = new EnumMap<>(LevelId.class);

    static {
        register(new LevelDefinition(
            LevelId.SUMMIT_CRASH,
            "Nexus_Level1_SummitCrash",
            "Server.Prefabs.Nexus.Level1_SummitCrash_Airplane",
            new Vector3i(0, 102, 200),
            new Vector3i(0, 103, 0)
        ));

        register(new LevelDefinition(
            LevelId.SHIPWRECK_OF_SILENCE,
            "Nexus_Level2_ShipwreckOfSilence",
            "Server.Prefabs.Nexus.Level2_ShipwreckOfSilence_Dreadnought",
            new Vector3i(0, 101, 300),
            new Vector3i(0, 102, 0)
        ));

        register(new LevelDefinition(
            LevelId.FACTORY_OF_MEMORIES,
            "Nexus_Level3_FactoryOfMemories",
            "Server.Prefabs.Nexus.Level3_FactoryOfMemories_Clocktower",
            new Vector3i(0, 102, 250),
            new Vector3i(0, 103, 0)
        ));

        register(new LevelDefinition(
            LevelId.MISTY_CARNIVAL,
            "Nexus_Level4_MistyCarnival",
            "Server.Prefabs.Nexus.Level4_MistyCarnival_FerrisWheel",
            new Vector3i(0, 102, 200),
            new Vector3i(0, 103, 0)
        ));

        register(new LevelDefinition(
            LevelId.STATION_RADIO_SILENCE,
            "Nexus_Level5_StationRadioSilence",
            "Server.Prefabs.Nexus.Level5_StationRadioSilence_Dish",
            new Vector3i(0, 102, 300),
            new Vector3i(0, 103, 0)
        ));

        register(new LevelDefinition(
            LevelId.THE_CORE,
            "Nexus_Level6_TheCore",
            null,
            null,
            new Vector3i(0, 200, 0)
        ));
    }

    private static void register(LevelDefinition definition) {
        DEFINITIONS.put(definition.id(), definition);
    }

    public static LevelDefinition get(LevelId id) {
        LevelDefinition definition = DEFINITIONS.get(id);
        if (definition == null) {
            throw new IllegalStateException("No LevelDefinition registered for " + id);
        }
        return definition;
    }

    private LevelRegistry() {
    }
}
