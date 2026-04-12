package fr.arnaud.nexus.item.weapon.enchantment;

import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.item.weapon.data.WeaponTag;
import fr.arnaud.nexus.item.weapon.enchantment.handlers.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public final class EnchantmentDefinitionLoader {

    private int weaponDamageScaleIndex = Integer.MIN_VALUE;
    private int weaponAttackSpeedIndex = Integer.MIN_VALUE;

    public boolean isReady() {
        return weaponDamageScaleIndex != Integer.MIN_VALUE
            && weaponAttackSpeedIndex != Integer.MIN_VALUE;
    }

    public int getWeaponDamageScaleIndex() {
        return weaponDamageScaleIndex;
    }

    public int getWeaponAttackSpeedIndex() {
        return weaponAttackSpeedIndex;
    }

    public void onAssetsLoaded(
        LoadedAssetsEvent<String, EntityStatType,
            IndexedLookupTableAssetMap<String, EntityStatType>> event
    ) {
        var statMap = event.getAssetMap();

        weaponDamageScaleIndex = statMap.getIndex("Nexus_WeaponDamageScale");
        weaponAttackSpeedIndex = statMap.getIndex("Nexus_WeaponAttackSpeed");

        logIndexResult("Nexus_WeaponDamageScale", weaponDamageScaleIndex);
        logIndexResult("Nexus_WeaponAttackSpeed", weaponAttackSpeedIndex);

        registerBehavioralHandlers();
    }

    private void registerBehavioralHandlers() {
        EnchantmentRegistry registry = EnchantmentRegistry.get();

        registry.registerHandler("cleave", new CleaveEnchantmentHandler());
        registry.registerHandler("shockwave", new ShockwaveEnchantmentHandler());
        registry.registerHandler("aspiration", new AspirationEnchantmentHandler());
        registry.registerHandler("chain_projectile", new ChainProjectileEnchantmentHandler());
        registry.registerHandler("piercing", new PiercingEnchantmentHandler());
        registry.registerHandler("bouncing_projectile", new BouncingProjectileEnchantmentHandler());

        registerDamageBoost(registry, 1, 1.15f);
        registerDamageBoost(registry, 2, 1.30f);
        registerDamageBoost(registry, 3, 1.50f);
    }

    private void registerDamageBoost(EnchantmentRegistry registry, int level, float multiplier) {
        String id = "Enchant_DamageBoost_" + level;

        Map<String, Float> params = new HashMap<>();
        params.put("Multiplier", multiplier);
        EnchantmentLevelData levelData = new EnchantmentLevelData(level, params);

        EnchantmentDefinition def = new EnchantmentDefinition(
            id,
            "Enchant_DamageBoost",
            level,
            WeaponTag.ANY,
            null,
            80f,
            1.5f,
            List.of(levelData)
        );

        registry.registerDefinition(def);
        registry.registerHandler(id, new StatModifierEnchantmentHandler(def));
    }

    private void logIndexResult(String statName, int index) {
        if (index != Integer.MIN_VALUE) {
            Nexus.get().getLogger().at(Level.INFO).log("SUCCESS: " + statName + " loaded. Index: " + index);
        } else {
            Nexus.get().getLogger().at(Level.WARNING).log("FAIL: " + statName + " not found in asset registry.");
        }
    }
}
