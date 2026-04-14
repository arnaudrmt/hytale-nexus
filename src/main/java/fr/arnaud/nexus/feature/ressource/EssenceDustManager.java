package fr.arnaud.nexus.feature.ressource;

import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap.Predictable;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class EssenceDustManager {

    private static final float DEATH_PENALTY_FRACTION = 0.25f;

    private int essenceDustIndex = Integer.MIN_VALUE;

    public void onAssetsLoaded(LoadedAssetsEvent<String, EntityStatType, IndexedLookupTableAssetMap<String, EntityStatType>> event) {
        essenceDustIndex = event.getAssetMap().getIndex("EssenceDust");
    }

    /**
     * Returns true once asset indices have been resolved.
     */
    public boolean isReady() {
        return essenceDustIndex != Integer.MIN_VALUE;
    }

    public int getEssenceDustIndex() {
        return essenceDustIndex;
    }

    public void addEssenceDust(Ref<EntityStore> player, Store<EntityStore> store, float amount) {
        addStat(player, store, amount);
    }

    public void removeEssenceDust(Ref<EntityStore> player, Store<EntityStore> store, float amount) {
        subtractStat(player, store, amount);
    }

    public float getBalance(Ref<EntityStore> player, Store<EntityStore> store) {
        EntityStatValue dust = getStatValue(player, store);
        return dust != null ? dust.get() : 0f;
    }

    private void addStat(Ref<EntityStore> ref, Store<EntityStore> store, float amount) {
        EntityStatMap stats = getStats(ref, store);
        if (stats == null || essenceDustIndex == Integer.MIN_VALUE) return;
        stats.addStatValue(Predictable.SELF, essenceDustIndex, amount);
    }

    private void subtractStat(Ref<EntityStore> ref, Store<EntityStore> store, float amount) {
        EntityStatMap stats = getStats(ref, store);
        if (stats == null || essenceDustIndex == Integer.MIN_VALUE) return;
        stats.subtractStatValue(Predictable.SELF, essenceDustIndex, amount);
    }

    private EntityStatValue getStatValue(Ref<EntityStore> ref, Store<EntityStore> store) {
        EntityStatMap stats = getStats(ref, store);
        if (stats == null || essenceDustIndex == Integer.MIN_VALUE) return null;
        return stats.get(essenceDustIndex);
    }

    private EntityStatMap getStats(Ref<EntityStore> ref, Store<EntityStore> store) {
        return store.getComponent(ref, EntityStatMap.getComponentType());
    }
}
