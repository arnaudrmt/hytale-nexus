package fr.arnaud.nexus.system;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.inventory.SetActiveSlot;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.component.EquippedWeaponsComponent;
import fr.arnaud.nexus.component.SwitchStrikeComponent;
import fr.arnaud.nexus.event.SwitchStrikeActivatedEvent;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

/**
 * Executes weapon swaps triggered by the player's right-click (second-ability) input.
 * <p>
 * Before swapping it checks whether a Switch Strike window is open. If so,
 * the window is consumed and {@link SwitchStrikeActivatedEvent} is dispatched.
 * The swap then proceeds unconditionally — the Switch Strike annotates it but
 * does not replace it.
 * <p>
 * The packet handler is obtained from {@link PlayerRef} in the store, not from
 * the {@link Player} component, which does not expose it directly.
 */
public final class WeaponSwapSystem {

    public void trySwap(@NonNullDecl Player player,
                        @NonNullDecl Ref<EntityStore> ref,
                        @NonNullDecl Store<EntityStore> store) {

        EquippedWeaponsComponent loadout = store.getComponent(ref, EquippedWeaponsComponent.getComponentType());
        if (loadout == null) return;

        checkAndConsumeSwitchStrikeWindow(ref, store);

        byte incomingSlot = loadout.swap();
        store.putComponent(ref, EquippedWeaponsComponent.getComponentType(), loadout);

        player.getInventory().setActiveHotbarSlot(incomingSlot);

        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef != null) {
            playerRef.getPacketHandler().write(new SetActiveSlot(Inventory.HOTBAR_SECTION_ID, incomingSlot));
        }
    }

    private static void checkAndConsumeSwitchStrikeWindow(@NonNullDecl Ref<EntityStore> ref,
                                                          @NonNullDecl Store<EntityStore> store) {
        SwitchStrikeComponent switchStrike = store.getComponent(ref, SwitchStrikeComponent.getComponentType());
        if (switchStrike == null || !switchStrike.consume()) return;

        store.putComponent(ref, SwitchStrikeComponent.getComponentType(), switchStrike);
        SwitchStrikeActivatedEvent.dispatch(ref);
    }
}
