package fr.arnaud.nexus.item.weapon.enchantment.event;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.item.weapon.system.StatIndexResolver;

public record NexusEnchantEvent(
    Type type,
    Ref<EntityStore> attacker,
    Ref<EntityStore> target,
    double damageDealt,
    Store<EntityStore> store,
    StatIndexResolver statIndexResolver
) {
    public enum Type {
        ON_HIT
    }
}
