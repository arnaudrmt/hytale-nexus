package fr.arnaud.nexus.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

/**
 * Developer command for inspecting live EntityStat values on the executing player.
 * <p>
 * Usage: {@code /nexusstats}
 * <p>
 * Dumps every stat index currently present in the player's {@link EntityStatMap}
 * so that test weapons can be verified without needing a debugger.
 * Remove before shipping.
 */
public final class NexusTestCommand extends AbstractPlayerCommand {

    public NexusTestCommand() {
        super("nexusstats", "Dump all EntityStat values for the executing player");
    }

    @Override
    protected void execute(@NonNullDecl CommandContext context,
                           @NonNullDecl Store<EntityStore> store,
                           @NonNullDecl Ref<EntityStore> ref,
                           @NonNullDecl PlayerRef playerRef,
                           @NonNullDecl World world) {

        world.execute(() -> {
            EntityStatMap stats = store.getComponent(ref, EntityStatMap.getComponentType());

            if (stats == null) {
                context.sendMessage(Message.raw("§c[Nexus] No EntityStatMap found on this player."));
                return;
            }

            context.sendMessage(Message.raw("§e[Nexus] === EntityStatMap dump ==="));
            for (int i = 0; i < stats.size(); i++) {
                EntityStatValue statValue = stats.get(i);
                if (statValue == null) continue;

                String line = "§7  [" + i + "] §f" + statValue.getId()
                    + " §7= §a" + statValue.get()
                    + " §7(max: " + statValue.getMax() + ")";
                context.sendMessage(Message.raw(line));
                System.out.println(line);
            }
            context.sendMessage(Message.raw("§e[Nexus] === End of dump ==="));
        });
    }
}
