package fr.arnaud.nexus.item.weapon.enchantment.event;

public interface EnchantEffectHandler {

    void handle(NexusEnchantEvent event, int enchantLevel);
}
