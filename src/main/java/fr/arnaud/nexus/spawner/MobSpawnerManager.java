package fr.arnaud.nexus.spawner;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import fr.arnaud.nexus.input.PlayerInputListener;
import fr.arnaud.nexus.level.LevelConfig;
import fr.arnaud.nexus.level.LevelProgressComponent;
import fr.arnaud.nexus.spawner.loot.LootRoller;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public final class MobSpawnerManager {

    private static final String CHEST_BLOCK_KEY = "Furniture_Dungeon_Chest_Epic";

    private final List<SpawnerState> spawnerStates = new ArrayList<>();
    private World activeWorld;

    public MobSpawnerManager() {
    }

    public void onLevelLoaded(World world, LevelConfig config) {
        this.activeWorld = world;
        spawnerStates.clear();
        int idSequence = 0;
        for (LevelConfig.SpawnerConfig spawnerConfig : config.getSpawners()) {
            spawnerStates.add(new SpawnerState(idSequence++, spawnerConfig));
        }
    }

    public void reset() {
        spawnerStates.clear();
        activeWorld = null;
    }

    public List<SpawnerState> getSpawnerStates() {
        return Collections.unmodifiableList(spawnerStates);
    }

    public void tick(float dt, Vector3d position, LevelProgressComponent progress,
                     CommandBuffer<EntityStore> commandBuffer, Ref<EntityStore> playerRef) {
        if (activeWorld == null) return;

        for (SpawnerState state : spawnerStates) {
            if (!state.isTriggered() && progress != null
                && progress.triggeredSpawners.contains(state.getId())) {
                state.markTriggered();
            }

            if (!state.isTriggered()) {
                checkProximityTrigger(state, position, progress, commandBuffer, playerRef);
            } else {
                tickActiveSpawner(state, dt);
            }
        }
    }

    private void checkProximityTrigger(SpawnerState state, Vector3d position,
                                       LevelProgressComponent progress,
                                       CommandBuffer<EntityStore> commandBuffer,
                                       Ref<EntityStore> playerRef) {
        LevelConfig.Position pos = state.getConfig().getPosition();
        double dx = position.getX() - pos.getX();
        double dy = position.getY() - pos.getY();
        double dz = position.getZ() - pos.getZ();
        double distanceSq = dx * dx + dy * dy + dz * dz;
        float triggerRadius = state.getConfig().getTriggerRadius();

        if (distanceSq > triggerRadius * triggerRadius) return;

        state.markTriggered();

        if (progress != null) {
            progress.triggeredSpawners.add(state.getId());
            commandBuffer.putComponent(playerRef, LevelProgressComponent.getComponentType(), progress);
        }

        activateSpawner(state);
    }

    private void activateSpawner(SpawnerState state) {
        if (state.getConfig().hasWaves()) {
            advanceToWave(state, 1);
        } else {
            spawnAllMobsForWave(state, 0);
        }
    }

    private void tickActiveSpawner(SpawnerState state, float dt) {
        tickSpawnRateDrip(state, dt);

        if (!state.getConfig().hasWaves()) return;

        int currentWave = state.getActiveWave();
        LevelConfig.WaveConfig nextWaveConfig = findWaveConfig(state, currentWave + 1);
        if (nextWaveConfig == null) {
            checkFinalWaveCompletion(state);
            return;
        }

        switch (nextWaveConfig.getType()) {
            case TIME -> tickTimeWaveTransition(state, nextWaveConfig, dt);
            case KILL -> tickKillWaveTransition(state, nextWaveConfig, dt);
        }
    }

    private void checkFinalWaveCompletion(SpawnerState state) {
        if (state.isChestSpawned()) return;
        if (state.getAliveMobsInCurrentWave() > 0) return;
        if (state.getTotalMobsInCurrentWave() == 0) return;
        if (!state.getConfig().hasLootChest()) return;

        state.markChestSpawned();
        spawnLootChest(state);
    }

    private void spawnLootChest(SpawnerState state) {
        List<String> rolledItems = LootRoller.roll(state.getConfig().getLootChest());
        LevelConfig.Position pos = state.getConfig().getPosition();
        Vector3d chestPos = new Vector3d(pos.getX(), pos.getY(), pos.getZ());

        state.setPendingChestLoot(rolledItems);
        state.setChestPosition(chestPos);

        activeWorld.execute(() -> {

            EntityStore store = activeWorld.getEntityStore();
            int index = SoundEvent.getAssetMap().getIndex("SFX_Portal_Neutral_Open.json");

            activeWorld.getPlayerRefs().forEach(playerRef -> {
                TransformComponent transform = store.getStore().getComponent(playerRef.getReference(), EntityModule.get().getTransformComponentType());
                SoundUtil.playSoundEvent3dToPlayer(playerRef.getReference(), index, SoundCategory.UI, transform.getPosition(), store.getStore());
            });

            int x = (int) Math.floor(chestPos.getX());
            int y = (int) Math.floor(chestPos.getY());
            int z = (int) Math.floor(chestPos.getZ());
            activeWorld.setBlock(x, y, z, CHEST_BLOCK_KEY);
        });
    }

    /**
     * Called by {@link PlayerInputListener} when a player left-clicks
     * a block. Checks if the clicked position matches any pending chest, ejects the loot
     * as flying item drops, and breaks the chest block.
     *
     * @return true if the click was consumed by a chest interaction
     */
    public boolean tryOpenChest(Vector3d clickedBlockCenter, Ref<EntityStore> playerRef,
                                Store<EntityStore> store) {
        for (SpawnerState state : spawnerStates) {
            if (!state.hasPendingChestLoot()) continue;

            Vector3d chestPos = state.getChestPosition();
            if (chestPos == null) continue;

            if (!isSameBlock(clickedBlockCenter, chestPos)) continue;

            List<String> loot = state.getPendingChestLoot();
            state.clearPendingChestLoot();

            int index = SoundEvent.getAssetMap().getIndex("SFX_Chest_Legendary_FirstOpen_Player");
            Player player = store.getComponent(playerRef, Player.getComponentType());
            World world = player.getWorld();

            world.execute(() -> {
                TransformComponent transform = store.getComponent(playerRef, EntityModule.get().getTransformComponentType());
                SoundUtil.playSoundEvent3dToPlayer(playerRef, index, SoundCategory.UI, transform.getPosition(), store);
            });

            ejectItems(loot, chestPos, playerRef, store);
            breakChestBlock(chestPos);
            return true;
        }
        return false;
    }

    private void ejectItems(List<String> itemIds, Vector3d origin,
                            Ref<EntityStore> playerRef, Store<EntityStore> store) {
        Random rng = ThreadLocalRandom.current();

        for (String itemId : itemIds) {
            float velX = (rng.nextFloat() - 0.5f) * 1.2f;
            float velY = 0.4f + rng.nextFloat() * 0.5f;
            float velZ = (rng.nextFloat() - 0.5f) * 1.2f;

            com.hypixel.hytale.server.core.inventory.ItemStack itemStack = buildLootStack(itemId);

            com.hypixel.hytale.component.Holder<EntityStore> itemHolder =
                com.hypixel.hytale.server.core.modules.entity.item.ItemComponent
                    .generateItemDrop(store, itemStack, origin, Vector3f.ZERO, velX, velY, velZ);

            if (itemHolder != null) {
                store.addEntity(itemHolder, com.hypixel.hytale.component.AddReason.SPAWN);
            }
        }
    }

    private com.hypixel.hytale.server.core.inventory.ItemStack buildLootStack(String itemId) {
        if (isNexusWeapon(itemId)) {
            com.hypixel.hytale.server.core.asset.type.item.config.Item item =
                com.hypixel.hytale.server.core.asset.type.item.config.Item
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

    private void breakChestBlock(Vector3d chestPos) {
        activeWorld.execute(() -> {
            int x = (int) Math.floor(chestPos.getX());
            int y = (int) Math.floor(chestPos.getY());
            int z = (int) Math.floor(chestPos.getZ());
            activeWorld.setBlock(x, y, z, "Empty");
        });
    }

    private static boolean isSameBlock(Vector3d a, Vector3d b) {
        return (int) Math.floor(a.getX()) == (int) Math.floor(b.getX())
            && (int) Math.floor(a.getY()) == (int) Math.floor(b.getY())
            && (int) Math.floor(a.getZ()) == (int) Math.floor(b.getZ());
    }

    private void tickTimeWaveTransition(SpawnerState state, LevelConfig.WaveConfig nextWave, float dt) {
        state.addWaveTimer(dt);
        if (state.getWaveTimer() >= nextWave.getValue()) {
            advanceToWave(state, nextWave.getWave());
        }
    }

    private void tickKillWaveTransition(SpawnerState state, LevelConfig.WaveConfig nextWave, float dt) {
        int total = state.getTotalMobsInCurrentWave();
        if (total == 0) return;

        int alive = state.getAliveMobsInCurrentWave();
        float killedRatio = (total - alive) / (float) total;

        boolean thresholdReached = killedRatio >= nextWave.getValue();
        boolean timedOut = nextWave.getTimeout() > 0f
            && state.getKillWaveTimeoutTimer() >= nextWave.getTimeout();

        if (thresholdReached || timedOut) {
            advanceToWave(state, nextWave.getWave());
            return;
        }

        state.addKillWaveTimeoutTimer(dt);
    }

    private void advanceToWave(SpawnerState state, int waveIndex) {
        state.resetForNewWave();
        state.setActiveWave(waveIndex);
        spawnAllMobsForWave(state, waveIndex);
    }

    private void spawnAllMobsForWave(SpawnerState state, int waveIndex) {
        List<LevelConfig.MobEntry> entries = state.getConfig().getMobs();
        int totalForWave = 0;

        for (int i = 0; i < entries.size(); i++) {
            LevelConfig.MobEntry entry = entries.get(i);
            if (entry.getWave() != waveIndex) continue;

            int count = rollCount(entry);
            totalForWave += count;

            if (entry.getSpawnRate() == 0f) {
                spawnMobBatch(state, entry, count);
            } else {
                state.setPendingSpawns(i, count);
            }
        }

        state.setTotalMobsInCurrentWave(state.getTotalMobsInCurrentWave() + totalForWave);
        state.setAliveMobsInCurrentWave(state.getAliveMobsInCurrentWave() + totalForWave);
    }

    private void tickSpawnRateDrip(SpawnerState state, float dt) {
        List<LevelConfig.MobEntry> entries = state.getConfig().getMobs();

        for (int i = 0; i < entries.size(); i++) {
            LevelConfig.MobEntry entry = entries.get(i);
            int pending = state.getPendingSpawns(i);
            if (pending <= 0) continue;

            state.addSpawnRateAccumulator(i, dt);

            if (state.getSpawnRateAccumulator(i) >= entry.getSpawnRate()) {
                state.resetSpawnRateAccumulator(i);
                spawnMobBatch(state, entry, 1);
                state.decrementPendingSpawns(i);
            }
        }
    }

    private void spawnMobBatch(SpawnerState state, LevelConfig.MobEntry entry, int count) {
        activeWorld.execute(() -> {
            Store<EntityStore> store = activeWorld.getEntityStore().getStore();

            for (int i = 0; i < count; i++) {
                Vector3d position = randomPositionAround(state.getConfig());
                var spawnedPair = NPCPlugin.get().spawnNPC(store, entry.getMobId(), null, position, Vector3f.ZERO);

                if (spawnedPair != null) {
                    var npcRef = spawnedPair.first();
                    if (npcRef != null && npcRef.isValid()) {
                        store.addComponent(
                            npcRef,
                            SpawnerTagComponent.getComponentType(),
                            new SpawnerTagComponent(state.getId(), entry.getMinEssence(), entry.getMaxEssence())
                        );
                    }
                }
            }
        });
    }

    private Vector3d randomPositionAround(LevelConfig.SpawnerConfig config) {
        Random rng = ThreadLocalRandom.current();
        float radius = config.getSpawnRadius();
        double angle = rng.nextDouble() * 2.0 * Math.PI;
        double distance = rng.nextDouble() * radius;

        return new Vector3d(
            config.getPosition().getX() + Math.cos(angle) * distance,
            config.getPosition().getY(),
            config.getPosition().getZ() + Math.sin(angle) * distance
        );
    }

    public void onMobDied(int spawnerId) {
        for (SpawnerState state : spawnerStates) {
            if (state.getId() == spawnerId) {
                state.decrementAliveMobs();
                break;
            }
        }
    }

    @Nullable
    private LevelConfig.WaveConfig findWaveConfig(SpawnerState state, int waveIndex) {
        for (LevelConfig.WaveConfig wc : state.getConfig().getWaves()) {
            if (wc.getWave() == waveIndex) return wc;
        }
        return null;
    }

    private int rollCount(LevelConfig.MobEntry entry) {
        if (entry.getMinCount() == entry.getMaxCount()) return entry.getMinCount();
        return entry.getMinCount()
            + ThreadLocalRandom.current().nextInt(entry.getMaxCount() - entry.getMinCount() + 1);
    }
}
