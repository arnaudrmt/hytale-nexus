package fr.arnaud.nexus.handler;

import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap.Predictable;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class LucidityHandler {

    private int lucidityIndex = Integer.MIN_VALUE;

    public void onAssetsLoaded(LoadedAssetsEvent<String, EntityStatType, IndexedLookupTableAssetMap<String, EntityStatType>> event) {
        lucidityIndex = event.getAssetMap().getIndex("Lucidity");
    }

    /**
     * Returns true once asset indices have been resolved. False means onAssetsLoaded never fired.
     */
    public boolean isReady() {
        return lucidityIndex != Integer.MIN_VALUE;
    }

    /**
     * Index assigned by the Asset Editor — MIN_VALUE means unresolved.
     */
    public int getLucidityIndex() {
        return lucidityIndex;
    }

    // --- Mutations ---

    public void addLucidity(Ref<EntityStore> player, Store<EntityStore> store, float amount) {
        addStat(player, store, amount);
    }

    public void removeLucidity(Ref<EntityStore> player, Store<EntityStore> store, float amount) {
        subtractStat(player, store, amount);
    }

    // --- Queries ---

    public boolean isDepleted(Ref<EntityStore> player, Store<EntityStore> store) {
        EntityStatValue lucidity = getStatValue(player, store);
        return lucidity != null && lucidity.get() <= 0f;
    }

    /**
     * 0.0–1.0 fraction of current lucidity relative to max.
     */
    public float getNormalized(Ref<EntityStore> player, Store<EntityStore> store) {
        EntityStatValue lucidity = getStatValue(player, store);
        return lucidity != null ? lucidity.asPercentage() : 0f;
    }

    /**
     * Score multiplier scaled linearly from lucidity: 1.0x at empty, 3.0x at full.
     */
    public float getScoreMultiplier(Ref<EntityStore> player, Store<EntityStore> store) {
        EntityStatValue lucidity = getStatValue(player, store);
        float normalized = lucidity != null ? lucidity.asPercentage() : 0f;
        return 1.0f + normalized * 2.0f;
    }

    // --- Internals ---

    private void addStat(Ref<EntityStore> ref, Store<EntityStore> store,
                         float amount) {
        EntityStatMap stats = getStats(ref, store);
        if (stats == null || lucidityIndex == Integer.MIN_VALUE) return;
        stats.addStatValue(Predictable.ALL, lucidityIndex, amount);
    }

    private void subtractStat(Ref<EntityStore> ref, Store<EntityStore> store,
                              float amount) {
        EntityStatMap stats = getStats(ref, store);
        if (stats == null || lucidityIndex == Integer.MIN_VALUE) return;
        stats.subtractStatValue(Predictable.ALL, lucidityIndex, amount);
    }

    private EntityStatValue getStatValue(Ref<EntityStore> ref, Store<EntityStore> store) {
        EntityStatMap stats = getStats(ref, store);
        if (stats == null || lucidityIndex == Integer.MIN_VALUE) return null;
        return stats.get(lucidityIndex);
    }

    private EntityStatMap getStats(Ref<EntityStore> ref, Store<EntityStore> store) {
        return store.getComponent(ref, EntityStatMap.getComponentType());
    }
}
