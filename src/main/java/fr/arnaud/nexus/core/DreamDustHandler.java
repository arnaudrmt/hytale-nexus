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

public final class DreamDustHandler {

    private static final float DEATH_PENALTY_FRACTION = 0.25f;

    private int dreamDustIndex = Integer.MIN_VALUE;

    public void onAssetsLoaded(LoadedAssetsEvent<String, EntityStatType, IndexedLookupTableAssetMap<String, EntityStatType>> event) {
        dreamDustIndex = event.getAssetMap().getIndex("DreamDust");
    }

    /**
     * Returns true once asset indices have been resolved. False means onAssetsLoaded never fired.
     */
    public boolean isReady() {
        return dreamDustIndex != Integer.MIN_VALUE;
    }

    /**
     * Index assigned by the Asset Editor — MIN_VALUE means unresolved.
     */
    public int getDreamDustIndex() {
        return dreamDustIndex;
    }

    // --- Mutations ---

    public void addDreamDust(Ref<EntityStore> player, Store<EntityStore> store, float amount) {
        addStat(player, store, amount);
    }

    public void removeDreamDust(Ref<EntityStore> player, Store<EntityStore> store, float amount) {
        subtractStat(player, store, amount);
    }

    /**
     * Deducts 25% of the player's balance on death.
     *
     * @return the amount lost
     */
    public float applyDeathPenalty(Ref<EntityStore> player, Store<EntityStore> store) {
        EntityStatMap stats = getStats(player, store);
        if (stats == null || dreamDustIndex == Integer.MIN_VALUE) return 0f;
        EntityStatValue dust = stats.get(dreamDustIndex);
        if (dust == null) return 0f;
        float lost = dust.get() * DEATH_PENALTY_FRACTION;
        stats.subtractStatValue(Predictable.SELF, dreamDustIndex, lost);
        return lost;
    }

    // --- Queries ---

    public float getBalance(Ref<EntityStore> player, Store<EntityStore> store) {
        EntityStatValue dust = getStatValue(player, store);
        return dust != null ? dust.get() : 0f;
    }

    // --- Internals ---

    private void addStat(Ref<EntityStore> ref, Store<EntityStore> store,
                         float amount) {
        EntityStatMap stats = getStats(ref, store);
        if (stats == null || dreamDustIndex == Integer.MIN_VALUE) return;
        stats.addStatValue(Predictable.SELF, dreamDustIndex, amount);
    }

    private void subtractStat(Ref<EntityStore> ref, Store<EntityStore> store,
                              float amount) {
        EntityStatMap stats = getStats(ref, store);
        if (stats == null || dreamDustIndex == Integer.MIN_VALUE) return;
        stats.subtractStatValue(Predictable.SELF, dreamDustIndex, amount);
    }

    private EntityStatValue getStatValue(Ref<EntityStore> ref, Store<EntityStore> store) {
        EntityStatMap stats = getStats(ref, store);
        if (stats == null || dreamDustIndex == Integer.MIN_VALUE) return null;
        return stats.get(dreamDustIndex);
    }

    private EntityStatMap getStats(Ref<EntityStore> ref, Store<EntityStore> store) {
        return store.getComponent(ref, EntityStatMap.getComponentType());
    }
}
