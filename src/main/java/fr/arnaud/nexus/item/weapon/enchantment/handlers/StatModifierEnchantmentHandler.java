package fr.arnaud.nexus.item.weapon.enchantment.handlers;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentDefinition;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentDefinitionLoader;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentHandler;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentLevelData;

public final class StatModifierEnchantmentHandler implements EnchantmentHandler {

    private final EnchantmentDefinition definition;

    public StatModifierEnchantmentHandler(EnchantmentDefinition definition) {
        this.definition = definition;
    }

    @Override
    public void onActivate(
        Ref<EntityStore> playerRef,
        int level,
        Store<EntityStore> store,
        CommandBuffer<EntityStore> cmd
    ) {
        EntityStatMap stats = store.getComponent(playerRef, EntityStatMap.getComponentType());
        if (stats == null) return;

        EnchantmentLevelData data = definition.getDataForLevel(level);
        if (data == null) return;

        int statIndex = loader().getWeaponDamageScaleIndex();
        if (statIndex == Integer.MIN_VALUE) return;

        float multiplier = data.get("Multiplier", 1.0f);
        StaticModifier modifier = new StaticModifier(
            Modifier.ModifierTarget.MAX,
            StaticModifier.CalculationType.MULTIPLICATIVE,
            multiplier
        );
        stats.putModifier(
            EntityStatMap.Predictable.NONE,
            statIndex,
            buildModifierKey(definition.getEnchantmentId()),
            modifier
        );
    }

    @Override
    public void onDeactivate(
        Ref<EntityStore> playerRef,
        int level,
        Store<EntityStore> store,
        CommandBuffer<EntityStore> cmd
    ) {
        EntityStatMap stats = store.getComponent(playerRef, EntityStatMap.getComponentType());
        if (stats == null) return;

        int statIndex = loader().getWeaponDamageScaleIndex();
        if (statIndex == Integer.MIN_VALUE) return;

        stats.removeModifier(
            EntityStatMap.Predictable.NONE,
            statIndex,
            buildModifierKey(definition.getEnchantmentId())
        );
    }

    private String buildModifierKey(String enchantmentId) {
        return "nexus_enchant_" + enchantmentId;
    }

    private EnchantmentDefinitionLoader loader() {
        return Nexus.get().getEnchantmentDefinitionLoader();
    }
}
