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
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

/**
 * Listens for Ability2 (weapon swap) on the Netty thread.
 */
public final class StrikeWeaponSwapInterceptor {

    public StrikeWeaponSwapInterceptor() {
        PacketAdapters.registerInbound(this::onPacket);
    }

    private boolean onPacket(@NonNullDecl PlayerRef playerRef, @NonNullDecl Packet packet) {
        if (!(packet instanceof SyncInteractionChains syncPacket)) return false;

        for (SyncInteractionChain chain : syncPacket.updates) {
            if (chain.interactionType != InteractionType.Ability2) continue;
            if (chain.data == null || !chain.initial) continue;
            scheduleComboEntry(playerRef);
            return false;
        }

        return false;
    }

    private void scheduleComboEntry(@NonNullDecl PlayerRef playerRef) {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) return;

        Store<EntityStore> store = ref.getStore();
        store.getExternalData().getWorld().execute(() -> {
            if (!ref.isValid()) return;
            StrikeComponent strike = store.getComponent(ref, StrikeComponent.getComponentType());
            if (strike == null || strike.getState() != StrikeComponent.State.SWITCH_WINDOW) return;
            store.putComponent(ref, StrikeSwapConfirmedComponent.getComponentType(), new StrikeSwapConfirmedComponent());
        });
    }
}
