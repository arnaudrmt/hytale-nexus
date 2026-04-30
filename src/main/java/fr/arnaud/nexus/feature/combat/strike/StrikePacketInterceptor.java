package fr.arnaud.nexus.feature.combat.strike;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.feature.resource.PlayerStatsManager;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

/**
 * Intercepts Ability2 packets on the Netty thread.
 */
public final class StrikePacketInterceptor {

    private final PlayerStatsManager statsManager;

    public StrikePacketInterceptor(@NonNullDecl PlayerStatsManager statsManager) {
        this.statsManager = statsManager;
        PacketAdapters.registerInbound(this::onPacket);
    }

    private boolean onPacket(@NonNullDecl PlayerRef playerRef, @NonNullDecl Packet packet) {
        if (!(packet instanceof SyncInteractionChains syncPacket)) return false;

        for (SyncInteractionChain chain : syncPacket.updates) {
            if (chain.interactionType != InteractionType.Ability1) continue;
            if (chain.data == null || !chain.initial) continue;
            scheduleWindowOpen(playerRef);
            return false;
        }

        return false;
    }

    private void scheduleWindowOpen(@NonNullDecl PlayerRef playerRef) {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return;
        }

        Store<EntityStore> store = ref.getStore();
        store.getExternalData().getWorld().execute(() -> {
            if (!ref.isValid()) return;

            StrikeComponent strike = store.getComponent(ref, StrikeComponent.getComponentType());
            if (strike == null) {
                return;
            }

            if (strike.getState() != StrikeComponent.State.IDLE) {
                return;
            }

            if (!hasEnoughStamina(ref, store)) {
                return;
            }

            drainStamina(ref, store);
            store.putComponent(ref, StrikePendingComponent.getComponentType(), new StrikePendingComponent());
        });
    }

    private boolean hasEnoughStamina(@NonNullDecl Ref<EntityStore> ref,
                                     @NonNullDecl Store<EntityStore> store) {
        float max = statsManager.getMaxStamina(ref, store);
        float current = statsManager.getStamina(ref, store);
        return max > 0f && current >= max * StrikeComponent.STAMINA_COST_RATIO;
    }

    private void drainStamina(@NonNullDecl Ref<EntityStore> ref,
                              @NonNullDecl Store<EntityStore> store) {
        float amount = statsManager.getMaxStamina(ref, store) * StrikeComponent.STAMINA_COST_RATIO;
        statsManager.removeStamina(ref, store, amount);
    }
}
