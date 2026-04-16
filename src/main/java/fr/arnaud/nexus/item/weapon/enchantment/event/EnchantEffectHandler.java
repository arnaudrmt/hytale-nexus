package fr.arnaud.nexus.item.weapon.enchantment.event;

public interface EnchantEffectHandler {
    default void onHit(NexusEnchantEvent event, int enchantLevel) {
    }

    default void onReceiveHit(NexusEnchantEvent event, int enchantLevel) {
    }

    default void onKill(NexusEnchantEvent event, int enchantLevel) {
    }
}
