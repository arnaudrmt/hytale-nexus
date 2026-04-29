package fr.arnaud.nexus.spawner;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import fr.arnaud.nexus.ability.ActiveCoreComponent;
import fr.arnaud.nexus.ability.CoreAbility;
import fr.arnaud.nexus.level.LevelConfig;
import fr.arnaud.nexus.spawner.loot.LootRoller;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages all loot chest instances — both spawner-attached and independent.
 */
public final class ChestManager {

    private static final String CHEST_BLOCK_KEY = "Furniture_Dungeon_Chest_Epic";

    private final List<IndependentChestState> independentChests = new ArrayList<>();
    private World activeWorld;

    public void onLevelLoaded(World world, LevelConfig config) {
        this.activeWorld = world;
        independentChests.clear();
        int id = 0;
        for (LevelConfig.IndependentChestConfig chestConfig : config.getIndependentChests()) {
            independentChests.add(new IndependentChestState(id++, chestConfig));
        }
    }

    public void reset() {
        independentChests.clear();
        activeWorld = null;
    }

    public void tick(Vector3d playerPosition) {
        if (activeWorld == null) return;
        for (IndependentChestState state : independentChests) {
            if (!state.isTriggered()) {
                checkProximityTrigger(state, playerPosition);
            }
        }
    }

    private void checkProximityTrigger(IndependentChestState state, Vector3d playerPosition) {
        LevelConfig.Position pos = state.getConfig().getPosition();
        double dx = playerPosition.getX() - pos.getX();
        double dy = playerPosition.getY() - pos.getY();
        double dz = playerPosition.getZ() - pos.getZ();
        float r = state.getConfig().getTriggerRadius();

        if (dx * dx + dy * dy + dz * dz > r * r) return;

        state.markTriggered();
        spawnChest(state);
    }

    private void spawnChest(IndependentChestState state) {
        if (state.isChestSpawned()) return;

        LevelConfig.IndependentChestConfig config = state.getConfig();
        LootChestConfig wrapper = new LootChestConfig(config.getItems());
        List<String> rolledItems = LootRoller.roll(wrapper);

        Vector3d chestPos = new Vector3d(
            config.getPosition().getX(),
            config.getPosition().getY(),
            config.getPosition().getZ()
        );

        state.markChestSpawned();
        state.setPendingLoot(rolledItems);
        state.setChestPosition(chestPos);

        activeWorld.execute(() -> {
            Store<EntityStore> store = activeWorld.getEntityStore().getStore();
            int soundIndex = SoundEvent.getAssetMap().getIndex("SFX_Portal_Neutral_Open.json");

            activeWorld.getPlayerRefs().forEach(playerRef -> {
                TransformComponent transform = store.getComponent(
                    playerRef.getReference(), EntityModule.get().getTransformComponentType());
                SoundUtil.playSoundEvent3dToPlayer(
                    playerRef.getReference(), soundIndex, SoundCategory.UI,
                    transform.getPosition(), store);
            });

            activeWorld.setBlock(
                (int) Math.floor(chestPos.getX()),
                (int) Math.floor(chestPos.getY()),
                (int) Math.floor(chestPos.getZ()),
                CHEST_BLOCK_KEY
            );
            spawnAuraSphere(chestPos);
        });
    }

    public boolean tryOpenChest(Vector3d clickedBlockCenter, Ref<EntityStore> playerRef,
                                Store<EntityStore> store, List<SpawnerState> spawnerStates) {
        if (tryOpenFromList(clickedBlockCenter, playerRef, store,
            collectSpawnerChestCandidates(spawnerStates))) return true;
        return tryOpenFromList(clickedBlockCenter, playerRef, store,
            collectIndependentChestCandidates());
    }

    private void grantLoot(List<String> itemIds, Vector3d origin,
                           Ref<EntityStore> playerRef, Store<EntityStore> store) {
        for (String itemId : itemIds) {
            if (isNexusCore(itemId)) {
                unlockCore(itemId, playerRef, store);
            } else {
                ejectSingleItem(itemId, origin, store);
            }
        }
    }

    private void unlockCore(String itemId, Ref<EntityStore> playerRef, Store<EntityStore> store) {
        String abilityId = itemId.substring("Nexus_Core_".length()).toLowerCase();
        CoreAbility ability = CoreAbility.fromId(abilityId);
        if (ability == null) return;

        ActiveCoreComponent core = store.getComponent(playerRef, ActiveCoreComponent.getComponentType());
        if (core == null) return;

        core.unlock(ability);
        store.putComponent(playerRef, ActiveCoreComponent.getComponentType(), core);

        PlayerRef playerRefComponent = store.getComponent(playerRef, PlayerRef.getComponentType());
        if (playerRefComponent != null) {
            EventTitleUtil.showEventTitleToPlayer(
                playerRefComponent,
                Message.translation("nexus.core.unlocked.title").param("ability", ability.getDisplayName()),
                Message.translation("nexus.core.unlocked.subtitle"),
                true
            );
        }
    }

    private void ejectSingleItem(String itemId, Vector3d origin, Store<EntityStore> store) {
        Random rng = ThreadLocalRandom.current();
        float velX = (rng.nextFloat() - 0.5f) * 1.2f;
        float velY = 0.4f + rng.nextFloat() * 0.5f;
        float velZ = (rng.nextFloat() - 0.5f) * 1.2f;

        com.hypixel.hytale.server.core.inventory.ItemStack stack = buildLootStack(itemId);
        var holder = com.hypixel.hytale.server.core.modules.entity.item.ItemComponent
            .generateItemDrop(store, stack, origin, Vector3f.ZERO, velX, velY, velZ);

        if (holder != null) {
            store.addEntity(holder, AddReason.SPAWN);
        }
    }

    private List<ChestCandidate> collectSpawnerChestCandidates(List<SpawnerState> states) {
        List<ChestCandidate> candidates = new ArrayList<>();
        for (SpawnerState state : states) {
            if (state.hasPendingChestLoot() && state.getChestPosition() != null) {
                candidates.add(new ChestCandidate(
                    state.getChestPosition(),
                    state.getPendingChestLoot(),
                    state::clearPendingChestLoot
                ));
            }
        }
        return candidates;
    }

    private List<ChestCandidate> collectIndependentChestCandidates() {
        List<ChestCandidate> candidates = new ArrayList<>();
        for (IndependentChestState state : independentChests) {
            if (state.hasPendingLoot() && state.getChestPosition() != null) {
                candidates.add(new ChestCandidate(
                    state.getChestPosition(),
                    state.getPendingLoot(),
                    state::clearPendingLoot
                ));
            }
        }
        return candidates;
    }

    private boolean tryOpenFromList(Vector3d clicked, Ref<EntityStore> playerRef,
                                    Store<EntityStore> store, List<ChestCandidate> candidates) {
        for (ChestCandidate candidate : candidates) {
            if (!isSameBlock(clicked, candidate.position())) continue;

            List<String> loot = candidate.loot();
            candidate.clearAction().run();

            playOpenSound(playerRef, store);
            grantLoot(loot, candidate.position(), playerRef, store);
            breakChestBlock(candidate.position());
            return true;
        }
        return false;
    }

    private void playOpenSound(Ref<EntityStore> playerRef, Store<EntityStore> store) {
        int index = SoundEvent.getAssetMap().getIndex("SFX_Chest_Legendary_FirstOpen_Player");
        Player player = store.getComponent(playerRef, Player.getComponentType());
        World world = player.getWorld();
        world.execute(() -> {
            TransformComponent transform = store.getComponent(
                playerRef, EntityModule.get().getTransformComponentType());
            SoundUtil.playSoundEvent3dToPlayer(
                playerRef, index, SoundCategory.UI, transform.getPosition(), store);
        });
    }

    private com.hypixel.hytale.server.core.inventory.ItemStack buildLootStack(String itemId) {
        if (isNexusWeapon(itemId)) {
            var item = com.hypixel.hytale.server.core.asset.type.item.config.Item
                .getAssetMap().getAsset(itemId);
            if (item != null) {
                org.bson.BsonDocument doc = fr.arnaud.nexus.core.Nexus.get()
                                                                      .getWeaponGenerator().generateWeapon(item);
                if (doc != null) {
                    return new com.hypixel.hytale.server.core.inventory.ItemStack(itemId, 1, doc);
                }
            }
        }

        return new com.hypixel.hytale.server.core.inventory.ItemStack(itemId, 1);
    }

    private static boolean isNexusWeapon(String itemId) {
        return itemId.startsWith("Nexus_Melee_") || itemId.startsWith("Nexus_Ranged_");
    }

    private static boolean isNexusCore(String itemId) {
        return itemId.startsWith("Nexus_Core_");
    }

    private void breakChestBlock(Vector3d pos) {
        activeWorld.execute(() -> activeWorld.setBlock(
            (int) Math.floor(pos.getX()),
            (int) Math.floor(pos.getY()),
            (int) Math.floor(pos.getZ()),
            "Empty"
        ));
    }

    void spawnAuraSphere(Vector3d pos) {
        String particleName = "Aura_Sphere";

        activeWorld.getPlayerRefs().forEach(playerRef -> {
            com.hypixel.hytale.server.core.universe.world.ParticleUtil
                .spawnParticleEffect(
                    particleName,
                    new Vector3d((float) pos.getX(), (float) pos.getY(), (float) pos.getZ()),
                    activeWorld.getEntityStore().getStore());
        });
    }

    private static boolean isSameBlock(Vector3d a, Vector3d b) {
        return (int) Math.floor(a.getX()) == (int) Math.floor(b.getX())
            && (int) Math.floor(a.getY()) == (int) Math.floor(b.getY())
            && (int) Math.floor(a.getZ()) == (int) Math.floor(b.getZ());
    }

    private static final class LootChestConfig extends LevelConfig.LootChestConfig {
        LootChestConfig(List<LevelConfig.LootChestItem> items) {
            super(items);
        }
    }

    private record ChestCandidate(Vector3d position, List<String> loot, Runnable clearAction) {
    }
}
