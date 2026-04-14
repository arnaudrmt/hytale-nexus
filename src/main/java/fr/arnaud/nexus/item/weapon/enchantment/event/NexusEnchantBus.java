package fr.arnaud.nexus.item.weapon.enchantment.event;

import fr.arnaud.nexus.item.weapon.component.WeaponInstanceComponent;
import fr.arnaud.nexus.item.weapon.data.EnchantmentSlot;

import java.util.HashMap;
import java.util.Map;

public final class NexusEnchantBus {

    private static final NexusEnchantBus INSTANCE = new NexusEnchantBus();

    private final Map<String, EnchantEffectHandler> handlers = new HashMap<>();

    private NexusEnchantBus() {}

    public static NexusEnchantBus get() {
        return INSTANCE;
    }

    public void register(String enchantmentId, EnchantEffectHandler handler) {
        handlers.put(enchantmentId, handler);
    }

    /**
     * Publishes an event. Looks up the attacker's active weapon enchantments
     * and dispatches to any handler whose enchant id is present and unlocked.
     */
    public void publish(NexusEnchantEvent event) {
        if (!event.attacker().isValid()) return;

        WeaponInstanceComponent weapon = event.store().getComponent(
            event.attacker(), WeaponInstanceComponent.getComponentType()
        );
        if (weapon == null) return;

        for (EnchantmentSlot slot : weapon.enchantmentSlots) {
            if (!slot.isUnlocked()) continue;

            EnchantEffectHandler handler = handlers.get(slot.chosen());
            if (handler != null) {
                handler.handle(event, slot.currentLevel());
            }
        }
    }
}
