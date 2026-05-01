package fr.arnaud.nexus.item.weapon.enchantment;

import fr.arnaud.nexus.item.weapon.enchantment.event.NexusEnchantBus;
import fr.arnaud.nexus.item.weapon.enchantment.impl.*;

public final class EnchantmentRegistrar {

    private EnchantmentRegistrar() {
    }

    public static void registerAll() {
        NexusEnchantBus bus = NexusEnchantBus.get();

        // Passive-only
        bus.register("Enchant_HealthBoost", EnchantHealthBoost.INSTANCE);
        bus.register("Enchant_StaminaBoost", EnchantStaminaBoost.INSTANCE);
        bus.register("Enchant_Sharpness", EnchantSharpness.INSTANCE);
        bus.register("Enchant_Swiftness", EnchantSwiftness.INSTANCE);

        // Handled in interceptor (no bus logic needed, registered for completeness)
        bus.register("Enchant_Resilience", EnchantResilience.INSTANCE);
        bus.register("Enchant_Warding", EnchantWarding.INSTANCE);
        bus.register("Enchant_Fortitude", EnchantFortitude.INSTANCE);
        bus.register("Enchant_Thorns", EnchantThorns.INSTANCE);
        bus.register("Enchant_Gambler", EnchantGambler.INSTANCE);
        bus.register("Enchant_CriticalStrike", EnchantCriticalStrike.INSTANCE);
        bus.register("Enchant_Bloodlust", EnchantBloodlust.INSTANCE);

        // Runtime trigger enchants
        bus.register("Enchant_Vampirism", EnchantVampirism.INSTANCE);
        bus.register("Enchant_FireAspect", EnchantFireAspect.INSTANCE);
        bus.register("Enchant_FreezeAspect", EnchantFreezeAspect.INSTANCE);
        bus.register("Enchant_Poison", EnchantPoison.INSTANCE);
        bus.register("Enchant_SwiftnessOnKill", EnchantSwiftnessOnKill.INSTANCE);
        bus.register("Enchant_Knockback", EnchantKnockback.INSTANCE);
        bus.register("Enchant_LifeDrain", EnchantLifeDrain.INSTANCE);
        bus.register("Enchant_Cripple", EnchantCripple.INSTANCE);
        bus.register("Enchant_SoulHarvest", EnchantSoulHarvest.INSTANCE);
    }
}
