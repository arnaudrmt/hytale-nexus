package fr.arnaud.nexus.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.feature.resource.PlayerStatsManager;
import fr.arnaud.nexus.item.weapon.component.WeaponInstanceComponent;
import fr.arnaud.nexus.item.weapon.level.WeaponUpgradeService;
import org.bson.BsonDocument;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

/**
 * Dev command for managing weapons.
 * Usage: /nexusweapon <give|stats|levelup|essence> [params]
 */
public final class AdminWeaponCommand extends AbstractPlayerCommand {

    private final RequiredArg<String> subcommandArg = this.withRequiredArg(
        "subcommand", "give | stats | levelup | essence", ArgTypes.STRING
    );
    private final RequiredArg<String> param1Arg = this.withRequiredArg(
        "param1", "Contextual parameter", ArgTypes.STRING
    );

    private final PlayerStatsManager statsManager;
    private final WeaponUpgradeService upgradeService;

    public AdminWeaponCommand(PlayerStatsManager playerStatsManager, WeaponUpgradeService upgradeService) {
        super("nexusweapon", "Dev tool for weapon and leveling system");
        this.statsManager = playerStatsManager;
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

        world.execute(() -> {
            switch (subcommand) {
                case "give" -> handleGive(context, store, ref, param1Arg.get(context));
                case "stats" -> handlePrintStats(context, store, ref);
                case "levelup" -> handleLevelUp(context, store, ref);
                case "essence" -> handleEssence(context, store, ref, param1Arg.get(context));
                default -> context.sendMessage(Message.raw("Unknown subcommand. Use: give, stats, levelup, essence"));
            }
        });
    }

    private void handleGive(
        CommandContext context,
        Store<EntityStore> store,
        Ref<EntityStore> ref,
        String archetypeArg
    ) {

        Item item = Item.getAssetMap().getAsset(archetypeArg);

        if (item == null) return;

        BsonDocument doc = Nexus.getInstance().getWeaponGenerator().generateWeapon(item);
        ItemStack stack = new ItemStack(archetypeArg, 1, doc);

        InventoryComponent.Hotbar hotbar = store.getComponent(ref, InventoryComponent.Hotbar.getComponentType());
        if (hotbar == null) {
            context.sendMessage(Message.raw("Player has no hotbar component."));
            return;
        }

        hotbar.getInventory().addItemStack(stack);
        hotbar.markDirty();
    }

    private void handlePrintStats(
        CommandContext context,
        Store<EntityStore> store,
        Ref<EntityStore> ref
    ) {
        WeaponInstanceComponent instance = store.getComponent(ref, WeaponInstanceComponent.getComponentType());
        if (instance == null) {
            context.sendMessage(Message.raw("No weapon equipped."));
        }
    }

    private void handleLevelUp(
        CommandContext context,
        Store<EntityStore> store,
        Ref<EntityStore> ref
    ) {
        InventoryComponent.Hotbar hotbar = store.getComponent(ref, InventoryComponent.Hotbar.getComponentType());
        if (hotbar == null) {
            context.sendMessage(Message.raw("No hotbar found."));
            return;
        }

        ItemStack stack = hotbar.getInventory().getItemStack((short) 0);
        if (stack == null || stack.getMetadata() == null) {
            context.sendMessage(Message.raw("No weapon in slot 0."));
            return;
        }

        BsonDocument doc = stack.getMetadata();
        WeaponUpgradeService.UpgradeResult result = upgradeService.attemptUpgrade(ref, doc, store);

        if (result.success()) {
            hotbar.getInventory().setItemStackForSlot((short) 0, stack);
            hotbar.markDirty();

        } else {
            context.sendMessage(Message.raw(
                "Upgrade failed: " + result.failureReason() +
                    (result.requiredEssence() > 0
                        ? "\nRequired: " + String.format("%.1f", result.requiredEssence()) +
                          " / Balance: " + String.format("%.1f", result.currentBalance())
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
            context.sendMessage(Message.raw("Amount must be a number."));
            return;
        }

        statsManager.addEssenceDust(ref, store, amount);
        float newBalance = statsManager.getEssenceDust(ref, store);
        context.sendMessage(Message.raw(
            "Added " + String.format("%.1f", amount) + " Essence Dust\n" +
                "New balance: " + String.format("%.1f", newBalance)
        ));
    }
}
