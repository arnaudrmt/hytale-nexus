package fr.arnaud.nexus.item.weapon.system;

import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;

public final class StatIndexResolver {

    private int weaponDamageIndex = Integer.MIN_VALUE;
    private int healthIndex = Integer.MIN_VALUE;
    private int movementSpeedIndex = Integer.MIN_VALUE;

    public void onAssetsLoaded(LoadedAssetsEvent<String, EntityStatType, IndexedLookupTableAssetMap<String, EntityStatType>> event) {
        weaponDamageIndex = event.getAssetMap().getIndex("WeaponDamage");
        healthIndex = event.getAssetMap().getIndex("Health");
        movementSpeedIndex = event.getAssetMap().getIndex("MovementSpeed");
    }

    public boolean isReady() {
        return weaponDamageIndex != Integer.MIN_VALUE
            && healthIndex != Integer.MIN_VALUE
            && movementSpeedIndex != Integer.MIN_VALUE;
    }

    public int getWeaponDamageIndex() {
        return weaponDamageIndex;
    }

    public int getHealthIndex() {
        return healthIndex;
    }

    public int getMovementSpeedIndex() {
        return movementSpeedIndex;
    }
}
