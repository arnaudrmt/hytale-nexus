package fr.arnaud.nexus.item.weapon.enchantment.event;

/**
 * Implement this interface for each enchantment that has runtime behaviour.
 *
 * <p>All methods have default no-op implementations so implementations only
 * need to override the triggers they care about.
 *
 * <p>Register instances via {@link NexusEnchantBus#register(String, EnchantEffectHandler)}.
 */
public interface EnchantEffectHandler {

    /**
     * Called when the player holding this weapon hits an entity.
     * Use for on-hit effects like fire aspect or lifesteal procs.
     */
    default void onHit(NexusEnchantEvent event, int enchantLevel) {
    }

    /**
     * Called when the player holding this weapon receives damage.
     * Use for defensive procs like thorns or damage reduction.
     */
    default void onReceiveHit(NexusEnchantEvent event, int enchantLevel) {
    }

    /**
     * Called when an entity killed by this player dies.
     * Use for kill procs like vampirism life steal.
     */
    default void onKill(NexusEnchantEvent event, int enchantLevel) {
    }
}
