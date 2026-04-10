package fr.arnaud.nexus.item.weapon.enchantment;

import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.item.weapon.data.WeaponTag;
import fr.arnaud.nexus.item.weapon.enchantment.handlers.*;

import java.util.List;
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
        loadEnchantmentDefinitions();
    }

    private void registerBehavioralHandlers() {

        EnchantmentRegistry registry = EnchantmentRegistry.get();
        EnchantmentDefinition damageboost1 = new EnchantmentDefinition(
            "Enchant_DamageBoost_1", 1, WeaponTag.ANY, null,
            List.of(new EnchantmentDefinition.StatModifierEntry(
                "Nexus_WeaponDamageScale", EnchantmentDefinition.ModifierType.MULTIPLY, 1.15f))
        );
        EnchantmentDefinition damageboost2 = new EnchantmentDefinition(
            "Enchant_DamageBoost_2", 2, WeaponTag.ANY, null,
            List.of(new EnchantmentDefinition.StatModifierEntry(
                "Nexus_WeaponDamageScale", EnchantmentDefinition.ModifierType.MULTIPLY, 1.30f))
        );
        EnchantmentDefinition damageboost3 = new EnchantmentDefinition(
            "Enchant_DamageBoost_3", 3, WeaponTag.ANY, null,
            List.of(new EnchantmentDefinition.StatModifierEntry(
                "Nexus_WeaponDamageScale", EnchantmentDefinition.ModifierType.MULTIPLY, 1.50f))
        );
        registry.registerDefinition(damageboost1);
        registry.registerDefinition(damageboost2);
        registry.registerDefinition(damageboost3);
        registry.registerHandler(damageboost1.getEnchantmentId(), new StatModifierEnchantmentHandler(damageboost1));
        registry.registerHandler(damageboost2.getEnchantmentId(), new StatModifierEnchantmentHandler(damageboost2));
        registry.registerHandler(damageboost3.getEnchantmentId(), new StatModifierEnchantmentHandler(damageboost3));

        registry.registerHandler("cleave", new CleaveEnchantmentHandler());
        registry.registerHandler("shockwave", new ShockwaveEnchantmentHandler());
        registry.registerHandler("aspiration", new AspirationEnchantmentHandler());
        registry.registerHandler("chain_projectile", new ChainProjectileEnchantmentHandler());
        registry.registerHandler("piercing", new PiercingEnchantmentHandler());
        registry.registerHandler("bouncing_projectile", new BouncingProjectileEnchantmentHandler());
    }

    private void loadEnchantmentDefinitions() {
        EnchantmentRegistry registry = EnchantmentRegistry.get();

        // Pure stat enchants — no behavior, just modifiers applied when Flow-gated active
        registry.registerDefinition(new EnchantmentDefinition(
            "Enchant_DamageBoost_1", 1, WeaponTag.ANY, null,
            java.util.List.of(new EnchantmentDefinition.StatModifierEntry(
                "Nexus_WeaponDamageScale", EnchantmentDefinition.ModifierType.MULTIPLY, 1.15f))
        ));
        registry.registerDefinition(new EnchantmentDefinition(
            "Enchant_DamageBoost_2", 2, WeaponTag.ANY, null,
            java.util.List.of(new EnchantmentDefinition.StatModifierEntry(
                "Nexus_WeaponDamageScale", EnchantmentDefinition.ModifierType.MULTIPLY, 1.30f))
        ));
        registry.registerDefinition(new EnchantmentDefinition(
            "Enchant_DamageBoost_3", 3, WeaponTag.ANY, null,
            java.util.List.of(new EnchantmentDefinition.StatModifierEntry(
                "Nexus_WeaponDamageScale", EnchantmentDefinition.ModifierType.MULTIPLY, 1.50f))
        ));

        // Behavioral enchants — stat modifier list is empty, handler owns the logic
        registry.registerDefinition(new EnchantmentDefinition(
            "Enchant_Cleave_1", 1, WeaponTag.MELEE, "cleave", java.util.List.of()
        ));
        registry.registerDefinition(new EnchantmentDefinition(
            "Enchant_Cleave_2", 2, WeaponTag.MELEE, "cleave", java.util.List.of()
        ));
        registry.registerDefinition(new EnchantmentDefinition(
            "Enchant_Cleave_3", 3, WeaponTag.MELEE, "cleave", java.util.List.of()
        ));
        registry.registerDefinition(new EnchantmentDefinition(
            "Enchant_Shockwave_1", 1, WeaponTag.MELEE, "shockwave", java.util.List.of()
        ));
        registry.registerDefinition(new EnchantmentDefinition(
            "Enchant_Shockwave_2", 2, WeaponTag.MELEE, "shockwave", java.util.List.of()
        ));
        registry.registerDefinition(new EnchantmentDefinition(
            "Enchant_Shockwave_3", 3, WeaponTag.MELEE, "shockwave", java.util.List.of()
        ));
        registry.registerDefinition(new EnchantmentDefinition(
            "Enchant_Aspiration_1", 1, WeaponTag.MELEE, "aspiration", java.util.List.of()
        ));
        registry.registerDefinition(new EnchantmentDefinition(
            "Enchant_Aspiration_2", 2, WeaponTag.MELEE, "aspiration", java.util.List.of()
        ));
        registry.registerDefinition(new EnchantmentDefinition(
            "Enchant_Aspiration_3", 3, WeaponTag.MELEE, "aspiration", java.util.List.of()
        ));
        registry.registerDefinition(new EnchantmentDefinition(
            "Enchant_ChainShot_1", 1, WeaponTag.RANGED, "chain_projectile", java.util.List.of()
        ));
        registry.registerDefinition(new EnchantmentDefinition(
            "Enchant_ChainShot_2", 2, WeaponTag.RANGED, "chain_projectile", java.util.List.of()
        ));
        registry.registerDefinition(new EnchantmentDefinition(
            "Enchant_ChainShot_3", 3, WeaponTag.RANGED, "chain_projectile", java.util.List.of()
        ));
        registry.registerDefinition(new EnchantmentDefinition(
            "Enchant_Piercing_1", 1, WeaponTag.RANGED, "piercing", java.util.List.of()
        ));
        registry.registerDefinition(new EnchantmentDefinition(
            "Enchant_Piercing_2", 2, WeaponTag.RANGED, "piercing", java.util.List.of()
        ));
        registry.registerDefinition(new EnchantmentDefinition(
            "Enchant_Piercing_3", 3, WeaponTag.RANGED, "piercing", java.util.List.of()
        ));
        registry.registerDefinition(new EnchantmentDefinition(
            "Enchant_Bouncing_1", 1, WeaponTag.RANGED, "bouncing_projectile", java.util.List.of()
        ));
        registry.registerDefinition(new EnchantmentDefinition(
            "Enchant_Bouncing_2", 2, WeaponTag.RANGED, "bouncing_projectile", java.util.List.of()
        ));
        registry.registerDefinition(new EnchantmentDefinition(
            "Enchant_Bouncing_3", 3, WeaponTag.RANGED, "bouncing_projectile", java.util.List.of()
        ));
    }

    private void logIndexResult(String statName, int index) {
        if (index != Integer.MIN_VALUE) {
            Nexus.get().getLogger().at(Level.INFO).log("SUCCESS: " + statName + " loaded. Index: " + index);
        } else {
            Nexus.get().getLogger().at(Level.WARNING).log("FAIL: " + statName + " not found in asset registry.");
        }
    }
}
