package fr.arnaud.nexus.item.weapon.enchantment.handlers;

import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentDefinition;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentRegistry;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentStatDefinition;
import fr.arnaud.nexus.item.weapon.enchantment.event.EnchantEffectHandler;
import fr.arnaud.nexus.item.weapon.enchantment.event.NexusEnchantEvent;

public final class VampirismEnchantHandler implements EnchantEffectHandler {

    private static final String ENCHANT_ID = "Enchant_Vampirism";
    private static final String KEY_HEAL_RATIO = "HealRatio";

    @Override
    public void handle(NexusEnchantEvent event, int enchantLevel) {
        if (!event.statIndexResolver().isReady()) return;

        EnchantmentDefinition def = EnchantmentRegistry.get().getDefinition(ENCHANT_ID);
        if (def == null) return;

        EnchantmentStatDefinition healthRegen = def.getStat("HealthRegen");
        float healAmount = (float) (event.damageDealt() * healthRegen.getValue(enchantLevel));
        if (healAmount <= 0) return;

        EntityStatMap stats = event.store().getComponent(
            event.attacker(), EntityStatMap.getComponentType()
        );
        if (stats == null) return;

        int healthIndex = event.statIndexResolver().getHealthIndex();

        event.store().getExternalData().getWorld().execute(() -> {
            if (event.attacker().isValid()) {
                stats.addStatValue(healthIndex, healAmount);
            }
        });
    }
}
