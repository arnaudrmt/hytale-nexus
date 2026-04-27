package fr.arnaud.nexus.ability;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.ability.impl.DashAbility;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Routes an activation request to whichever Core ability the player has equipped.
 * Owned by {@link fr.arnaud.nexus.input.PlayerInputListener} — replaces the direct
 * {@link fr.arnaud.nexus.feature.movement.PlayerDashSystem} reference.
 */
public final class CoreAbilityRouter {

    private static final Logger LOGGER = Logger.getLogger(CoreAbilityRouter.class.getName());

    private final DashAbility dashAbility;

    public CoreAbilityRouter(@NonNullDecl DashAbility dashAbility) {
        this.dashAbility = dashAbility;
    }

    /**
     * Reads the player's equipped Core and delegates to the matching ability.
     * No-ops silently if the slot is empty or the Core has no input-driven activation.
     */
    public void tryActivate(@NonNullDecl Player player,
                            @NonNullDecl Ref<EntityStore> ref,
                            @NonNullDecl Store<EntityStore> store) {
        ActiveCoreComponent activeCore = store.getComponent(ref, ActiveCoreComponent.getComponentType());
        if (activeCore == null || activeCore.isEmpty()) return;

        CoreAbility equipped = activeCore.getEquippedCore();
        if (equipped == null) return;

        switch (equipped) {
            case DASH -> dashAbility.tryActivate(player, ref, store);
            default -> LOGGER.log(Level.FINE,
                "[CoreAbilityRouter] No input handler registered for Core: {0}", equipped);
        }
    }
}
