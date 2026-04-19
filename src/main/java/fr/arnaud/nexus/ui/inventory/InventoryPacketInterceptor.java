package fr.arnaud.nexus.ui.inventory;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.packets.window.ClientOpenWindow;
import com.hypixel.hytale.protocol.packets.window.CloseWindow;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.io.handlers.game.GamePacketHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class InventoryPacketInterceptor {

    private PacketFilter registeredFilter;

    public void register() {
        registeredFilter = (packetHandler, packet) -> {
            if (!(packetHandler instanceof GamePacketHandler gameHandler)) return false;

            if (packet instanceof CloseWindow closeWindow && closeWindow.id == 0) return true;

            if (!(packet instanceof ClientOpenWindow)) return false;

            PlayerRef playerRef = gameHandler.getPlayerRef();
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref == null || !ref.isValid()) return true;

            World world = ref.getStore().getExternalData().getWorld();
            world.execute(() -> {
                Store<EntityStore> s = ref.getStore();
                Player p = s.getComponent(ref, Player.getComponentType());
                if (p == null || !p.getGameMode().equals(GameMode.Adventure)) return;
                p.getPageManager().openCustomPage(ref, s, new NexusInventoryPage(playerRef));
            });

            return true;
        };
        PacketAdapters.registerInbound(registeredFilter);
    }

    public void unregister() {
        if (registeredFilter != null) {
            PacketAdapters.deregisterInbound(registeredFilter);
            registeredFilter = null;
        }
    }
}
