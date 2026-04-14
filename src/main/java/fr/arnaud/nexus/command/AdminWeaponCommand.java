package fr.arnaud.nexus.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemQuality;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.feature.ressource.EssenceDustManager;
import fr.arnaud.nexus.item.weapon.component.WeaponInstanceComponent;
import fr.arnaud.nexus.item.weapon.data.WeaponTag;
import fr.arnaud.nexus.item.weapon.level.WeaponUpgradeService;
import org.bson.BsonDocument;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public final class AdminWeaponCommand extends AbstractPlayerCommand {

    private final RequiredArg<String> subcommandArg = this.withRequiredArg(
        "subcommand", "give | stats | levelup | essence", ArgTypes.STRING
    );
    private final RequiredArg<String> param1Arg = this.withRequiredArg(
        "param1", "Contextual parameter", ArgTypes.STRING
    );
    private final OptionalArg<String> param2Arg = this.withOptionalArg(
        "param2", "Optional parameter", ArgTypes.STRING
    );

    private final EssenceDustManager essenceManager;
    private final WeaponUpgradeService upgradeService;

    public AdminWeaponCommand(EssenceDustManager essenceManager, WeaponUpgradeService upgradeService) {
        super("nexusweapon", "Dev tool for weapon and leveling system");
        this.essenceManager = essenceManager;
        this.upgradeService = upgradeService;
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
        String defaultQuality = param2Arg.get(context) != null ? param2Arg.get(context) : "Common";

        world.execute(() -> {
            switch (subcommand) {
                case "give" -> handleGive(context, store, ref, param1Arg.get(context), defaultQuality);
                case "stats" -> handlePrintStats(context, store, ref);
                case "levelup" -> handleLevelUp(context, store, ref);
                case "essence" -> handleEssence(context, store, ref, param1Arg.get(context));
                default -> context.sendMessage(Message.raw("§cUnknown subcommand. Use: give, stats, levelup, essence"));
            }
        });
    }

    private void handleGive(
        CommandContext context,
        Store<EntityStore> store,
        Ref<EntityStore> ref,
        String typeArg,
        String qualityIdArg
    ) {
        WeaponTag tag = parseTag(typeArg);
        if (tag == null) {
            context.sendMessage(Message.raw("§cUnknown type. Use: melee, ranged"));
            return;
        }

        ItemQuality quality = ItemQuality.getAssetMap().getAsset(qualityIdArg);
        if (quality == null) {
            context.sendMessage(Message.raw("§cUnknown quality ID: " + qualityIdArg));
            return;
        }

        String archetypeId = tag == WeaponTag.MELEE
            ? "Nexus_Melee_Sword_Default"
            : "Nexus_Ranged_Staff_Default";

        Item item = Item.getAssetMap().getAsset(archetypeId);

        BsonDocument doc = Nexus.get().getWeaponGenerator().generateWeapon(item);
        ItemStack stack = new ItemStack(archetypeId, 1, doc);

        InventoryComponent.Hotbar hotbar = store.getComponent(ref, InventoryComponent.Hotbar.getComponentType());
        if (hotbar == null) {
            context.sendMessage(Message.raw("§cPlayer has no hotbar component."));
            return;
        }

        hotbar.getInventory().setItemStackForSlot((short) 0, stack);
        hotbar.markDirty();
    }

    private void handlePrintStats(
        CommandContext context,
        Store<EntityStore> store,
        Ref<EntityStore> ref
    ) {
        WeaponInstanceComponent instance = store.getComponent(ref, WeaponInstanceComponent.getComponentType());
        if (instance == null) {
            context.sendMessage(Message.raw("§cNo weapon equipped."));
            return;
        }
    }

    private void handleLevelUp(
        CommandContext context,
        Store<EntityStore> store,
        Ref<EntityStore> ref
    ) {
        InventoryComponent.Hotbar hotbar = store.getComponent(ref, InventoryComponent.Hotbar.getComponentType());
        if (hotbar == null) {
            context.sendMessage(Message.raw("§cNo hotbar found."));
            return;
        }

        ItemStack stack = hotbar.getInventory().getItemStack((short) 0);
        if (stack == null || stack.getMetadata() == null) {
            context.sendMessage(Message.raw("§cNo weapon in slot 0."));
            return;
        }

        BsonDocument doc = stack.getMetadata();
        WeaponUpgradeService.UpgradeResult result = upgradeService.attemptUpgrade(ref, doc, store);

        if (result.success()) {
            hotbar.getInventory().setItemStackForSlot((short) 0, stack);
            hotbar.markDirty();

        } else {
            context.sendMessage(Message.raw(
                "§cUpgrade failed: " + result.failureReason() +
                    (result.requiredEssence() > 0
                        ? "\n§7Required: §f" + String.format("%.1f", result.requiredEssence()) +
                          " §7/ Balance: §f" + String.format("%.1f", result.currentBalance())
                        : "")
            ));
        }
    }

    private void handleEssence(
        CommandContext context,
        Store<EntityStore> store,
        Ref<EntityStore> ref,
        String amountArg
    ) {
        float amount;
        try {
            amount = Float.parseFloat(amountArg);
        } catch (NumberFormatException e) {
            context.sendMessage(Message.raw("§cAmount must be a number."));
            return;
        }

        essenceManager.addEssenceDust(ref, store, amount);
        float newBalance = essenceManager.getBalance(ref, store);
        context.sendMessage(Message.raw(
            "§aAdded §f" + String.format("%.1f", amount) + " §aEssence Dust\n" +
                "§7New balance: §f" + String.format("%.1f", newBalance)
        ));
    }

    private WeaponTag parseTag(String s) {
        return switch (s.toLowerCase()) {
            case "melee" -> WeaponTag.MELEE;
            case "ranged" -> WeaponTag.RANGED;
            default -> null;
        };
    }
}
