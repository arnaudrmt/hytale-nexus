package fr.arnaud.nexus.system;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.protocol.packets.inventory.SetActiveSlot;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Intercepts inbound weapon-swap packets and blocks them based on lock state.
 * <p>
 * Two lock levels exist because the Switch Strike uses {@link InteractionType#Ability2}
 * as its own swap detection signal — it must be allowed through during the 1-second
 * swap window, but blocked once the swap is confirmed and the sequence is running.
 * <p>
 * <b>Soft lock</b> ({@link #softLock}) — blocks only {@link InteractionType#SwapFrom}
 * (numeric keys, mouse wheel). Active from {@code PENDING_OPEN} through
 * {@code WINDOW_OPEN}. Ability2 is still allowed so {@link fr.arnaud.nexus.switchstrike.SwitchStrikePacketInterceptor}
 * can detect the swap.
 * <p>
 * <b>Hard lock</b> ({@link #hardLock}) — blocks both {@link InteractionType#SwapFrom}
 * and {@link InteractionType#Ability2}. Active from swap confirmation (when the breach
 * or normal Strike executes) until Ability3 fires and the sequence ends.
 * <p>
 * Caller responsibilities:
 * <ul>
 *   <li>{@link fr.arnaud.nexus.switchstrike.SwitchStrikeTriggerSystem} → {@code softLock} on PENDING_OPEN</li>
 *   <li>{@link fr.arnaud.nexus.switchstrike.SwitchStrikeExecutionSystem} → upgrade to {@code hardLock} when swap confirmed, clear both on exit</li>
 *   <li>{@link fr.arnaud.nexus.breach.BreachSequenceSystem} → clears both on Ability3 / abort</li>
 * </ul>
 */
public final class SlotLockSystem {

    private static final Set<UUID> softLock = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> hardLock = ConcurrentHashMap.newKeySet();

    public SlotLockSystem() {
        PacketAdapters.registerInbound(this::filterSlotSwitch);
    }

    /**
     * Blocks only SwapFrom (keys/scroll). Safe during the swap detection window.
     */
    public static void softLock(@NonNullDecl UUID playerUuid) {
        softLock.add(playerUuid);
    }

    /**
     * Blocks both SwapFrom and Ability2. Call once the swap is confirmed and the
     * sequence is running — at this point Ability2 must no longer reach the interceptor.
     */
    public static void hardLock(@NonNullDecl UUID playerUuid) {
        softLock.add(playerUuid);
        hardLock.add(playerUuid);
    }

    /**
     * Releases all locks for this player.
     */
    public static void unlock(@NonNullDecl UUID playerUuid) {
        softLock.remove(playerUuid);
        hardLock.remove(playerUuid);
    }

    private boolean filterSlotSwitch(@NonNullDecl PlayerRef playerRef, @NonNullDecl Packet packet) {
        if (!(packet instanceof SyncInteractionChains syncPacket)) return false;

        UUID uuid = resolveUuid(playerRef);
        if (uuid == null) return false;

        for (SyncInteractionChain chain : syncPacket.updates) {
            if (chain.data == null || !chain.initial) continue;

            if (chain.interactionType == InteractionType.SwapFrom
                && softLock.contains(uuid)) {
                revertClientSlot(playerRef, 0);
                return true;
            }

            if (chain.interactionType == InteractionType.Ability2
                && hardLock.contains(uuid)) {
                revertClientSlotToCurrent(playerRef);
                return true;
            }
        }

        return false;
    }

    private void revertClientSlot(@NonNullDecl PlayerRef playerRef, int slot) {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null) return;

        Store<EntityStore> store = ref.getStore();
        store.getExternalData().getWorld().execute(() -> {
            Player player = store.getComponent(ref, Player.getComponentType());
            player.getInventory().setActiveHotbarSlot(ref, (byte) slot, store);

            SetActiveSlot correction = new SetActiveSlot(Inventory.HOTBAR_SECTION_ID, slot);
            playerRef.getPacketHandler().write(correction);
        });
    }

    /**
     * Resends the player's current server-side hotbar slot to the client without
     * changing it. Used when blocking {@link InteractionType#Ability2} to keep the
     * player on whichever slot the Switch Strike placed them on (slot 1, ranged),
     * rather than snapping back to slot 0.
     */
    private void revertClientSlotToCurrent(@NonNullDecl PlayerRef playerRef) {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null) return;

        Store<EntityStore> store = ref.getStore();
        store.getExternalData().getWorld().execute(() -> {
            Player player = store.getComponent(ref, Player.getComponentType());
            int currentSlot = player.getInventory().getActiveHotbarSlot();

            SetActiveSlot correction = new SetActiveSlot(Inventory.HOTBAR_SECTION_ID, currentSlot);
            playerRef.getPacketHandler().write(correction);
        });
    }

    @javax.annotation.Nullable
    private UUID resolveUuid(@NonNullDecl PlayerRef playerRef) {
        return playerRef.getUuid();
    }
}
