package fr.arnaud.nexus.item.weapon.enchantment.event;

import com.hypixel.hytale.component.CommandBuffer;
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
 * cmd      — the command buffer for the current tick (needed for spatial queries,
 * applying effects, etc.)
 */
public record NexusEnchantEvent(
    Type type,
    Ref<EntityStore> attacker,
    Ref<EntityStore> target,
    double damage,
    Store<EntityStore> store,
    CommandBuffer<EntityStore> cmd,
    StatIndexResolver statIndexResolver
) {
    public enum Type {
        ON_HIT,
        ON_RECEIVE_HIT,
        ON_KILL
    }
}
