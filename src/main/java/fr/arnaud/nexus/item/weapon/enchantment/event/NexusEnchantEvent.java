package fr.arnaud.nexus.item.weapon.enchantment.event;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.item.weapon.system.StatIndexResolver;

/**
 * Fired through {@link NexusEnchantBus} for every enchant trigger type.
 * <p>
 * attacker — the entity dealing damage / the player whose weapon proc'd
 * target   — the entity receiving damage / dying
 * damage   — the raw damage amount (0 for non-damage triggers)
 */
public record NexusEnchantEvent(
    Type type,
    Ref<EntityStore> attacker,
    Ref<EntityStore> target,
    double damage,
    Store<EntityStore> store,
    StatIndexResolver statIndexResolver
) {
    public enum Type {
        /**
         * Player hit an entity with a weapon.
         */
        ON_HIT,
        /**
         * Player received damage from any source.
         */
        ON_RECEIVE_HIT,
        /**
         * An entity killed by the player died.
         */
        ON_KILL
    }
}
