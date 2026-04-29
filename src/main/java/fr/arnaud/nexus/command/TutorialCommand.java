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
import fr.arnaud.nexus.core.Nexus;

import javax.annotation.Nonnull;

/**
 * Player command to re-play the tutorial.
 * Usage: /tutorial
 */
public final class TutorialCommand extends AbstractPlayerCommand {

    public TutorialCommand() {
        super("tutorial", "Restart the Nexus tutorial");
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

            RunSessionComponent session = s.getComponent(ref, RunSessionComponent.getComponentType());
            if (session == null) return;

            session.markTutorialCompleted(false);
            s.putComponent(ref, RunSessionComponent.getComponentType(), session);

            Player player = s.getComponent(ref, Player.getComponentType());
            if (player == null) return;

            Nexus.get().getTutorialManager().removePlayer(ref);
            Nexus.get().getTutorialManager().onPlayerReady(player);
        });
    }
}
