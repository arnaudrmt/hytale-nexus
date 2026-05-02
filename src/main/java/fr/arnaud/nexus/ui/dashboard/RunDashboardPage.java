package fr.arnaud.nexus.ui.dashboard;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.common.util.FormatUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.session.RunSessionComponent;

import javax.annotation.Nonnull;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class RunDashboardPage extends InteractiveCustomUIPage<RunDashboardPage.EventData> {

    private final RunSessionComponent snapshot;

    public RunDashboardPage(@Nonnull PlayerRef playerRef,
                            @Nonnull RunSessionComponent snapshot) {
        super(playerRef, CustomPageLifetime.CanDismiss, EventData.CODEC);
        this.snapshot = snapshot;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder event, @Nonnull Store<EntityStore> store) {
        cmd.append("Pages/RunDashboard.ui");

        event.addEventBinding(CustomUIEventBindingType.Activating, "#StartNewRunButton",
            com.hypixel.hytale.server.core.ui.builder.EventData.of("StartNewRun", ""), false);

        cmd.set("#StatTime.TextSpans", Message.raw(FormatUtil.timeUnitToString(snapshot.getTotalDurationMs(), TimeUnit.MILLISECONDS)));
        cmd.set("#StatScore.TextSpans", Message.raw(String.valueOf(snapshot.computeFinalScore())));
        cmd.set("#StatKills.TextSpans", Message.raw(String.valueOf(snapshot.getKillCount())));
        cmd.set("#StatDeaths.TextSpans", Message.raw(String.valueOf(snapshot.getDeathCount())));
        cmd.set("#StatDamageDealt.TextSpans", Message.raw(String.format(Locale.ROOT, "%.0f", snapshot.getTotalDamageDealt())));
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull EventData data) {
        if (data.close == null) return;

        Store<EntityStore> s = ref.getStore();
        World world = s.getExternalData().getWorld();

        world.execute(() -> {
            if (!ref.isValid()) return;
            Store<EntityStore> ws = ref.getStore();

            Player player = ws.getComponent(ref, Player.getComponentType());
            if (player != null) {
                PlayerRef playerRef = ws.getComponent(ref, PlayerRef.getComponentType());
                if (playerRef != null) {
                    player.getPageManager().setPage(ref, ws, Page.None);
                }
            }

            Nexus.getInstance().getRunStartService().startNewRun(ref, ws, world);
        });
    }

    public static final class EventData {

        public static final BuilderCodec<EventData> CODEC = BuilderCodec
            .builder(EventData.class, EventData::new)
            .append(new KeyedCodec<>("StartNewRunButton", Codec.STRING),
                (d, v) -> d.close = v, d -> d.close)
            .add()
            .build();

        public String close;

        public EventData() {
        }
    }
}
