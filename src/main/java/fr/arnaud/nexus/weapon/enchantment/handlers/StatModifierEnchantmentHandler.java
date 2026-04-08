package fr.arnaud.nexus.weapon.enchantment.handlers;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.weapon.enchantment.EnchantmentDefinition;
import fr.arnaud.nexus.weapon.enchantment.EnchantmentHandler;

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

        for (EnchantmentDefinition.StatModifierEntry entry : definition.getStatModifiers()) {
            int statIndex = EntityStatType.getAssetMap().getIndex(entry.statId());
            if (statIndex < 0) continue;

            StaticModifier modifier = new StaticModifier(
                Modifier.ModifierTarget.MAX,
                toCalculationType(entry.type()),
                entry.value()
            );
            stats.putModifier(
                EntityStatMap.Predictable.NONE,
                statIndex,
                buildModifierKey(definition.getEnchantmentId(), entry.statId()),
                modifier
            );
        }
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

        for (EnchantmentDefinition.StatModifierEntry entry : definition.getStatModifiers()) {
            int statIndex = EntityStatType.getAssetMap().getIndex(entry.statId());
            if (statIndex < 0) continue;

            stats.removeModifier(
                EntityStatMap.Predictable.NONE,
                statIndex,
                buildModifierKey(definition.getEnchantmentId(), entry.statId())
            );
        }
    }

    private StaticModifier.CalculationType toCalculationType(EnchantmentDefinition.ModifierType type) {
        return switch (type) {
            case ADD -> StaticModifier.CalculationType.ADDITIVE;
            case MULTIPLY -> StaticModifier.CalculationType.MULTIPLICATIVE;
        };
    }

    private String buildModifierKey(String enchantmentId, String statId) {
        return "nexus_enchant_" + enchantmentId + "_" + statId;
    }
}
