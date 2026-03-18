package fr.arnaud.nexus.listener;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.component.SwitchStrikeComponent;
import fr.arnaud.nexus.component.SwitchStrikeComponent.State;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Intercepts weapon-swap packets on the Netty thread and signals the
 * {@link SwitchStrikeComponent} when a swap arrives inside an open window.
 * <p>
 * The packet arrives on the Netty {@code ServerWorkerGroup} thread.
 * All {@link Store} access is illegal on that thread, so any component read or
 * write is dispatched onto the world thread via
 * {@code store.getExternalData().getWorld().execute()} — the same pattern used
 * by {@link fr.arnaud.nexus.system.SlotLockSystem}.
 * <p>
 * Always returns {@code false} — this interceptor never blocks the swap.
 */
public final class SwitchStrikePacketInterceptor {

    private static final Logger LOGGER = Logger.getLogger(SwitchStrikePacketInterceptor.class.getName());

    private static final int MELEE_SLOT = 0;

    public SwitchStrikePacketInterceptor() {
        PacketAdapters.registerInbound(this::onPacket);
    }

    private boolean onPacket(@NonNullDecl PlayerRef playerRef, @NonNullDecl Packet packet) {
        if (!(packet instanceof SyncInteractionChains syncPacket)) return false;

        for (SyncInteractionChain chain : syncPacket.updates) {
            if (chain.interactionType != InteractionType.Ability2) continue;
            if (chain.data == null || !chain.initial) continue;

            scheduleSwapSignal(playerRef);
            return false;
        }

        return false;
    }

    /**
     * Schedules all component access on the world thread so we never touch the
     * Store from the Netty worker thread.
     */
    private void scheduleSwapSignal(@NonNullDecl PlayerRef playerRef) {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            LOGGER.log(Level.WARNING, "[SwitchStrike] Ability2 received but player ref is invalid.");
            return;
        }

        Store<EntityStore> store = ref.getStore();
        store.getExternalData().getWorld().execute(() -> {
            if (!ref.isValid()) return;

            SwitchStrikeComponent switchStrike =
                store.getComponent(ref, SwitchStrikeComponent.getComponentType());

            if (switchStrike == null) {
                LOGGER.log(Level.WARNING,
                    "[SwitchStrike] Ability2 received but SwitchStrikeComponent is missing.");
                return;
            }

            if (switchStrike.getState() != State.WINDOW_OPEN) {
                LOGGER.log(Level.FINE,
                    "[SwitchStrike] Ability2 received but window is IDLE — ignoring.");
                return;
            }

            boolean meleeToRanged = resolveSwapDirection(ref, store);
            LOGGER.log(Level.INFO,
                "[SwitchStrike] Swap detected inside window. meleeToRanged={0}",
                meleeToRanged);
            switchStrike.signalSwap(meleeToRanged);
        });
    }

    /**
     * Reads the player's active hotbar slot to determine swap direction.
     * Must be called on the world thread — store access is valid here.
     * Slot 0 = melee (swapping toward ranged). Slot 1 = ranged (swapping toward melee).
     */
    private boolean resolveSwapDirection(@NonNullDecl Ref<EntityStore> ref,
                                         @NonNullDecl Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            LOGGER.log(Level.WARNING,
                "[SwitchStrike] Could not resolve swap direction — Player component missing.");
            return false;
        }
        int currentSlot = player.getInventory().getActiveHotbarSlot();
        LOGGER.log(Level.INFO,
            "[SwitchStrike] Swap direction resolved. Current slot before swap: {0}",
            currentSlot);
        return currentSlot == MELEE_SLOT;
    }
}
