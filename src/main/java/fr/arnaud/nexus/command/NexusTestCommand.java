package fr.arnaud.nexus.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.breach.BreachSequenceComponent;
import fr.arnaud.nexus.camera.CameraComponent;
import fr.arnaud.nexus.camera.CameraPacketBuilder;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

/**
 * Developer test command: /nexustest <mode>
 * <p>
 * Camera modes: iso, firstperson, entry, exit
 * Breach modes: breach_start, breach_stop
 * Time modes:   time_slow, time_normal
 */
public final class NexusTestCommand extends AbstractPlayerCommand {

    private final RequiredArg<String> modeArg = this.withRequiredArg(
        "mode",
        "iso | firstperson | entry | exit | breach_start | breach_stop | time_slow | time_normal",
        ArgTypes.STRING
    );

    public NexusTestCommand() {
        super("nexustest", "Test Nexus systems");
    }

    @Override
    protected void execute(@NonNullDecl CommandContext context,
                           @NonNullDecl Store<EntityStore> store,
                           @NonNullDecl Ref<EntityStore> ref,
                           @NonNullDecl PlayerRef playerRef,
                           @NonNullDecl World world) {
        String mode = modeArg.get(context).toLowerCase();

        world.execute(() -> {
            switch (mode) {
                case "iso" -> handleIso(context, store, ref, playerRef);
                case "firstperson" -> handleFirstPerson(context, store, ref, playerRef);
                case "entry" -> handleEntry(context, store, ref);
                case "exit" -> handleExit(context, store, ref);
                case "breach_start" -> handleBreachStart(context, store, ref);
                case "breach_stop" -> handleBreachStop(context, store, ref, world);
                case "time_slow" -> handleTimeSlow(context, store, world);
                case "time_normal" -> handleTimeNormal(context, store, world);
                default -> context.sendMessage(Message.raw(
                    "§cUnknown mode. Use: iso, firstperson, entry, exit, breach_start, breach_stop, time_slow, time_normal"));
            }
        });
    }

    // --- Camera handlers ---

    private void handleIso(@NonNullDecl CommandContext context,
                           @NonNullDecl Store<EntityStore> store,
                           @NonNullDecl Ref<EntityStore> ref,
                           @NonNullDecl PlayerRef playerRef) {
        CameraComponent cam = store.getComponent(ref, CameraComponent.getComponentType());
        if (cam == null) {
            context.sendMessage(Message.raw("§cCameraComponent not found."));
            return;
        }
        cam.completeGlimpseExit();
        playerRef.getPacketHandler().writeNoCache(CameraPacketBuilder.buildIso(cam));
        context.sendMessage(Message.raw("§aSwitched to ISO mode."));
    }

    private void handleFirstPerson(@NonNullDecl CommandContext context,
                                   @NonNullDecl Store<EntityStore> store,
                                   @NonNullDecl Ref<EntityStore> ref,
                                   @NonNullDecl PlayerRef playerRef) {
        CameraComponent cam = store.getComponent(ref, CameraComponent.getComponentType());
        if (cam == null) {
            context.sendMessage(Message.raw("§cCameraComponent not found."));
            return;
        }
        cam.completeGlimpseEntry();
        playerRef.getPacketHandler().writeNoCache(CameraPacketBuilder.buildGlimpseActive());
        context.sendMessage(Message.raw("§aSwitched to First-Person mode."));
    }

    private void handleEntry(@NonNullDecl CommandContext context,
                             @NonNullDecl Store<EntityStore> store,
                             @NonNullDecl Ref<EntityStore> ref) {
        CameraComponent cam = store.getComponent(ref, CameraComponent.getComponentType());
        if (cam == null) {
            context.sendMessage(Message.raw("§cCameraComponent not found."));
            return;
        }
        if (cam.beginGlimpseEntry()) {
            context.sendMessage(Message.raw("§aPlaying entry transition..."));
        } else {
            context.sendMessage(Message.raw("§cCannot play entry (must be in ISO)."));
        }
    }

    private void handleExit(@NonNullDecl CommandContext context,
                            @NonNullDecl Store<EntityStore> store,
                            @NonNullDecl Ref<EntityStore> ref) {
        CameraComponent cam = store.getComponent(ref, CameraComponent.getComponentType());
        if (cam == null) {
            context.sendMessage(Message.raw("§cCameraComponent not found."));
            return;
        }
        if (cam.beginGlimpseExit()) {
            context.sendMessage(Message.raw("§aPlaying exit transition..."));
        } else {
            context.sendMessage(Message.raw("§cCannot play exit (must be in Glimpse)."));
        }
    }

    // --- Breach handlers ---

    private void handleBreachStart(@NonNullDecl CommandContext context,
                                   @NonNullDecl Store<EntityStore> store,
                                   @NonNullDecl Ref<EntityStore> ref) {
        BreachSequenceComponent breach = store.getComponent(ref, BreachSequenceComponent.getComponentType());
        if (breach == null) {
            context.sendMessage(Message.raw("§cBreachSequenceComponent not found."));
            return;
        }
        if (breach.getState() != BreachSequenceComponent.State.IDLE) {
            context.sendMessage(Message.raw("§cBreach sequence already running."));
            return;
        }

        // In debug mode we pass the player's own ref as the boss so there is no
        // dependency on a live NPC being present. We're already on the world thread
        // inside world.execute(), so direct store mutation is safe here.
        boolean started = breach.begin(ref);
        if (started) {
            store.putComponent(ref, BreachSequenceComponent.getComponentType(), breach);
        }
        context.sendMessage(started
            ? Message.raw("§aBreach sequence started (debug — self as boss).")
            : Message.raw("§cFailed to start breach sequence."));
    }

    private void handleBreachStop(@NonNullDecl CommandContext context,
                                  @NonNullDecl Store<EntityStore> store,
                                  @NonNullDecl Ref<EntityStore> ref,
                                  @NonNullDecl World world) {
        BreachSequenceComponent breach = store.getComponent(ref, BreachSequenceComponent.getComponentType());
        if (breach == null) {
            context.sendMessage(Message.raw("§cBreachSequenceComponent not found."));
            return;
        }
        if (breach.getState() == BreachSequenceComponent.State.IDLE) {
            context.sendMessage(Message.raw("§cNo breach sequence is running."));
            return;
        }
        breach.beginExit(false);
        store.putComponent(ref, BreachSequenceComponent.getComponentType(), breach);
        context.sendMessage(Message.raw("§aBreach sequence force-stopped."));
    }

    // --- Time dilation handlers ---

    private void handleTimeSlow(@NonNullDecl CommandContext context,
                                @NonNullDecl Store<EntityStore> store,
                                @NonNullDecl World world) {
        try {
            World.setTimeDilation(BreachSequenceComponent.TIME_DILATION, store);
            context.sendMessage(Message.raw("§aTime dilation set to " + BreachSequenceComponent.TIME_DILATION + "x."));
        } catch (Exception e) {
            context.sendMessage(Message.raw("§csetTimeDilation failed: " + e.getMessage()));
        }
    }

    private void handleTimeNormal(@NonNullDecl CommandContext context,
                                  @NonNullDecl Store<EntityStore> store,
                                  @NonNullDecl World world) {
        try {
            World.setTimeDilation(1.0f, store);
            context.sendMessage(Message.raw("§aTime dilation restored to 1.0x."));
        } catch (Exception e) {
            context.sendMessage(Message.raw("§csetTimeDilation failed: " + e.getMessage()));
        }
    }
}
