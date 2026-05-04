package fr.arnaud.nexus.spawner.chest;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import fr.arnaud.nexus.ability.core.ActiveCoreComponent;
import fr.arnaud.nexus.ability.core.CoreAbility;
import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.level.LevelConfig;
import fr.arnaud.nexus.level.LevelProgressComponent;
import fr.arnaud.nexus.math.WorldPosition;
import fr.arnaud.nexus.spawner.SpawnerState;
import fr.arnaud.nexus.spawner.loot.LootRoller;
import org.bson.BsonDocument;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ChestManager {

    private static final String CHEST_BLOCK_KEY = "Furniture_Dungeon_Chest_Epic";
    private static final String CHEST_OPEN_SOUND = "SFX_Chest_Legendary_FirstOpen_Player";
    private static final String CHEST_SPAWN_SOUND = "SFX_Portal_Neutral_Open.json";
    private static final String CHEST_AURA_PARTICLE = "Aura_Sphere";

    private final Supplier<World> activeWorldSupplier;
    private final Consumer<LevelProgressComponent> progressWriter;
    private final List<StandaloneChestState> standaloneChests = new ArrayList<>();

    public ChestManager(Supplier<World> activeWorldSupplier,
                        Consumer<LevelProgressComponent> progressWriter) {
        this.activeWorldSupplier = activeWorldSupplier;
        this.progressWriter = progressWriter;
    }

    public void onLevelLoaded(LevelConfig config) {
        standaloneChests.clear();
        int id = 0;
        for (LevelConfig.StandaloneChest chestConfig : config.standaloneChests()) {
            standaloneChests.add(new StandaloneChestState(id++, chestConfig));
        }
    }

    public void restoreFromProgress(LevelProgressComponent progress) {
        for (StandaloneChestState state : standaloneChests) {
            if (progress.openedStandaloneChestIndices.contains(state.getId())) {
                state.markTriggered();
                state.markChestSpawned();
                continue;
            }

            List<String> savedLoot = progress.pendingStandaloneChestLoot.get(state.getId());
            if (savedLoot == null || savedLoot.isEmpty()) continue;

            LevelConfig.StandaloneChest chestConfig = state.getConfig();
            state.markTriggered();
            state.markChestSpawned();
            state.setPendingLoot(new ArrayList<>(savedLoot));
            state.setChestPosition(new Vector3d(
                chestConfig.position().x(),
                chestConfig.position().y(),
                chestConfig.position().z()
            ));
            placeChestInWorld(state.getChestPosition());
        }
    }

    public void placeRestoredSpawnerChest(Vector3d chestPos) {
        placeChestInWorld(chestPos);
    }

    public void reset() {
        standaloneChests.clear();
    }

    public void tick(Vector3d playerPosition) {
        for (StandaloneChestState state : standaloneChests) {
            if (!state.isTriggered()) {
                checkStandaloneProximity(state, playerPosition);
            }
        }
    }

    public void spawnLootChest(SpawnerState spawnerState, LevelProgressComponent progress) {
        LevelConfig.LootChest lootChest = spawnerState.getConfig().lootChest();
        if (lootChest == null) return;

        List<String> savedLoot = progress.pendingSpawnerChestLoot.get(spawnerState.getId());
        List<String> rolledItems;
        if (savedLoot != null && !savedLoot.isEmpty()) {
            rolledItems = new ArrayList<>(savedLoot);
        } else {
            rolledItems = LootRoller.roll(lootChest.getItems());
            progress.recordSpawnerChestLoot(spawnerState.getId(), rolledItems);
            progressWriter.accept(progress);
        }

        WorldPosition pos = spawnerState.getConfig().position();
        Vector3d chestPos = new Vector3d(pos.x(), pos.y(), pos.z());

        spawnerState.setPendingChestLoot(rolledItems);
        spawnerState.setChestPosition(chestPos);

        placeChestInWorld(chestPos);
    }

    private void checkStandaloneProximity(StandaloneChestState state, Vector3d playerPosition) {
        WorldPosition pos = state.getConfig().position();
        double dx = playerPosition.getX() - pos.x();
        double dy = playerPosition.getY() - pos.y();
        double dz = playerPosition.getZ() - pos.z();
        float r = state.getConfig().activationRadius();

        if (dx * dx + dy * dy + dz * dz > r * r) return;

        state.markTriggered();
        spawnStandaloneChest(state);
    }

    private void spawnStandaloneChest(StandaloneChestState state) {
        if (state.isChestSpawned()) return;

        List<String> rolledItems = LootRoller.roll(state.getConfig().items());
        Vector3d chestPos = new Vector3d(
            state.getConfig().position().x(),
            state.getConfig().position().y(),
            state.getConfig().position().z()
        );

        state.markChestSpawned();
        state.setPendingLoot(rolledItems);
        state.setChestPosition(chestPos);

        standaloneChestLootPersister.accept(state.getId(), rolledItems);

        placeChestInWorld(chestPos);
    }

    private BiConsumer<Integer, List<String>> standaloneChestLootPersister = (_, _) -> {
    };

    public void setStandaloneChestLootPersister(BiConsumer<Integer, List<String>> persister) {
        this.standaloneChestLootPersister = persister;
    }

    private void placeChestInWorld(Vector3d chestPos) {
        World world = activeWorldSupplier.get();
        if (world == null) return;

        world.execute(() -> {
            Store<EntityStore> store = world.getEntityStore().getStore();
            playChestSpawnSoundToAll(world, store);
            world.setBlock(
                (int) Math.floor(chestPos.getX()),
                (int) Math.floor(chestPos.getY()),
                (int) Math.floor(chestPos.getZ()),
                CHEST_BLOCK_KEY
            );
            spawnAuraParticle(chestPos, world, store);
        });
    }

    private void playChestSpawnSoundToAll(World world, Store<EntityStore> store) {
        int soundIndex = SoundEvent.getAssetMap().getIndex(CHEST_SPAWN_SOUND);
        world.getPlayerRefs().forEach(playerRef -> {
            if (playerRef.getReference() == null) return;
            TransformComponent transform = store.getComponent(
                playerRef.getReference(), EntityModule.get().getTransformComponentType());
            if (transform == null) return;
            SoundUtil.playSoundEvent3dToPlayer(
                playerRef.getReference(), soundIndex, SoundCategory.UI,
                transform.getPosition(), store);
        });
    }

    private void spawnAuraParticle(Vector3d pos, World world, Store<EntityStore> store) {
        world.getPlayerRefs().forEach(_ ->
            ParticleUtil.spawnParticleEffect(
                CHEST_AURA_PARTICLE,
                new Vector3d((float) pos.getX(), (float) pos.getY(), (float) pos.getZ()),
                store)
        );
    }

    public boolean tryOpenChest(Vector3d clickedBlockCenter, Ref<EntityStore> playerRef,
                                Store<EntityStore> store, List<SpawnerState> spawnerStates,
                                LevelProgressComponent progress) {
        if (tryOpenFromCandidates(clickedBlockCenter, playerRef, store,
            buildSpawnerChestCandidates(spawnerStates, progress, playerRef, store))) return true;
        return tryOpenFromCandidates(clickedBlockCenter, playerRef, store,
            buildStandaloneChestCandidates(progress, playerRef, store));
    }

    private boolean tryOpenFromCandidates(Vector3d clicked, Ref<EntityStore> playerRef,
                                          Store<EntityStore> store, List<ChestCandidate> candidates) {
        for (ChestCandidate candidate : candidates) {
            if (!isSameBlock(clicked, candidate.position())) continue;

            candidate.clearAction().run();
            playChestOpenSound(playerRef, store);
            grantLoot(candidate.loot(), candidate.position(), playerRef, store);
            removeChestBlock(candidate.position());
            return true;
        }
        return false;
    }

    private List<ChestCandidate> buildSpawnerChestCandidates(List<SpawnerState> states,
                                                             LevelProgressComponent progress,
                                                             Ref<EntityStore> playerRef,
                                                             Store<EntityStore> store) {
        List<ChestCandidate> candidates = new ArrayList<>();
        for (SpawnerState state : states) {
            if (!state.hasPendingChestLoot() || state.getChestPosition() == null) continue;

            int spawnerIndex = state.getId();
            candidates.add(new ChestCandidate(
                state.getChestPosition(),
                state.getPendingChestLoot(),
                () -> {
                    state.clearPendingChestLoot();
                    progress.clearSpawnerChestLoot(spawnerIndex);
                    store.putComponent(playerRef, LevelProgressComponent.getComponentType(), progress);
                    Nexus.getInstance().getSpawnerManager().onChestOpened(spawnerIndex);
                }
            ));
        }
        return candidates;
    }

    private List<ChestCandidate> buildStandaloneChestCandidates(LevelProgressComponent progress,
                                                                Ref<EntityStore> playerRef,
                                                                Store<EntityStore> store) {
        List<ChestCandidate> candidates = new ArrayList<>();
        for (StandaloneChestState state : standaloneChests) {
            if (!state.hasPendingLoot() || state.getChestPosition() == null) continue;

            int chestIndex = state.getId();
            candidates.add(new ChestCandidate(
                state.getChestPosition(),
                state.getPendingLoot(),
                () -> {
                    state.clearPendingLoot();
                    progress.clearStandaloneChestLoot(chestIndex);
                    progress.recordStandaloneChestOpened(chestIndex);
                    store.putComponent(playerRef, LevelProgressComponent.getComponentType(), progress);
                }
            ));
        }
        return candidates;
    }

    private void grantLoot(List<String> itemIds, Vector3d origin,
                           Ref<EntityStore> playerRef, Store<EntityStore> store) {
        for (String itemId : itemIds) {
            if (isNexusCore(itemId)) {
                unlockCore(itemId, playerRef, store);
            } else {
                ejectItem(itemId, origin, store);
            }
        }
    }

    private void unlockCore(String itemId, Ref<EntityStore> playerRef, Store<EntityStore> store) {
        String abilityId = itemId.substring("Nexus_Core_".length()).toLowerCase();
        CoreAbility ability = CoreAbility.getAbilityFromId(abilityId);
        if (ability == null) return;

        ActiveCoreComponent core = store.getComponent(playerRef, ActiveCoreComponent.getComponentType());
        if (core == null) return;

        core.unlock(ability);
        store.putComponent(playerRef, ActiveCoreComponent.getComponentType(), core);

        PlayerRef playerRefComponent = store.getComponent(playerRef, PlayerRef.getComponentType());
        if (playerRefComponent != null) {
            EventTitleUtil.showEventTitleToPlayer(
                playerRefComponent,
                Message.translation("nexus.core.unlocked.title")
                       .param("ability", Message.translation(ability.getDisplayNameKey())),
                Message.translation("nexus.core.unlocked.subtitle"),
                true
            );
        }
    }

    private void ejectItem(String itemId, Vector3d origin, Store<EntityStore> store) {
        Random rng = ThreadLocalRandom.current();
        float velX = (rng.nextFloat() - 0.5f) * 1.2f;
        float velY = 0.4f + rng.nextFloat() * 0.5f;
        float velZ = (rng.nextFloat() - 0.5f) * 1.2f;

        ItemStack stack = buildLootStack(itemId);
        var holder = ItemComponent.generateItemDrop(store, stack, origin, Vector3f.ZERO, velX, velY, velZ);
        if (holder != null) {
            store.addEntity(holder, AddReason.SPAWN);
        }
    }

    private ItemStack buildLootStack(String itemId) {
        if (isNexusWeapon(itemId)) {
            Item item = Item.getAssetMap().getAsset(itemId);
            if (item != null) {
                BsonDocument doc = Nexus.getInstance().getWeaponGenerator().generateWeapon(item);
                if (doc != null) return new ItemStack(itemId, 1, doc);
            }
        }
        return new ItemStack(itemId, 1);
    }

    private void playChestOpenSound(Ref<EntityStore> playerRef, Store<EntityStore> store) {
        Player player = store.getComponent(playerRef, Player.getComponentType());
        if (player == null) return;
        World world = player.getWorld();
        if (world == null) return;

        int index = SoundEvent.getAssetMap().getIndex(CHEST_OPEN_SOUND);
        world.execute(() -> {
            TransformComponent transform = store.getComponent(
                playerRef, EntityModule.get().getTransformComponentType());
            if (transform == null) return;
            SoundUtil.playSoundEvent3dToPlayer(
                playerRef, index, SoundCategory.UI, transform.getPosition(), store);
        });
    }

    private void removeChestBlock(Vector3d pos) {
        World world = activeWorldSupplier.get();
        if (world == null) return;
        world.execute(() -> world.setBlock(
            (int) Math.floor(pos.getX()),
            (int) Math.floor(pos.getY()),
            (int) Math.floor(pos.getZ()),
            "Empty"
        ));
    }

    private static boolean isSameBlock(Vector3d a, Vector3d b) {
        return (int) Math.floor(a.getX()) == (int) Math.floor(b.getX())
            && (int) Math.floor(a.getY()) == (int) Math.floor(b.getY())
            && (int) Math.floor(a.getZ()) == (int) Math.floor(b.getZ());
    }

    private static boolean isNexusWeapon(String itemId) {
        return itemId.startsWith("Nexus_Melee_") || itemId.startsWith("Nexus_Ranged_");
    }

    private static boolean isNexusCore(String itemId) {
        return itemId.startsWith("Nexus_Core_");
    }

    @FunctionalInterface
    public interface BiConsumer<A, B> {
        void accept(A a, B b);
    }

    private record ChestCandidate(Vector3d position, List<String> loot, Runnable clearAction) {
    }
}
