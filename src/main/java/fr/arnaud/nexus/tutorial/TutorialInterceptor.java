package fr.arnaud.nexus.tutorial;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.MouseButtonState;
import com.hypixel.hytale.protocol.MouseButtonType;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.protocol.packets.window.ClientOpenWindow;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.core.Nexus;

public final class TutorialInterceptor {

    public void register() {
        PacketAdapters.registerInbound(this::interceptAbility2);
        PacketAdapters.registerInbound(this::interceptInventoryOpen);
    }

    private boolean interceptAbility2(PlayerRef playerRef, Packet packet) {
        if (!(packet instanceof SyncInteractionChains syncPacket)) return false;

        for (SyncInteractionChain chain : syncPacket.updates) {
            if (chain.data == null || !chain.initial) continue;
            if (chain.interactionType != InteractionType.Ability2) continue;

            Ref<EntityStore> ref = playerRef.getReference();
            if (ref == null || !ref.isValid()) return false;

            Nexus.get().getTutorialManager()
                 .onTriggerFired(ref, TutorialTriggerType.WEAPON_SWAP);
        }
        return false;
    }

    private boolean interceptInventoryOpen(PlayerRef playerRef, Packet packet) {
        if (!(packet instanceof ClientOpenWindow)) return false;

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) return false;

        Nexus.get().getTutorialManager()
             .onTriggerFired(ref, TutorialTriggerType.INVENTORY_OPEN);

        return false;
    }

    public void onMouseButton(PlayerMouseButtonEvent event) {
        if (event.getMouseButton().mouseButtonType != MouseButtonType.Left) return;
        if (event.getMouseButton().state != MouseButtonState.Pressed) return;

        Ref<EntityStore> ref = event.getPlayer().getReference();
        if (ref == null || !ref.isValid()) return;

        TutorialStepConfig step = Nexus.get().getTutorialManager().getCurrentStep(ref);
        if (step == null || !step.isClickBased()) return;

        Nexus.get().getTutorialManager().onNextClicked(ref);
    }
}
