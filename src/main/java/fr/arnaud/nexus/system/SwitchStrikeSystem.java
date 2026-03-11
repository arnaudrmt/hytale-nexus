package fr.arnaud.nexus.system;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.handler.FlowHandler;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class SwitchStrikeSystem {

    private static final Logger LOG = Logger.getLogger("Nexus/SwitchStrike");
    private static final long SWAP_WINDOW_MS = 1000L;

    private enum Phase {IDLE, PHASE_A_PENDING, PHASE_B_PENDING}

    private record PhaseState(Phase phase, long timestampMs) {
    }

    private final Map<UUID, PhaseState> states = new ConcurrentHashMap<>();
    private final FlowHandler flowHandler;

    public SwitchStrikeSystem(@NonNullDecl FlowHandler flowHandler) {
        this.flowHandler = flowHandler;
        PacketAdapters.registerInbound((PlayerPacketFilter) this::onPacket);
    }

    private boolean onPacket(@NonNullDecl PlayerRef playerRef, @NonNullDecl Packet packet) {
        if (!(packet instanceof SyncInteractionChains syncPacket)) return false;

        for (SyncInteractionChain chain : syncPacket.updates) {
            if (chain.data == null || !chain.initial) continue;

            switch (chain.interactionType) {
                case Ability1 -> handleAbility1(playerRef);
                case ProjectileHit -> handleProjectileHit(playerRef);
                case SwapFrom -> handleSwap(playerRef);
                default -> {
                }
            }
        }

        return false;
    }

    // Phase A — Ability1 at max Flow: arm the system, wait for the hit
    private void handleAbility1(@NonNullDecl PlayerRef playerRef) {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null) return;

        Store<EntityStore> store = ref.getStore();
        store.getExternalData().getWorld().execute(() -> {
            if (!flowHandler.isFull(ref, store)) {
                reset(playerRef);
                return;
            }

            LOG.info("[SwitchStrike] %s — Phase A armed: Ability1 at max Flow, waiting for hit"
                .formatted(playerRef.getUsername()));
            states.put(playerRef.getUuid(), new PhaseState(Phase.PHASE_A_PENDING, System.currentTimeMillis()));
        });
    }

    // Phase B — ability hit landed: open the 1s swap window
    private void handleProjectileHit(@NonNullDecl PlayerRef playerRef) {
        PhaseState state = states.get(playerRef.getUuid());
        if (state == null || state.phase() != Phase.PHASE_A_PENDING) return;

        LOG.info("[SwitchStrike] %s — Phase B: hit confirmed, 1s swap window open"
            .formatted(playerRef.getUsername()));
        states.put(playerRef.getUuid(), new PhaseState(Phase.PHASE_B_PENDING, System.currentTimeMillis()));
    }

    // Phase C — swap within window: Switch Strike confirmed
    private void handleSwap(@NonNullDecl PlayerRef playerRef) {
        PhaseState state = states.get(playerRef.getUuid());
        if (state == null || state.phase() != Phase.PHASE_B_PENDING) return;

        long elapsed = System.currentTimeMillis() - state.timestampMs();
        if (elapsed > SWAP_WINDOW_MS) {
            LOG.info("[SwitchStrike] %s — swap window expired (%dms), resetting"
                .formatted(playerRef.getUsername(), elapsed));
            reset(playerRef);
            return;
        }

        LOG.info("[SwitchStrike] %s — CONFIRMED in %dms! Switch Strike triggered."
            .formatted(playerRef.getUsername(), elapsed));
        reset(playerRef);
        // TODO: trigger Glimpse mode, camera transition, vulnerability window
    }

    private void reset(@NonNullDecl PlayerRef playerRef) {
        states.remove(playerRef.getUuid());
    }
}
