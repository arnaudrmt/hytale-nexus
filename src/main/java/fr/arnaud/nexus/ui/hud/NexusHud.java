package fr.arnaud.nexus.ui.hud;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.Nexus;
import fr.arnaud.nexus.core.DreamDustHandler;
import fr.arnaud.nexus.core.FlowHandler;
import fr.arnaud.nexus.core.LucidityHandler;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class NexusHud extends CustomUIHud {

    private static final String HUD_FILE = "NexusHud.ui";

    public NexusHud(@NotNull PlayerRef playerRef) {
        super(playerRef);
    }

    @Override
    public void build(@NonNullDecl UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append(HUD_FILE);
    }

    public void refresh(@NonNullDecl Ref<EntityStore> ref,
                        @NonNullDecl Store<EntityStore> store) {
        UICommandBuilder builder = new UICommandBuilder();
        applyLucidity(ref, store, builder);
        applyFlow(ref, store, builder);
        applyDreamDust(ref, store, builder);
        update(false, builder);
    }

    // ---

    private static void applyLucidity(Ref<EntityStore> ref, Store<EntityStore> store,
                                      UICommandBuilder builder) {
        LucidityHandler lucidity = Nexus.get().getLucidityHandler();
        if (!lucidity.isReady()) {
            builder.set("#LucidityDebug.TextSpans", Message.raw("LUCIDITY: not ready"));
            return;
        }
        float pct = lucidity.getNormalized(ref, store);
        builder.set("#LucidityDebug.TextSpans",
            Message.raw(String.format(Locale.ROOT, "LUCIDITY: %d%%", Math.round(pct * 100))));
    }

    private static void applyFlow(Ref<EntityStore> ref, Store<EntityStore> store,
                                  UICommandBuilder builder) {
        FlowHandler flow = Nexus.get().getFlowHandler();
        if (!flow.isReady()) {
            builder.set("#FlowDebug.TextSpans", Message.raw("FLOW: not ready"));
            return;
        }
        int filled = flow.getFilledSegments(ref, store);
        int maxSegs = flow.getMaxSegments(ref, store);
        builder.set("#FlowDebug.TextSpans",
            Message.raw(String.format(Locale.ROOT, "FLOW: %d/%d seg  (full: %s)",
                filled, maxSegs, flow.isFull(ref, store))));
    }

    private static void applyDreamDust(Ref<EntityStore> ref, Store<EntityStore> store,
                                       UICommandBuilder builder) {
        DreamDustHandler dust = Nexus.get().getDreamDustHandler();
        if (!dust.isReady()) {
            builder.set("#DustDebug.TextSpans", Message.raw("DUST: not ready"));
            return;
        }
        builder.set("#DustDebug.TextSpans",
            Message.raw(String.format(Locale.ROOT, "DUST: %.0f", dust.getBalance(ref, store))));
    }

}
