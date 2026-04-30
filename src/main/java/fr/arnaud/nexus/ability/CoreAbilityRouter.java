package fr.arnaud.nexus.ability;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.ability.impl.DashAbility;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public final class CoreAbilityRouter {

    private final DashAbility dashAbility;

    public CoreAbilityRouter(@NonNullDecl DashAbility dashAbility) {
        this.dashAbility = dashAbility;
    }

    public void tryActivate(@NonNullDecl Player player,
                            @NonNullDecl Ref<EntityStore> ref,
                            @NonNullDecl Store<EntityStore> store) {
        ActiveCoreComponent activeCore = store.getComponent(ref, ActiveCoreComponent.getComponentType());
        if (activeCore == null || activeCore.isEmpty()) return;

        CoreAbility equipped = activeCore.getEquippedCore();
        if (equipped == null) return;

        if (equipped == CoreAbility.DASH) {
            dashAbility.tryActivate(player, ref, store);
        }
    }
}
