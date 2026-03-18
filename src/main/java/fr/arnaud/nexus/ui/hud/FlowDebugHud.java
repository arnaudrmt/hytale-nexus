package fr.arnaud.nexus.ui.hud;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.Nexus;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.Locale;

public final class FlowDebugHud extends CustomUIHud {

    private static final String HUD_FILE = "FlowDebugHud.ui";

    public FlowDebugHud(@NonNullDecl PlayerRef playerRef) {
        super(playerRef);
    }

    @Override
    public void build(@NonNullDecl UICommandBuilder cmd) {
        cmd.append(HUD_FILE);
    }

    public void refresh(@NonNullDecl Ref<EntityStore> ref,
                        @NonNullDecl Store<EntityStore> store) {
        UICommandBuilder cmd = new UICommandBuilder();
        applyFlow(ref, store, cmd);
        update(false, cmd);
    }

    private static void applyFlow(Ref<EntityStore> ref, Store<EntityStore> store,
                                  UICommandBuilder cmd) {
        var flow = Nexus.get().getFlowHandler();
        if (!flow.isReady()) {
            cmd.set("#FlowDebugLabel.TextSpans", Message.raw("FLOW: not ready"));
            return;
        }
        int filled = flow.getFilledSegments(ref, store);
        int max = flow.getMaxSegments(ref, store);
        cmd.set("#FlowDebugLabel.TextSpans",
            Message.raw(String.format(Locale.ROOT, "FLOW: %d / %d", filled, max)));
    }
}
