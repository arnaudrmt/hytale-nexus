package fr.arnaud.nexus.level;

import com.hypixel.hytale.builtin.instances.InstancesPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.ability.ActiveCoreComponent;
import fr.arnaud.nexus.camera.CameraPacketBuilder;
import fr.arnaud.nexus.component.RunSessionComponent;
import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.feature.resource.PlayerStatsManager;
import fr.arnaud.nexus.item.weapon.component.PlayerWeaponStateComponent;
import fr.arnaud.nexus.item.weapon.component.WeaponInstanceComponent;
import fr.arnaud.nexus.item.weapon.data.WeaponTag;
import fr.arnaud.nexus.spawner.SpawnerTagComponent;
import org.bson.BsonDocument;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class NewRunService {

    private static final String STARTING_LEVEL_ID = "level_1";

    public void startNewRun(Ref<EntityStore> playerRef, Store<EntityStore> store, World world) {
        Nexus.get().getNexusWorldLoadSystem()
             .spawnLevelWorld(STARTING_LEVEL_ID, world)
             .thenAccept(nextWorld -> {
                 if (nextWorld == null) return;

                 LevelConfig config = LevelConfigLoader.loadAndParseLevelConfig(STARTING_LEVEL_ID);
                 if (config == null) return;

                 LevelConfig.Position spawn = config.getSpawnPoint();
                 Transform spawnTransform = new Transform(
                     new Vector3d(spawn.getX(), spawn.getY(), spawn.getZ()),
                     new Vector3f(0f, CameraPacketBuilder.ISO_YAW_RAD, 0f)
                 );

                 world.execute(() -> {
                     if (!playerRef.isValid()) return;
                     Store<EntityStore> s = world.getEntityStore().getStore();
                     resetPlayerComponents(playerRef, s, config);
                     Nexus.get().getNexusWorldLoadSystem().activateLevelWorld(nextWorld, STARTING_LEVEL_ID);

                     if (nextWorld == world) {
                         s.addComponent(playerRef,
                             com.hypixel.hytale.server.core.modules.entity.teleport.Teleport.getComponentType(),
                             com.hypixel.hytale.server.core.modules.entity.teleport.Teleport.createForPlayer(
                                 new com.hypixel.hytale.math.vector.Vector3d(spawn.getX(), spawn.getY(), spawn.getZ()),
                                 new com.hypixel.hytale.math.vector.Vector3f(0f, fr.arnaud.nexus.camera.CameraPacketBuilder.ISO_YAW_RAD, 0f)
                             ));
                     } else {
                         InstancesPlugin.teleportPlayerToLoadingInstance(
                             playerRef, s,
                             CompletableFuture.completedFuture(nextWorld),
                             spawnTransform
                         );
                     }

                     world.execute(() -> {
                         s.forEachChunk(
                             SpawnerTagComponent.getComponentType(),
                             (chunk, cmd) -> {
                                 for (int i = 0; i < chunk.size(); i++) {
                                     Ref<EntityStore> ref = chunk.getReferenceTo(i);
                                     if (ref.isValid()) cmd.removeEntity(ref, RemoveReason.REMOVE);
                                 }
                             }
                         );
                     });
                 });
             });
    }

    private void resetPlayerComponents(Ref<EntityStore> playerRef,
                                       Store<EntityStore> store,
                                       LevelConfig config) {
        resetSession(playerRef, store);
        resetWeapons(playerRef, store);
        resetInventory(playerRef, store);
        resetCores(playerRef, store);
        resetEssence(playerRef, store);
        resetStats(playerRef, store);
        teleportToSpawn(playerRef, store, config);
    }

    private void resetSession(Ref<EntityStore> playerRef, Store<EntityStore> store) {
        RunSessionComponent session = store.getComponent(
            playerRef, RunSessionComponent.getComponentType());
        boolean tutorialDone = session != null && session.isTutorialCompleted();
        RunSessionComponent fresh = new RunSessionComponent();
        fresh.markTutorialCompleted(tutorialDone);
        store.putComponent(playerRef, RunSessionComponent.getComponentType(), fresh);

        LevelProgressComponent progress = store.getComponent(
            playerRef, LevelProgressComponent.getComponentType());
        if (progress != null) {
            progress.triggeredSpawners.clear();
            progress.completedSpawners.clear();
            progress.checkpointX = Float.NaN;
            progress.checkpointY = Float.NaN;
            progress.checkpointZ = Float.NaN;
            store.putComponent(playerRef, LevelProgressComponent.getComponentType(), progress);
        }
    }

    private void resetWeapons(Ref<EntityStore> playerRef, Store<EntityStore> store) {
        WeaponInstanceComponent existing = store.getComponent(
            playerRef, WeaponInstanceComponent.getComponentType());
        if (existing != null) {
            Nexus.get().getWeaponEquipSystem().onWeaponUnequipped(playerRef, store);
        }

        BsonDocument meleeDoc = generateDefaultWeapon("Nexus_Melee_Sword_Default");
        BsonDocument rangedDoc = generateDefaultWeapon("Nexus_Ranged_Staff_Default");

        PlayerWeaponStateComponent state = new PlayerWeaponStateComponent();
        state.meleeDocument = meleeDoc;
        state.rangedDocument = rangedDoc;
        state.activeTag = WeaponTag.MELEE;
        store.putComponent(playerRef, PlayerWeaponStateComponent.getComponentType(), state);

        InventoryComponent.Hotbar hotbar = store.getComponent(
            playerRef, InventoryComponent.Hotbar.getComponentType());
        if (hotbar != null) {
            hotbar.getInventory().setItemStackForSlot(
                (short) 0, new ItemStack("Nexus_Melee_Sword_Default", 1, meleeDoc));
            hotbar.markDirty();
        }

        Store<EntityStore> s = store;
        store.getExternalData().getWorld().execute(() -> {
            if (!playerRef.isValid()) return;
            Nexus.get().getWeaponEquipSystem().onWeaponEquipped(
                playerRef,
                new ItemStack("Nexus_Melee_Sword_Default", 1, meleeDoc),
                s);
        });
    }

    private BsonDocument generateDefaultWeapon(String archetypeId) {
        Item item = Item.getAssetMap().getAsset(archetypeId);
        return Nexus.get().getWeaponGenerator().generateWeapon(item);
    }

    private void resetInventory(Ref<EntityStore> playerRef, Store<EntityStore> store) {
        InventoryComponent.Storage storage = store.getComponent(
            playerRef, InventoryComponent.Storage.getComponentType());
        if (storage != null) {
            for (short i = 0; i < storage.getInventory().getCapacity(); i++) {
                storage.getInventory().setItemStackForSlot(i, ItemStack.EMPTY);
            }
        }

        InventoryComponent.Hotbar hotbar = store.getComponent(
            playerRef, InventoryComponent.Hotbar.getComponentType());
        if (hotbar != null) {
            for (short i = 1; i < hotbar.getInventory().getCapacity(); i++) {
                hotbar.getInventory().setItemStackForSlot(i, ItemStack.EMPTY);
            }
            hotbar.markDirty();
        }
    }

    private void resetCores(Ref<EntityStore> playerRef, Store<EntityStore> store) {
        ActiveCoreComponent core = store.getComponent(
            playerRef, ActiveCoreComponent.getComponentType());
        if (core == null) return;

        store.putComponent(playerRef, ActiveCoreComponent.getComponentType(),
            new ActiveCoreComponent());
    }

    private void resetEssence(Ref<EntityStore> playerRef, Store<EntityStore> store) {
        PlayerStatsManager psm = Nexus.get().getPlayerStatsManager();
        if (!psm.isReady()) return;
        float current = psm.getEssenceDust(playerRef, store);
        if (current > 0f) psm.removeEssenceDust(playerRef, store, current);
    }

    private void resetStats(Ref<EntityStore> playerRef, Store<EntityStore> store) {
        PlayerStatsManager psm = Nexus.get().getPlayerStatsManager();
        if (!psm.isReady()) return;

        EntityStatMap stats = store.getComponent(playerRef, EntityStatMap.getComponentType());
        if (stats == null) return;

        resetStatToBase(stats, psm.getHealthIndex());
        resetStatToBase(stats, psm.getStaminaIndex());
        resetMovementSpeed(playerRef, store);
    }

    private void resetStatToBase(EntityStatMap stats, int index) {
        if (index == Integer.MIN_VALUE) return;
        EntityStatValue stat = stats.get(index);
        if (stat == null) return;

        Map<String, Modifier> modifiers = stat.getModifiers();
        if (modifiers != null) {
            new java.util.ArrayList<>(modifiers.keySet())
                .forEach(key -> stats.removeModifier(
                    EntityStatMap.Predictable.NONE, index, key));
        }

        EntityStatType asset = EntityStatType.getAssetMap().getAsset(index);
        if (asset != null) {
            stats.setStatValue(EntityStatMap.Predictable.NONE, index, asset.getInitialValue());
        }
    }

    private void resetMovementSpeed(Ref<EntityStore> playerRef, Store<EntityStore> store) {
        MovementManager mm = store.getComponent(playerRef, MovementManager.getComponentType());
        if (mm == null) return;
        float defaultSpeed = mm.getDefaultSettings().baseSpeed;
        mm.getSettings().baseSpeed = defaultSpeed;
        PlayerRef playerRef2 = store.getComponent(playerRef, PlayerRef.getComponentType());
        if (playerRef2 != null) mm.update(playerRef2.getPacketHandler());
    }

    private void teleportToSpawn(Ref<EntityStore> playerRef, Store<EntityStore> store,
                                 LevelConfig config) {
        LevelConfig.Position spawn = config.getSpawnPoint();
        Vector3d spawnPos = new Vector3d(spawn.getX(), spawn.getY(), spawn.getZ());
        store.addComponent(playerRef, Teleport.getComponentType(),
            Teleport.createForPlayer(spawnPos, new Vector3f(0f, CameraPacketBuilder.ISO_YAW_RAD, 0f)));
    }
}
