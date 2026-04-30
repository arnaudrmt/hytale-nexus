package fr.arnaud.nexus.item.weapon.enchantment.event;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Fired through NexusEnchantBus for every enchant trigger type.
 */
public record NexusEnchantEvent(
    Type type,
    Ref<EntityStore> attacker,
    Ref<EntityStore> target,
    double damage,
    Store<EntityStore> store,
    CommandBuffer<EntityStore> cmd
) {
    public enum Type {
        ON_HIT,
        ON_RECEIVE_HIT,
        ON_KILL
    }
}
