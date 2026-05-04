package fr.arnaud.nexus.item.weapon.enchantment;

import fr.arnaud.nexus.item.weapon.enchantment.event.NexusEnchantBus;
import fr.arnaud.nexus.item.weapon.enchantment.impl.*;

public final class EnchantmentRegistrar {

    private EnchantmentRegistrar() {
    }

    public static void registerAll() {
        NexusEnchantBus bus = NexusEnchantBus.getInstance();

        // Passive-only
        bus.register(EnchantHealthBoost.ENCHANT_ID, EnchantHealthBoost.INSTANCE);
        bus.register(EnchantStaminaBoost.ENCHANT_ID, EnchantStaminaBoost.INSTANCE);
        bus.register(EnchantSharpness.ENCHANT_ID, EnchantSharpness.INSTANCE);
        bus.register(EnchantSwiftness.ENCHANT_ID, EnchantSwiftness.INSTANCE);

        // Handled in interceptor (no bus logic needed, registered for completeness)
        bus.register(EnchantResilience.ENCHANT_ID, EnchantResilience.INSTANCE);
        bus.register(EnchantWarding.ENCHANT_ID, EnchantWarding.INSTANCE);
        bus.register(EnchantFortitude.ENCHANT_ID, EnchantFortitude.INSTANCE);
        bus.register(EnchantThorns.ENCHANT_ID, EnchantThorns.INSTANCE);
        bus.register(EnchantGambler.ENCHANT_ID, EnchantGambler.INSTANCE);
        bus.register(EnchantCriticalStrike.ENCHANT_ID, EnchantCriticalStrike.INSTANCE);
        bus.register(EnchantBloodlust.ENCHANT_ID, EnchantBloodlust.INSTANCE);

        // Runtime trigger enchants
        bus.register(EnchantVampirism.ENCHANT_ID, EnchantVampirism.INSTANCE);
        bus.register(EnchantFireAspect.ENCHANT_ID, EnchantFireAspect.INSTANCE);
        bus.register(EnchantFreezeAspect.ENCHANT_ID, EnchantFreezeAspect.INSTANCE);
        bus.register(EnchantPoison.ENCHANT_ID, EnchantPoison.INSTANCE);
        bus.register(EnchantPredator.ENCHANT_ID, EnchantPredator.INSTANCE);
        bus.register(EnchantKnockback.ENCHANT_ID, EnchantKnockback.INSTANCE);
        bus.register(EnchantLifeDrain.ENCHANT_ID, EnchantLifeDrain.INSTANCE);
        bus.register(EnchantCripple.ENCHANT_ID, EnchantCripple.INSTANCE);
        bus.register(EnchantSoulHarvest.ENCHANT_ID, EnchantSoulHarvest.INSTANCE);
    }
}
