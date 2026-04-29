package fr.arnaud.nexus.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.component.RunSessionComponent;
import fr.arnaud.nexus.ui.dashboard.RunDashboardPage;

import javax.annotation.Nonnull;

/**
 * Dev command to open the run dashboard.
 * Usage: /opendashboard
 */
public final class OpenDashboardCommand extends AbstractPlayerCommand {

    public OpenDashboardCommand() {
        super("opendashboard", "Dev command to open the run dashboard with current session stats");
    }

    @Override
    protected void execute(
        @Nonnull CommandContext context,
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> ref,
        @Nonnull PlayerRef playerRef,
        @Nonnull World world
    ) {
        world.execute(() -> {
            if (!ref.isValid()) return;
            Store<EntityStore> s = ref.getStore();

            Player player = s.getComponent(ref, Player.getComponentType());
            if (player == null) return;

            RunSessionComponent session = s.getComponent(ref, RunSessionComponent.getComponentType());
            if (session == null) return;

            player.getPageManager().openCustomPage(ref, s,
                new RunDashboardPage(playerRef, session.clone()));
        });
    }
}
