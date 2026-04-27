package fr.arnaud.nexus.feature.ressource;

import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap.Predictable;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Unified access point for all player stats shown in the character panel:
 * Health, Stamina, EssenceDust (EntityStatMap) and MovementSpeed (MovementManager).
 * <p>
 * Call {@link #onAssetsLoaded} once during startup to resolve stat indices.
 */
public final class PlayerStatsManager {

    // Modifier keys — unique strings used to identify our modifiers on the stat
    private static final String MOD_KEY_MAX_HEALTH = "nexus_max_health";
    private static final String MOD_KEY_MAX_STAMINA = "nexus_max_stamina";
    // Tracks the last speed bonus applied so we can reverse it exactly
    private static final String SPEED_BONUS_KEY = "nexus_weapon_speed";

    private int healthIndex = Integer.MIN_VALUE;
    private int staminaIndex = Integer.MIN_VALUE;
    private int essenceDustIndex = Integer.MIN_VALUE;

    // ── Asset loading ─────────────────────────────────────────────────────────

    public void onAssetsLoaded(
        LoadedAssetsEvent<String, EntityStatType,
            IndexedLookupTableAssetMap<String, EntityStatType>> event) {
        healthIndex = event.getAssetMap().getIndex("Health");
        staminaIndex = event.getAssetMap().getIndex("Stamina");
        essenceDustIndex = event.getAssetMap().getIndex("EssenceDust");
    }

    public boolean isReady() {
        return healthIndex != Integer.MIN_VALUE
            && staminaIndex != Integer.MIN_VALUE
            && essenceDustIndex != Integer.MIN_VALUE;
    }

    // ── Health ────────────────────────────────────────────────────────────────

    public float getHealth(Ref<EntityStore> ref, Store<EntityStore> store) {
        EntityStatValue v = getStat(ref, store, healthIndex);
        return v != null ? v.get() : 0f;
    }

    public float getMaxHealth(Ref<EntityStore> ref, Store<EntityStore> store) {
        EntityStatValue v = getStat(ref, store, healthIndex);
        return v != null ? v.getMax() : 0f;
    }

    public void addHealth(Ref<EntityStore> ref, Store<EntityStore> store, float amount) {
        addStat(ref, store, healthIndex, amount);
    }

    public void removeHealth(Ref<EntityStore> ref, Store<EntityStore> store, float amount) {
        subtractStat(ref, store, healthIndex, amount);
    }

    /**
     * Applies an additive bonus to the player's max health via a named modifier.
     * Pass 0 to remove the modifier and restore the base max.
     */
    public void setMaxHealthBonus(Ref<EntityStore> ref, Store<EntityStore> store, float bonus) {
        EntityStatMap stats = getStats(ref, store);
        if (stats == null || healthIndex == Integer.MIN_VALUE) return;

        // Read old max before applying the new modifier so we can compute the delta
        EntityStatValue healthStat = stats.get(healthIndex);
        float oldMax = healthStat != null ? healthStat.getMax() : 0f;

        if (bonus == 0f) {
            stats.removeModifier(Predictable.NONE, healthIndex, MOD_KEY_MAX_HEALTH);
        } else {
            stats.putModifier(Predictable.NONE, healthIndex, MOD_KEY_MAX_HEALTH,
                new StaticModifier(Modifier.ModifierTarget.MAX,
                    StaticModifier.CalculationType.ADDITIVE, bonus));
        }

        // Adjust current health:
        // - If max increased (upgrade): add the delta so player keeps relative health
        //   e.g. 10/100 → 110/200
        // - If max decreased (downgrade): clamp current health to the new max
        //   e.g. 200/200 → 100/100, but 10/200 → 10/100 (stays at 10)
        if (healthStat != null) {
            float newMax = healthStat.getMax();
            float delta = newMax - oldMax;
            if (delta > 0f) {
                // Upgrade — add delta to current health
                addHealth(ref, store, delta);
            } else if (delta < 0f) {
                // Downgrade — clamp current health to new max without dealing damage
                float currentHealth = healthStat.get();
                if (currentHealth > newMax) {
                    // Set health directly to new max by adding the difference
                    // (negative add = reduce, but clamped to newMax not zero)
                    addHealth(ref, store, newMax - currentHealth);
                }
            }
        }
    }

    // ── Stamina ───────────────────────────────────────────────────────────────

    public float getStamina(Ref<EntityStore> ref, Store<EntityStore> store) {
        EntityStatValue v = getStat(ref, store, staminaIndex);
        return v != null ? v.get() : 0f;
    }

    public float getMaxStamina(Ref<EntityStore> ref, Store<EntityStore> store) {
        EntityStatValue v = getStat(ref, store, staminaIndex);
        return v != null ? v.getMax() : 0f;
    }

    public void addStamina(Ref<EntityStore> ref, Store<EntityStore> store, float amount) {
        addStat(ref, store, staminaIndex, amount);
    }

    public void removeStamina(Ref<EntityStore> ref, Store<EntityStore> store, float amount) {
        subtractStat(ref, store, staminaIndex, amount);
    }

    /**
     * Applies an additive bonus to the player's max stamina via a named modifier.
     * Pass 0 to remove the modifier and restore the base max.
     */
    public void setMaxStaminaBonus(Ref<EntityStore> ref, Store<EntityStore> store, float bonus) {
        EntityStatMap stats = getStats(ref, store);
        if (stats == null || staminaIndex == Integer.MIN_VALUE) return;
        if (bonus == 0f) {
            stats.removeModifier(Predictable.NONE, staminaIndex, MOD_KEY_MAX_STAMINA);
        } else {
            stats.putModifier(Predictable.NONE, staminaIndex, MOD_KEY_MAX_STAMINA,
                new StaticModifier(Modifier.ModifierTarget.MAX,
                    StaticModifier.CalculationType.ADDITIVE, bonus));
        }
    }

    // ── Essence Dust ──────────────────────────────────────────────────────────

    public float getEssenceDust(Ref<EntityStore> ref, Store<EntityStore> store) {
        EntityStatValue v = getStat(ref, store, essenceDustIndex);
        return v != null ? v.get() : 0f;
    }

    public void addEssenceDust(Ref<EntityStore> ref, Store<EntityStore> store, float amount) {
        addStat(ref, store, essenceDustIndex, amount);
    }

    public void removeEssenceDust(Ref<EntityStore> ref, Store<EntityStore> store, float amount) {
        subtractStat(ref, store, essenceDustIndex, amount);
    }

    // ── Movement Speed ────────────────────────────────────────────────────────

    /**
     * Returns the player's current base movement speed from {@link MovementManager}.
     */
    public float getMovementSpeed(Ref<EntityStore> ref, Store<EntityStore> store) {
        MovementManager mm = store.getComponent(ref, MovementManager.getComponentType());
        if (mm == null || mm.getSettings() == null) return 0f;
        return mm.getSettings().baseSpeed;
    }

    /**
     * Sets the player's base movement speed and pushes the update to the client.
     */
    public void setMovementSpeed(Ref<EntityStore> ref, Store<EntityStore> store, float speed) {
        MovementManager mm = store.getComponent(ref, MovementManager.getComponentType());
        if (mm == null) return;
        mm.getSettings().baseSpeed = speed;
        mm.getDefaultSettings().baseSpeed = speed;
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef != null) mm.update(playerRef.getPacketHandler());
    }

    /**
     * Adds a delta to the player's base movement speed and pushes the update.
     */
    public void addMovementSpeed(Ref<EntityStore> ref, Store<EntityStore> store, float delta) {
        setMovementSpeed(ref, store, getMovementSpeed(ref, store) + delta);
    }

    /**
     * Sets a named weapon speed bonus, replacing any previous one.
     * Safe to call repeatedly — removes old bonus and applies new one atomically.
     * Pass 0 to remove the bonus entirely.
     */
    public void setMovementSpeedBonus(Ref<EntityStore> ref, Store<EntityStore> store,
                                      float bonus) {
        MovementManager mm = store.getComponent(ref, MovementManager.getComponentType());
        if (mm == null) return;

        // Remove old bonus by reading what was previously stored
        float oldBonus = mm.getSettings().baseSpeed
            - mm.getDefaultSettings().baseSpeed;

        // Apply new net speed: default + new bonus
        float newSpeed = mm.getDefaultSettings().baseSpeed + bonus;
        mm.getSettings().baseSpeed = newSpeed;

        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef != null) mm.update(playerRef.getPacketHandler());
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

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

    private EntityStatValue getStat(Ref<EntityStore> ref, Store<EntityStore> store,
                                    int index) {
        if (index == Integer.MIN_VALUE) return null;
        EntityStatMap stats = getStats(ref, store);
        return stats != null ? stats.get(index) : null;
    }

    private EntityStatMap getStats(Ref<EntityStore> ref, Store<EntityStore> store) {
        return store.getComponent(ref, EntityStatMap.getComponentType());
    }

    public int getHealthIndex() {
        return healthIndex;
    }

    public int getStaminaIndex() {
        return staminaIndex;
    }
}
