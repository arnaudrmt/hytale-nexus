package fr.arnaud.nexus.core;

import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap.Predictable;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class FlowHandler {

    private int flowIndex = Integer.MIN_VALUE;
    private int flowCapacityIndex = Integer.MIN_VALUE;

    public void onAssetsLoaded(LoadedAssetsEvent<String, EntityStatType, IndexedLookupTableAssetMap<String, EntityStatType>> event) {
        IndexedLookupTableAssetMap<String, EntityStatType> map = event.getAssetMap();
        flowIndex = map.getIndex("Flow");
        flowCapacityIndex = map.getIndex("FlowSegmentCapacity");
    }

    /**
     * Returns true once asset indices have been resolved. False means onAssetsLoaded never fired.
     */
    public boolean isReady() {
        return flowIndex != Integer.MIN_VALUE && flowCapacityIndex != Integer.MIN_VALUE;
    }

    /**
     * Index assigned by the Asset Editor — MIN_VALUE means unresolved.
     */
    public int getFlowIndex() {
        return flowIndex;
    }

    public int getFlowCapacityIndex() {
        return flowCapacityIndex;
    }

    // --- Raw flow ---

    public void addFlow(Ref<EntityStore> player, Store<EntityStore> store, float amount) {
        addStat(player, store, flowIndex, amount);
    }

    public void removeFlow(Ref<EntityStore> player, Store<EntityStore> store, float amount) {
        subtractStat(player, store, flowIndex, amount);
    }

    public void drainFlow(Ref<EntityStore> player, Store<EntityStore> store) {
        minimizeStat(player, store, flowIndex);
    }

    // --- Segment operations ---

    public void addSegments(Ref<EntityStore> player, Store<EntityStore> store, int count) {
        addStat(player, store, flowIndex, count * segmentSize(player, store));
    }

    /**
     * Drains {@code count} whole segments. Returns false without mutating state
     * if the player does not have enough filled segments.
     */
    public boolean drainSegments(Ref<EntityStore> player, Store<EntityStore> store, int count) {
        if (getFilledSegments(player, store) < count) return false;
        subtractStat(player, store, flowIndex, count * segmentSize(player, store));
        return true;
    }

    // --- Queries ---

    public boolean isFull(Ref<EntityStore> player, Store<EntityStore> store) {
        EntityStatValue flow = getStatValue(player, store, flowIndex);
        return flow != null && flow.asPercentage() >= 1.0f;
    }

    public int getFilledSegments(Ref<EntityStore> player, Store<EntityStore> store) {
        EntityStatValue flow = getStatValue(player, store, flowIndex);
        return flow != null ? (int) (flow.get() / segmentSize(player, store)) : 0;
    }

    public int getMaxSegments(Ref<EntityStore> player, Store<EntityStore> store) {
        EntityStatValue capacity = getStatValue(player, store, flowCapacityIndex);
        return capacity != null ? (int) capacity.get() : 3;
    }

    public float segmentSize(Ref<EntityStore> player, Store<EntityStore> store) {
        return 100f / getMaxSegments(player, store);
    }

    // --- Internals ---

    private void addStat(Ref<EntityStore> ref, Store<EntityStore> store,
                         int index, float amount) {
        EntityStatMap stats = getStats(ref, store);
        if (stats == null || index == Integer.MIN_VALUE) return;
        stats.addStatValue(Predictable.SELF, index, amount);
    }

    private void subtractStat(Ref<EntityStore> ref, Store<EntityStore> store,
                              int index, float amount) {
        EntityStatMap stats = getStats(ref, store);
        if (stats == null || index == Integer.MIN_VALUE) return;
        stats.subtractStatValue(Predictable.SELF, index, amount);
    }

    private void minimizeStat(Ref<EntityStore> ref, Store<EntityStore> store, int index) {
        EntityStatMap stats = getStats(ref, store);
        if (stats == null || index == Integer.MIN_VALUE) return;
        stats.minimizeStatValue(index);
    }

    private EntityStatValue getStatValue(Ref<EntityStore> ref, Store<EntityStore> store, int index) {
        EntityStatMap stats = getStats(ref, store);
        if (stats == null || index == Integer.MIN_VALUE) return null;
        return stats.get(index);
    }

    private EntityStatMap getStats(Ref<EntityStore> ref, Store<EntityStore> store) {
        return store.getComponent(ref, EntityStatMap.getComponentType());
    }
}
