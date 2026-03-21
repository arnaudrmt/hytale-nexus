package fr.arnaud.nexus.level;

import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.prefab.selection.buffer.BsonPrefabBufferDeserializer;
import com.hypixel.hytale.server.core.prefab.selection.buffer.PrefabLoader;
import com.hypixel.hytale.server.core.prefab.selection.buffer.impl.IPrefabBuffer;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.events.StartWorldEvent;
import com.hypixel.hytale.server.core.util.PrefabUtil;
import fr.arnaud.nexus.Nexus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

/**
 * Listens for {@link StartWorldEvent} and pastes the POI schematic for any
 * Nexus level world exactly once, when the world first starts.
 * <p>
 * The paste runs on the world thread via {@link World#execute} so that chunk
 * access is safe. Schematics are resolved using {@link PrefabLoader} with
 * dot-notation names and deserialized via {@link BsonPrefabBufferDeserializer}.
 * <p>
 * Registered as a global event listener in {@link fr.arnaud.nexus.Nexus#setup()}.
 */
public final class LevelWorldLoadSystem {

    private static final Random PASTE_RANDOM = new Random();

    // TODO: confirm BsonPrefabBufferDeserializer constructor signature once its source is available.
    // If the deserializer requires constructor arguments, instantiate it in the constructor
    // of this class and store it as a final field instead.
    private static final BsonPrefabBufferDeserializer DESERIALIZER = new BsonPrefabBufferDeserializer();

    public void onWorldStart(@Nonnull StartWorldEvent event) {
        World world = event.getWorld();
        String worldName = world.getName();

        for (LevelId levelId : LevelId.values()) {
            LevelDefinition definition = LevelRegistry.get(levelId);

            if (!definition.instanceName().equalsIgnoreCase(worldName)) continue;
            if (!definition.hasSchematic()) return;

            world.execute(() -> pasteSchematic(definition, world));
            return;
        }
    }

    private void pasteSchematic(@Nonnull LevelDefinition definition, @Nonnull World world) {
        IPrefabBuffer buffer = loadBuffer(definition);
        if (buffer == null) return;

        // Store<EntityStore> implements ComponentAccessor<EntityStore>, so it can be
        // passed directly to PrefabUtil.paste without any .access() wrapper.
        PrefabUtil.paste(
            buffer,
            world,
            definition.schematicOrigin(),
            Rotation.None,
            true,
            PASTE_RANDOM,
            world.getEntityStore().getStore()
        );
    }

    @Nullable
    private IPrefabBuffer loadBuffer(@Nonnull LevelDefinition definition) {
        // AssetModule.get().getBaseAssetPack().getRoot() is the non-deprecated replacement
        // for the removed AssetUtil.getHytaleAssetsPath().
        Path assetRoot = AssetModule.get().getBaseAssetPack().getRoot();
        PrefabLoader loader = new PrefabLoader(assetRoot);

        AtomicReference<IPrefabBuffer> result = new AtomicReference<>(null);

        try {
            loader.resolvePrefabs(definition.schematicName(), resolvedPath -> {
                try {
                    // TODO: confirm the second argument to deserialize() once
                    // BsonPrefabBufferDeserializer.java is available. The interface signature is
                    // deserialize(Path path, T context). Pass null if context is optional,
                    // or the appropriate context object if required.
                    IPrefabBuffer buffer = DESERIALIZER.deserialize(resolvedPath, null).newAccess();
                    result.set(buffer);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to deserialize prefab at: " + resolvedPath, e);
                }
            });
        } catch (IOException | RuntimeException e) {
            Nexus.get().getLogger().at(Level.SEVERE)
                 .log("Failed to load schematic '" + definition.schematicName()
                     + "' for level " + definition.id() + ": " + e.getMessage());
            return null;
        }

        if (result.get() == null) {
            Nexus.get().getLogger().at(Level.WARNING)
                 .log("Schematic not found on disk: '" + definition.schematicName()
                     + "' for level " + definition.id());
        }

        return result.get();
    }
}
