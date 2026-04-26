package fr.arnaud.nexus.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.component.RunSessionComponent;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.List;

public final class AdminRunCommand extends AbstractPlayerCommand {

    public AdminRunCommand() {
        super("nexusrun", "Dev tool to inspect the current run session");
    }

    @Override
    protected void execute(
        @NonNullDecl CommandContext context,
        @NonNullDecl Store<EntityStore> store,
        @NonNullDecl Ref<EntityStore> ref,
        @NonNullDecl PlayerRef playerRef,
        @NonNullDecl World world
    ) {
        world.execute(() -> {
            RunSessionComponent session = store.getComponent(ref, RunSessionComponent.getComponentType());
            if (session == null) {
                context.sendMessage(Message.raw("No active run session found."));
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("--- Run Session ---\n");
            sb.append("Total time:    ").append(formatMs(session.getTotalDurationMs())).append("\n");

            List<Long> splits = session.getLevelSplits();
            if (!splits.isEmpty()) {
                sb.append("Level splits:\n");
                for (int i = 0; i < splits.size(); i++) {
                    sb.append("  Level ").append(i + 1).append(":    ")
                      .append(formatMs(splits.get(i))).append("\n");
                }
            }

            sb.append("Current level: ").append(formatMs(session.getCurrentLevelDurationMs())).append("\n");
            sb.append("Score:         ").append(session.computeFinalScore()).append("\n");
            sb.append("Kills: ").append(session.getKillCount())
              .append("  Deaths: ").append(session.getDeathCount());

            context.sendMessage(Message.raw(sb.toString()));
        });
    }

    /**
     * Formats milliseconds as mm:ss:SSS
     */
    private static String formatMs(long ms) {
        long minutes = ms / 60_000;
        long seconds = (ms % 60_000) / 1_000;
        long millis = ms % 1_000;
        return String.format("%02d:%02d:%03d", minutes, seconds, millis);
    }
}
