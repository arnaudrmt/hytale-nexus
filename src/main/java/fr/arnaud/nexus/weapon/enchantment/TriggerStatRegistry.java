package fr.arnaud.nexus.weapon.enchantment;

import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.Nexus;

import java.util.logging.Level;

public final class TriggerStatRegistry {

    private static final TriggerStatRegistry INSTANCE = new TriggerStatRegistry();

    private int healthIndex = Integer.MIN_VALUE;
    private int weaponDamageScaleIndex = Integer.MIN_VALUE;
    private int triggerCleaveIndex = Integer.MIN_VALUE;
    private int triggerShockwaveIndex = Integer.MIN_VALUE;
    private int triggerAspirationIndex = Integer.MIN_VALUE;
    private int triggerChainProjectileIndex = Integer.MIN_VALUE;
    private int triggerPiercingIndex = Integer.MIN_VALUE;
    private int triggerBouncingIndex = Integer.MIN_VALUE;

    private TriggerStatRegistry() {
    }

    public static TriggerStatRegistry get() {
        return INSTANCE;
    }

    public void onAssetsLoaded(
        LoadedAssetsEvent<String, EntityStatType,
            IndexedLookupTableAssetMap<String, EntityStatType>> event
    ) {
        var map = event.getAssetMap();
        healthIndex = resolve(map, "Health");
        weaponDamageScaleIndex = resolve(map, "Nexus_WeaponDamageScale");
        triggerCleaveIndex = resolve(map, "Nexus_Trigger_Cleave");
        triggerShockwaveIndex = resolve(map, "Nexus_Trigger_Shockwave");
        triggerAspirationIndex = resolve(map, "Nexus_Trigger_Aspiration");
        triggerChainProjectileIndex = resolve(map, "Nexus_Trigger_ChainProjectile");
        triggerPiercingIndex = resolve(map, "Nexus_Trigger_Piercing");
        triggerBouncingIndex = resolve(map, "Nexus_Trigger_Bouncing");
    }

    public boolean isReady() {
        return healthIndex != Integer.MIN_VALUE && weaponDamageScaleIndex != Integer.MIN_VALUE;
    }

    public int getHealthIndex() {
        return healthIndex;
    }

    public int getWeaponDamageScaleIndex() {
        return weaponDamageScaleIndex;
    }

    public int getTriggerCleaveIndex() {
        return triggerCleaveIndex;
    }

    public int getTriggerShockwaveIndex() {
        return triggerShockwaveIndex;
    }

    public int getTriggerAspirationIndex() {
        return triggerAspirationIndex;
    }

    public int getTriggerChainProjectileIndex() {
        return triggerChainProjectileIndex;
    }

    public int getTriggerPiercingIndex() {
        return triggerPiercingIndex;
    }

    public int getTriggerBouncingIndex() {
        return triggerBouncingIndex;
    }

    public float resolveWeaponDamage(Ref<EntityStore> playerRef, Store<EntityStore> store) {
        EntityStatMap stats = store.getComponent(playerRef, EntityStatMap.getComponentType());
        if (stats == null) return 0f;
        EntityStatValue value = stats.get(weaponDamageScaleIndex);
        return value != null ? value.get() : 0f;
    }

    public float resolveWeaponDamage(Ref<EntityStore> playerRef, CommandBuffer<EntityStore> cmd) {
        EntityStatMap stats = cmd.getComponent(playerRef, EntityStatMap.getComponentType());
        if (stats == null) return 0f;
        EntityStatValue value = stats.get(weaponDamageScaleIndex);
        return value != null ? value.get() : 0f;
    }

    private int resolve(IndexedLookupTableAssetMap<String, EntityStatType> map, String id) {
        int index = map.getIndex(id);
        if (index == Integer.MIN_VALUE) {
            Nexus.get().getLogger().at(Level.WARNING).log("FAIL: Stat not found in registry: " + id);
        }
        return index;
    }
}
