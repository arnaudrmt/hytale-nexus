package fr.arnaud.nexus.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.ability.ActiveCoreComponent;
import fr.arnaud.nexus.ability.CoreAbility;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.StringJoiner;

/**
 * Dev command for managing Core abilities.
 * Usage: /nexuscore <unlock|equip|unequip|info> [coreId]
 */
public final class AdminCoreCommand extends AbstractPlayerCommand {

    private final RequiredArg<String> subcommandArg = this.withRequiredArg(
        "subcommand", "unlock | equip | unequip | info", ArgTypes.STRING
    );

    private final OptionalArg<String> coreIdArg = this.withOptionalArg(
        "coreId", "Core ability id (e.g. dash, switch_strike)", ArgTypes.STRING
    );

    public AdminCoreCommand() {
        super("nexuscore", "Dev tool for Core ability slot");
    }

    @Override
    protected void execute(
        @NonNullDecl CommandContext context,
        @NonNullDecl Store<EntityStore> store,
        @NonNullDecl Ref<EntityStore> ref,
        @NonNullDecl PlayerRef playerRef,
        @NonNullDecl World world
    ) {
        String subcommand = subcommandArg.get(context).toLowerCase();

        world.execute(() -> {
            switch (subcommand) {
                case "unlock" -> handleUnlock(context, store, ref);
                case "equip" -> handleEquip(context, store, ref);
                case "unequip" -> handleUnequip(context, store, ref);
                case "info" -> handleInfo(context, store, ref);
                default -> context.sendMessage(
                    Message.raw("§cUsage: /nexuscore <unlock|equip|unequip|info> [coreId]")
                );
            }
        });
    }

    private void handleUnlock(CommandContext context, Store<EntityStore> store, Ref<EntityStore> ref) {
        CoreAbility ability = resolveAbilityArg(context);
        if (ability == null) return;

        ActiveCoreComponent core = resolveCore(context, store, ref);
        if (core == null) return;

        boolean wasNew = core.unlock(ability);
        store.putComponent(ref, ActiveCoreComponent.getComponentType(), core);
        context.sendMessage(wasNew
            ? Message.raw("§aUnlocked Core: §f" + ability.getId())
            : Message.raw("§7Core §f" + ability.getId() + "§7 was already unlocked.")
        );
    }

    private void handleEquip(CommandContext context, Store<EntityStore> store, Ref<EntityStore> ref) {
        CoreAbility ability = resolveAbilityArg(context);
        if (ability == null) return;

        ActiveCoreComponent core = resolveCore(context, store, ref);
        if (core == null) return;

        if (!core.equip(ability)) {
            context.sendMessage(Message.raw(
                "§cCore §f" + ability.getId() + "§c is not unlocked. Use §f/nexuscore unlock " + ability.getId() + "§c first."
            ));
            return;
        }

        store.putComponent(ref, ActiveCoreComponent.getComponentType(), core);
        context.sendMessage(Message.raw("§aCore equipped: §f" + ability.getId()));
    }

    private void handleUnequip(CommandContext context, Store<EntityStore> store, Ref<EntityStore> ref) {
        ActiveCoreComponent core = resolveCore(context, store, ref);
        if (core == null) return;

        core.unequip();
        store.putComponent(ref, ActiveCoreComponent.getComponentType(), core);
        context.sendMessage(Message.raw("§aCore slot cleared."));
    }

    private void handleInfo(CommandContext context, Store<EntityStore> store, Ref<EntityStore> ref) {
        ActiveCoreComponent core = resolveCore(context, store, ref);
        if (core == null) return;

        CoreAbility equipped = core.getEquippedCore();
        String equippedDisplay = equipped != null ? "§f" + equipped.getId() : "§7(none)";

        StringJoiner unlocked = new StringJoiner("§7, §f", "§f", "");
        if (core.getUnlockedCores().isEmpty()) {
            unlocked.add("§7(none)");
        } else {
            core.getUnlockedCores().forEach(a -> unlocked.add(a.getId()));
        }

        context.sendMessage(Message.raw(
            "§7Equipped: " + equippedDisplay + "\n§7Unlocked: " + unlocked
        ));
    }

    // --- Helpers ---

    @javax.annotation.Nullable
    private CoreAbility resolveAbilityArg(CommandContext context) {
        String id = coreIdArg.get(context);
        if (id == null) {
            context.sendMessage(Message.raw("§cMissing coreId. Valid ids: " + validIds()));
            return null;
        }
        CoreAbility ability = CoreAbility.fromId(id.toLowerCase());
        if (ability == null) {
            context.sendMessage(Message.raw("§cUnknown Core: §f" + id + "§c. Valid ids: " + validIds()));
        }
        return ability;
    }

    @javax.annotation.Nullable
    private ActiveCoreComponent resolveCore(CommandContext context, Store<EntityStore> store,
                                            Ref<EntityStore> ref) {
        ActiveCoreComponent core = store.getComponent(ref, ActiveCoreComponent.getComponentType());
        if (core == null) {
            context.sendMessage(Message.raw("§cPlayer is missing ActiveCoreComponent."));
        }
        return core;
    }

    private static String validIds() {
        StringJoiner sj = new StringJoiner(", ");
        for (CoreAbility ability : CoreAbility.values()) sj.add(ability.getId());
        return sj.toString();
    }
}
