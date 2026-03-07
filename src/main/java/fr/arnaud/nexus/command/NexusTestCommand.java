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
import fr.arnaud.nexus.camera.CameraComponent;
import fr.arnaud.nexus.camera.CameraPacketBuilder;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

/**
 * Developer test command: /nexustest <mode>
 */
public final class NexusTestCommand extends AbstractPlayerCommand {

    private final RequiredArg<String> modeArg = this.withRequiredArg("mode", "Camera mode: iso, firstperson, entry, exit", ArgTypes.STRING);

    public NexusTestCommand() {
        super("nexustest", "Test Nexus camera system");
    }

    @Override
    protected void execute(@NonNullDecl CommandContext context,
                           @NonNullDecl Store<EntityStore> store,
                           @NonNullDecl Ref<EntityStore> ref,
                           @NonNullDecl PlayerRef playerRef,
                           @NonNullDecl World world) {

        String mode = modeArg.get(context).toLowerCase();

        // Ensure we are on the world thread before modifying components
        world.execute(() -> {
            CameraComponent cam = store.getComponent(ref, CameraComponent.getComponentType());
            if (cam == null) {
                context.sendMessage(Message.raw("§cCameraComponent not found."));
                return;
            }

            switch (mode) {
                case "iso" -> {
                    cam.completeGlimpseExit();
                    playerRef.getPacketHandler().writeNoCache(CameraPacketBuilder.buildIso(cam));
                    context.sendMessage(Message.raw("§aSwitched to ISO mode."));
                }
                case "firstperson" -> {
                    cam.completeGlimpseEntry();
                    playerRef.getPacketHandler().writeNoCache(CameraPacketBuilder.buildGlimpseActive());
                    context.sendMessage(Message.raw("§aSwitched to First-Person mode."));
                }
                case "entry" -> {
                    // Directly trigger the state change on the component itself
                    if (cam.beginGlimpseEntry()) {
                        context.sendMessage(Message.raw("§aPlaying entry transition..."));
                    } else {
                        context.sendMessage(Message.raw("§cCannot play entry (must be in ISO)."));
                    }
                }
                case "exit" -> {
                    // Directly trigger the state change on the component itself
                    if (cam.beginGlimpseExit()) {
                        context.sendMessage(Message.raw("§aPlaying exit transition..."));
                    } else {
                        context.sendMessage(Message.raw("§cCannot play exit (must be in Glimpse)."));
                    }
                }
                default -> context.sendMessage(Message.raw("§cUnknown mode. Use: iso, firstperson, entry, exit"));
            }
        });
    }
}