package fr.arnaud.nexus.input.hover;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.Set;

/**
 * Defines which asset IDs are eligible for cursor hover highlighting.
 */
public final class CursorHoverAllowlist {

    private static final Set<String> INTERACTABLE_BLOCK_IDS = Set.of(
        "Furniture_Dungeon_Chest_Epic",
        "Furniture_Dungeon_Chest_Rare",
        "Furniture_Dungeon_Chest_Common",
        "Soil_Grass"
    );

    private static final Set<String> INTERACTABLE_ENTITY_TYPE_IDS = Set.of(
        "Nexus_Sentinel_Boss",
        "Nexus_Mob_Standard"
    );

    private CursorHoverAllowlist() {
    }

    public static boolean isInteractableBlock(@NonNullDecl String blockAssetId) {
        return INTERACTABLE_BLOCK_IDS.contains(blockAssetId);
    }

    public static boolean isInteractableEntity(@NonNullDecl String entityTypeId) {
        return INTERACTABLE_ENTITY_TYPE_IDS.contains(entityTypeId);
    }
}
