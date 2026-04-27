package fr.arnaud.nexus.item.weapon.system;

import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public final class WeaponAbilityGuard {

    public WeaponAbilityGuard() {
        PacketAdapters.registerInbound(this::interceptAbilityUse);
    }

    private boolean interceptAbilityUse(
        @NonNullDecl PlayerRef playerRef,
        @NonNullDecl Packet packet
    ) {
        if (!(packet instanceof SyncInteractionChains syncPacket)) return false;

        for (SyncInteractionChain chain : syncPacket.updates) {
            if (chain.data == null || !chain.initial) continue;
            if (chain.interactionType != InteractionType.Primary
                && chain.interactionType != InteractionType.Secondary) continue;

            //if (chain.activeHotbarSlot != 0) return true;
        }

        return false;
    }
}
