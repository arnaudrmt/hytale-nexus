package fr.arnaud.nexus.ui.inventory;

import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.window.ClientOpenWindow;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public final class InventoryPacketInterceptor {

    private PacketFilter registeredFilter;

    public void register() {
        registeredFilter = PacketAdapters.registerInbound(
            this::interceptClientOpenWindow
        );
    }

    public void unregister() {
        if (registeredFilter != null) {
            PacketAdapters.deregisterInbound(registeredFilter);
            registeredFilter = null;
        }
    }

    private boolean interceptClientOpenWindow(PlayerRef playerRef, Packet packet) {
        if (!(packet instanceof ClientOpenWindow)) return false;

        /*Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) return false;

        World world = ref.getStore().getExternalData().getWorld();

        world.execute(() -> {
            Store<EntityStore> s = ref.getStore();

            Player p = s.getComponent(ref, Player.getComponentType());
            if (p == null) return;

            p.getPageManager().openCustomPage(ref, s, new NexusInventoryPage(playerRef));
        });*/

        return true;
    }
}
