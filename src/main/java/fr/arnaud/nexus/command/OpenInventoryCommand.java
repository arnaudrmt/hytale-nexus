package fr.arnaud.nexus.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.ui.inventory.NexusInventoryPage;

import javax.annotation.Nonnull;

/**
 * Dev command to open the custom inventory.
 * Usage: /openinv
 */
public final class OpenInventoryCommand extends AbstractPlayerCommand {

    public OpenInventoryCommand() {
        super("openinv", "description");
    }

    @Override
    protected void execute(
        @Nonnull CommandContext commandContext,
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> ref,
        @Nonnull PlayerRef playerRef,
        @Nonnull World world
    ) {

        world.execute(() -> {
            if (!ref.isValid()) return;
            Store<EntityStore> s = ref.getStore();

            Player p = s.getComponent(ref, Player.getComponentType());
            if (p == null) return;

            p.getPageManager().openCustomPage(ref, s, new NexusInventoryPage(playerRef));
        });
    }
}
