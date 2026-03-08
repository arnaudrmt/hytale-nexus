package fr.arnaud.nexus.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.component.RunSessionComponent;
import fr.arnaud.nexus.i18n.I18n;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.jetbrains.annotations.NotNull;

public final class StartRunCommand extends AbstractPlayerCommand {

    private final OptionalArg<String> modeArgs =
        this.withOptionalArg("mode", "Optional: 'hard' to start in Hard Mode", ArgTypes.STRING);

    public StartRunCommand() {
        super("startrun", "Start or restart a Nexus run");
    }

    @Override
    protected void execute(@NotNull CommandContext context,
                           @NotNull Store<EntityStore> store,
                           @NotNull Ref<EntityStore> ref,
                           @NotNull PlayerRef playerRef,
                           @NotNull World world) {

        boolean hardMode = modeArgs.get(context).equalsIgnoreCase("hard");
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;

        world.execute(() -> {
            initOrResetRunComponents(ref, store, hardMode);

            String key = hardMode ? "command.startrun.started_hard" : "command.startrun.started";
            context.sendMessage(Message.raw(I18n.t(key)));
        });
    }

    /**
     * Attaches run-scoped components if absent, or resets them if already present,
     * then equips starter weapons into protected utility slots.
     */
    public static void initOrResetRunComponents(
        @NonNullDecl Ref<EntityStore> ref,
        @NonNullDecl Store<EntityStore> store,
        boolean hardMode) {

        resetFlowComponent(ref, store);
        resetLucidityComponent(ref, store);
        resetDreamDustComponent(ref, store);
        resetRunSessionComponent(ref, store, hardMode);
    }

    // --- Component resets ---

    private static void resetFlowComponent(Ref<EntityStore> ref, Store<EntityStore> store) {
        // TODO: Reset Flow
    }

    private static void resetLucidityComponent(Ref<EntityStore> ref, Store<EntityStore> store) {
        // TODO: Reset Lucidity
    }

    private static void resetDreamDustComponent(Ref<EntityStore> ref, Store<EntityStore> store) {
        // TODO: Reset Dream Dust
    }

    private static void resetRunSessionComponent(Ref<EntityStore> ref, Store<EntityStore> store,
                                                 boolean hardMode) {
        RunSessionComponent session = store.getComponent(ref, RunSessionComponent.getComponentType());
        if (session == null) {
            session = new RunSessionComponent();
        }
        session.startRun(hardMode);
        store.putComponent(ref, RunSessionComponent.getComponentType(), session);
    }
}
