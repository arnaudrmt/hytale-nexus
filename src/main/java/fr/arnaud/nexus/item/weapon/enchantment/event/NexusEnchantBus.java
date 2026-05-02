package fr.arnaud.nexus.item.weapon.enchantment.event;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.item.weapon.component.WeaponInstanceComponent;
import fr.arnaud.nexus.item.weapon.data.EnchantmentSlot;

import java.util.HashMap;
import java.util.Map;

public final class NexusEnchantBus {

    private static final NexusEnchantBus INSTANCE = new NexusEnchantBus();
    private final Map<String, EnchantEffectHandler> handlersByEnchantmentId = new HashMap<>();

    private NexusEnchantBus() {
    }

    public static NexusEnchantBus getInstance() {
        return INSTANCE;
    }

    public void register(String enchantmentId, EnchantEffectHandler handler) {
        handlersByEnchantmentId.put(enchantmentId, handler);
    }

    public void publish(NexusEnchantEvent event) {
        Ref<EntityStore> attacker = event.attacker();
        if (!attacker.isValid()) return;

        WeaponInstanceComponent weapon = event.store().getComponent(attacker, WeaponInstanceComponent.getComponentType());
        if (weapon == null) return;

        for (EnchantmentSlot slot : weapon.enchantmentSlots) {
            if (!slot.isUnlocked()) continue;
            EnchantEffectHandler handler = handlersByEnchantmentId.get(slot.chosen());
            if (handler == null) continue;

            int level = slot.currentLevel();
            switch (event.type()) {
                case ON_HIT -> handler.onHit(event, level);
                case ON_RECEIVE_HIT -> handler.onReceiveHit(event, level);
                case ON_KILL -> handler.onKill(event, level);
            }
        }
    }
}
