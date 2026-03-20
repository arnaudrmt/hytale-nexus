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
import fr.arnaud.nexus.Nexus;

import java.util.logging.Level;

public final class FlowHandler {

    private static final float SEGMENTS_PER_KILL = 0.2f;
    private static final float SEGMENTS_LOST_ON_HIT = 0.4f;

    private int flowSegmentIndex = Integer.MIN_VALUE;

    public void onAssetsLoaded(LoadedAssetsEvent<String, EntityStatType, IndexedLookupTableAssetMap<String, EntityStatType>> event) {
        flowSegmentIndex = event.getAssetMap().getIndex("FlowCapacity");

        if (flowSegmentIndex != Integer.MIN_VALUE) {
            Nexus.get().getLogger().at(Level.INFO).log("SUCCESS: FlowCapacity loaded. Index: " + flowSegmentIndex);
        } else {
            Nexus.get().getLogger().at(Level.WARNING).log("FAIL: FlowCapacity not found in asset registry.");
        }
    }

    /**
     * Returns true once asset indices have been resolved. False means onAssetsLoaded never fired.
     */
    public boolean isReady() {
        return flowSegmentIndex != Integer.MIN_VALUE;
    }

    public int getFlowSegmentIndex() {
        return flowSegmentIndex;
    }

    // --- Gameplay events ---

    public void onKill(Ref<EntityStore> player, Store<EntityStore> store) {
        addSegments(player, store, SEGMENTS_PER_KILL);
    }

    public void onHitReceived(Ref<EntityStore> player, Store<EntityStore> store) {
        removeSegments(player, store, SEGMENTS_LOST_ON_HIT);
    }

    // --- Ability cost ---

    /**
     * Attempts to spend {@code count} whole segments. Returns false without
     * mutating state if the player cannot afford it.
     */
    public boolean trySpendSegments(Ref<EntityStore> player, Store<EntityStore> store, float count) {
        EntityStatValue segments = getStatValue(player, store);
        if (segments == null || segments.get() < count) return false;
        subtractStat(player, store, count);
        return true;
    }

    // --- Queries ---

    public float getSegments(Ref<EntityStore> player, Store<EntityStore> store) {
        EntityStatValue segments = getStatValue(player, store);
        return segments != null ? segments.get() : 0f;
    }

    public int getFilledSegments(Ref<EntityStore> player, Store<EntityStore> store) {
        return (int) getSegments(player, store);
    }

    public int getMaxSegments(Ref<EntityStore> player, Store<EntityStore> store) {
        EntityStatValue segments = getStatValue(player, store);
        return segments != null ? (int) segments.getMax() : 3;
    }

    public boolean isFull(Ref<EntityStore> player, Store<EntityStore> store) {
        EntityStatValue segments = getStatValue(player, store);
        return segments != null && segments.get() >= segments.getMax() / 2;
    }

    // --- Internals ---

    public void addSegments(Ref<EntityStore> ref, Store<EntityStore> store, float amount) {
        EntityStatMap stats = getStats(ref, store);
        if (stats == null || flowSegmentIndex == Integer.MIN_VALUE) return;
        stats.addStatValue(Predictable.SELF, flowSegmentIndex, amount);
    }

    public void removeSegments(Ref<EntityStore> player, Store<EntityStore> store, float amount) {
        subtractStat(player, store, amount);
    }

    public void drainFlow(Ref<EntityStore> player, Store<EntityStore> store) {
        EntityStatMap stats = getStats(player, store);
        if (stats == null || flowSegmentIndex == Integer.MIN_VALUE) return;
        stats.minimizeStatValue(flowSegmentIndex);
    }

    private void subtractStat(Ref<EntityStore> ref, Store<EntityStore> store, float amount) {
        EntityStatMap stats = getStats(ref, store);
        if (stats == null || flowSegmentIndex == Integer.MIN_VALUE) return;
        stats.subtractStatValue(Predictable.SELF, flowSegmentIndex, amount);
    }

    private EntityStatValue getStatValue(Ref<EntityStore> ref, Store<EntityStore> store) {
        EntityStatMap stats = getStats(ref, store);
        if (stats == null || flowSegmentIndex == Integer.MIN_VALUE) return null;
        return stats.get(flowSegmentIndex);
    }

    private EntityStatMap getStats(Ref<EntityStore> ref, Store<EntityStore> store) {
        return store.getComponent(ref, EntityStatMap.getComponentType());
    }
}
