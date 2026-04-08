package fr.arnaud.nexus.ui.inventory;

import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.lang.reflect.Field;
import java.util.logging.Logger;

public final class PacketDiagnostic {

    private static final Logger LOG = Logger.getLogger(PacketDiagnostic.class.getName());

    private PacketFilter outboundFilter;
    private PacketFilter inboundFilter;

    public void register() {
        outboundFilter = PacketAdapters.registerOutbound(
            (PlayerPacketFilter) this::logOutbound
        );
        inboundFilter = PacketAdapters.registerInbound(
            (PlayerPacketFilter) this::logInbound
        );
    }

    public void unregister() {
        if (outboundFilter != null) PacketAdapters.deregisterOutbound(outboundFilter);
        if (inboundFilter != null) PacketAdapters.deregisterInbound(inboundFilter);
    }

    private boolean logOutbound(PlayerRef playerRef, Packet packet) {
        String name = packet.getClass().getSimpleName();
        // Only log UI/window-relevant packets to avoid drowning in block packets
        if (isRelevant(name)) {
            LOG.info("[DIAG] OUT " + name + dumpFields(packet));
        }
        return false;
    }

    private boolean logInbound(PlayerRef playerRef, Packet packet) {
        String name = packet.getClass().getSimpleName();
        if (isRelevant(name)) {
            LOG.info("[DIAG] IN  " + name + dumpFields(packet));
        }
        return false;
    }

    private static boolean isRelevant(String name) {
        return name.contains("Window")
            || name.contains("Page")
            || name.contains("Custom")
            || name.contains("Inventory")
            || name.contains("SetPage")
            || name.contains("Open")
            || name.contains("Close")
            || name.contains("Interaction");
    }

    private static String dumpFields(Packet packet) {
        StringBuilder sb = new StringBuilder(" { ");
        for (Field f : packet.getClass().getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
            try {
                f.setAccessible(true);
                Object val = f.get(packet);
                if (val != null) {
                    String str = val.toString();
                    // Truncate large payloads (commands arrays etc.)
                    if (str.length() > 120) str = str.substring(0, 120) + "...";
                    sb.append(f.getName()).append("=").append(str).append(", ");
                }
            } catch (Exception ignored) {
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
