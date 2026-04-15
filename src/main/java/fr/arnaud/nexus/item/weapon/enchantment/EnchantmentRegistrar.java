package fr.arnaud.nexus.item.weapon.enchantment;

import fr.arnaud.nexus.item.weapon.enchantment.event.NexusEnchantBus;
import fr.arnaud.nexus.item.weapon.enchantment.impl.*;

/**
 * Registers all enchantment effect handlers into {@link NexusEnchantBus}.
 *
 * Call {@link #registerAll()} once during server startup, after
 * {@link EnchantmentRegistry#loadAll()} has completed.
 *
 * To add a new enchant: create a class in the impl package implementing
 * {@link fr.arnaud.nexus.item.weapon.enchantment.event.EnchantEffectHandler},
 * then add a line here.
 */
public final class EnchantmentRegistrar {

    private EnchantmentRegistrar() {}

    public static void registerAll() {
        NexusEnchantBus bus = NexusEnchantBus.get();

        // Passive-only enchants (no runtime trigger, registered for completeness)
        bus.register("Enchant_HealthBoost",  EnchantHealthBoost.INSTANCE);
        bus.register("Enchant_StaminaBoost", EnchantStaminaBoost.INSTANCE);
        bus.register("Enchant_Sharpness",   EnchantSharpness.INSTANCE);
        bus.register("Enchant_Swiftness",   EnchantSwiftness.INSTANCE);

        // Runtime trigger enchants
        bus.register("Enchant_Vampirism",   EnchantVampirism.INSTANCE);
    }
}
