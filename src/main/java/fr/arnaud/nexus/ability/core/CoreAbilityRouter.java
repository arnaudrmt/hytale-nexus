package fr.arnaud.nexus.ability.core;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.ability.dash.DashAbility;
import fr.arnaud.nexus.ability.strike.SwitchStrikeAbility;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.logging.Level;

public final class CoreAbilityRouter {

    private final DashAbility dashAbility;
    private final SwitchStrikeAbility switchStrikeAbility;

    public CoreAbilityRouter(@NonNullDecl DashAbility dashAbility,
                             @NonNullDecl SwitchStrikeAbility switchStrikeAbility) {
        this.dashAbility = dashAbility;
        this.switchStrikeAbility = switchStrikeAbility;
    }

    public void tryActivate(@NonNullDecl Ref<EntityStore> ref,
                            @NonNullDecl Store<EntityStore> store) {
        ActiveCoreComponent activeCore = store.getComponent(ref, ActiveCoreComponent.getComponentType());
        if (activeCore == null || activeCore.isEmpty()) return;

        CoreAbility equippedCore = activeCore.getEquippedCore();
        if (equippedCore == null) return;

        switch (equippedCore) {
            case DASH -> dashAbility.tryActivate(ref, store);
            case SWITCH_STRIKE -> switchStrikeAbility.tryActivate(ref, store);
            default -> HytaleLogger.getLogger().at(Level.FINE).log(
                "[CoreAbilityRouter] No input handler registered for Core: {0}", equippedCore);
        }
    }
}
