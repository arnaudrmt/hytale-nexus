package fr.arnaud.nexus.level;

import com.hypixel.hytale.math.vector.Vector3i;

import javax.annotation.Nullable;

/**
 * Immutable descriptor for a single Nexus level instance.
 *
 * @param id              the canonical level identifier
 * @param instanceName    name of the Hytale world instance, matching the folder
 *                        name under the universe worlds directory (case-insensitive
 *                        at lookup time via {@code Universe.get().getWorld(name)})
 * @param schematicName   dot-notation prefab name passed to {@link
 *                        com.hypixel.hytale.server.core.prefab.selection.buffer.PrefabLoader}
 *                        (e.g. {@code "Server.Prefabs.Level1_Airplane"} resolves to
 *                        {@code Server/Prefabs/Level1_Airplane.prefab.json}).
 *                        {@code null} if this level has no POI schematic (e.g. the boss
 *                        arena, which is built entirely by worldgen).
 * @param schematicOrigin world-space block coordinates at which the schematic anchor
 *                        is placed. {@code null} when {@code schematicName} is null.
 * @param playerSpawn     world-space block coordinates where players spawn on entry
 */
public record LevelDefinition(
    LevelId id,
    String instanceName,
    @Nullable String schematicName,
    @Nullable Vector3i schematicOrigin,
    Vector3i playerSpawn
) {
    public boolean hasSchematic() {
        return schematicName != null;
    }
}
