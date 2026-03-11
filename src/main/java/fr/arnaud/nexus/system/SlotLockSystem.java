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
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public final class SlotLockSystem {

    public SlotLockSystem() {
        PacketAdapters.registerInbound((PlayerPacketFilter) this::filterSlotSwitch);
    }

    private boolean filterSlotSwitch(@NonNullDecl PlayerRef playerRef, @NonNullDecl Packet packet) {
        if (!(packet instanceof SyncInteractionChains syncPacket)) {
            return false;
        }

        for (SyncInteractionChain chain : syncPacket.updates) {
            if (chain.interactionType != InteractionType.SwapFrom) continue;
            if (chain.data == null || !chain.initial) continue;

            revertClientSlot(playerRef, 0);
            return true;
        }

        return false;
    }

    private void revertClientSlot(@NonNullDecl PlayerRef playerRef, int slot) {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null) return;

        Store<EntityStore> store = ref.getStore();
        store.getExternalData().getWorld().execute(() -> {
            Player player = store.getComponent(ref, Player.getComponentType());
            player.getInventory().setActiveHotbarSlot((byte) slot);

            SetActiveSlot correction = new SetActiveSlot(Inventory.HOTBAR_SECTION_ID, slot);
            playerRef.getPacketHandler().write(correction);
        });
    }
}
