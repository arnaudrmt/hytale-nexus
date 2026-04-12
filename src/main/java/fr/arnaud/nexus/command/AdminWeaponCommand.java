package fr.arnaud.nexus.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
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
import fr.arnaud.nexus.item.weapon.component.WeaponInstanceComponent;
import fr.arnaud.nexus.item.weapon.component.WeaponInstanceComponent.ActiveEnchantmentState;
import fr.arnaud.nexus.item.weapon.data.EnchantmentSlot;
import fr.arnaud.nexus.item.weapon.data.WeaponBsonSchema;
import fr.arnaud.nexus.item.weapon.data.WeaponRarity;
import fr.arnaud.nexus.item.weapon.data.WeaponTag;
import fr.arnaud.nexus.item.weapon.upgrade.UpgradeResult;
import fr.arnaud.nexus.item.weapon.upgrade.WeaponUpgradeProvider;
import org.bson.BsonDocument;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.List;

public final class AdminWeaponCommand extends AbstractPlayerCommand {

    private final RequiredArg<String> subcommandArg = this.withRequiredArg(
        "subcommand", "give | enchants | activate | equip", ArgTypes.STRING
    );
    private final RequiredArg<String> param1Arg = this.withRequiredArg(
        "param1", "For give: melee|ranged  /  For activate: slot index (0-2)", ArgTypes.STRING
    );
    private final RequiredArg<String> param2Arg = this.withRequiredArg(
        "param2", "For give: common|rare|epic|legendary", ArgTypes.STRING
    );

    public AdminWeaponCommand() {
        super("nexusweapon", "Dev tool for testing the weapon and enchantment system");
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
                case "give" -> handleGive(context, store, ref, param1Arg.get(context), param2Arg.get(context));
                case "enchants" -> handlePrintEnchants(context, store, ref);
                case "activate" -> handleActivateSlot(context, store, ref, param1Arg.get(context));
                case "equip" -> handleForceEquip(context, store, ref);
                default ->
                    context.sendMessage(Message.raw("§cUnknown subcommand. Use: give, enchants, activate, equip"));
            }
        });
    }

    private void handleGive(
        CommandContext context,
        Store<EntityStore> store,
        Ref<EntityStore> ref,
        String typeArg,
        String rarityArg
    ) {
        WeaponTag tag = parseTag(typeArg);
        WeaponRarity rarity = parseRarity(rarityArg);
        if (tag == null) {
            context.sendMessage(Message.raw("§cUnknown type. Use: melee, ranged"));
            return;
        }
        if (rarity == null) {
            context.sendMessage(Message.raw("§cUnknown rarity. Use: common, rare, epic, legendary"));
            return;
        }

        String archetypeId = tag == WeaponTag.MELEE
            ? "Nexus_Weapon_Sword_Default"
            : "Nexus_Weapon_Staff_Default";

        BsonDocument doc = Nexus.get().getWeaponGenerator().generate(archetypeId, tag, rarity);
        ItemStack stack = new ItemStack(archetypeId, 1, doc);

        InventoryComponent.Hotbar hotbar = store.getComponent(ref, InventoryComponent.Hotbar.getComponentType());
        if (hotbar == null) {
            context.sendMessage(Message.raw("§cPlayer has no hotbar component."));
            return;
        }

        hotbar.getInventory().setItemStackForSlot((short) 0, stack);
        hotbar.markDirty();

        StringBuilder sb = new StringBuilder();
        sb.append("§aGave §f").append(rarity.name()).append(" §a")
          .append(tag.name()).append(" §aweapon → hotbar slot 0\n");
        sb.append("§7Damage multiplier: §f").append(rarity.getDamageMultiplier()).append("x\n");

        List<EnchantmentSlot> slots = WeaponBsonSchema.readEnchantmentSlots(doc);
        if (slots.isEmpty()) {
            sb.append("§7No enchantment slots (Common weapon).\n");
        }
        for (EnchantmentSlot slot : slots) {
            sb.append("§7Slot ").append(slot.slotIndex()).append(": §f")
              .append(slot.choiceA()).append(" §7vs §f").append(slot.choiceB()).append("\n");
        }

        context.sendMessage(Message.raw(sb.toString()));
    }

    private void handlePrintEnchants(
        CommandContext context,
        Store<EntityStore> store,
        Ref<EntityStore> ref
    ) {
        WeaponInstanceComponent instance = store.getComponent(ref, WeaponInstanceComponent.getComponentType());
        if (instance == null) {
            context.sendMessage(Message.raw("§cNo weapon equipped. Use /nexusweapon equip first."));
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("§aEquipped: §f").append(instance.archetypeId)
          .append(" §7(").append(instance.rarity).append(", ").append(instance.weaponTag).append(")\n");
        sb.append("§7Damage multiplier: §f").append(instance.damageMultiplier).append("x\n");

        for (EnchantmentSlot slot : instance.enchantmentSlots) {
            sb.append("§7Slot ").append(slot.slotIndex()).append(": ");
            if (!slot.isUnlocked()) {
                sb.append("§8[locked] §f").append(slot.choiceA())
                  .append(" §7| §f").append(slot.choiceB());
            } else {
                sb.append("§a[active] §f").append(slot.chosen());
            }
            sb.append("\n");
        }

        for (ActiveEnchantmentState state : instance.activeStates) {
            sb.append("§7Runtime state: §f").append(state.enchantmentId())
              .append(" lv").append(state.level())
              .append(state.flowGateActive() ? " §a[GATED ON]" : " §c[GATED OFF]")
              .append("\n");
        }

        context.sendMessage(Message.raw(sb.toString()));
    }

    private void handleActivateSlot(
        CommandContext context,
        Store<EntityStore> store,
        Ref<EntityStore> ref,
        String slotArg
    ) {
        int slotIndex;
        try {
            slotIndex = Integer.parseInt(slotArg);
        } catch (NumberFormatException e) {
            context.sendMessage(Message.raw("§cSlot index must be a number."));
            return;
        }

        WeaponInstanceComponent instance = store.getComponent(
            ref, WeaponInstanceComponent.getComponentType());
        if (instance == null || slotIndex >= instance.enchantmentSlots.size()) {
            context.sendMessage(Message.raw("§cSlot does not exist."));
            return;
        }

        EnchantmentSlot slot = instance.enchantmentSlots.get(slotIndex);
        if (slot.isUnlocked()) {
            context.sendMessage(Message.raw("§cSlot " + slotIndex
                + " already unlocked: §f" + slot.resolveActiveEnchantId()));
            return;
        }

        // Dev bypass — no Essence Dust check, directly pick choiceA
        UpgradeResult result = WeaponUpgradeProvider.get()
                                                    .unlockEnchantmentSlot(ref, slotIndex, slot.choiceA(), store);

        context.sendMessage(result.success()
            ? Message.raw("§aUnlocked slot " + slotIndex
                          + " with §f" + slot.choiceA()
                          + " §7(bypassed cost: " + result.essenceDustSpent() + " dust)")
            : Message.raw("§c" + result.failureReason()));
    }

    private void handleForceEquip(
        CommandContext context,
        Store<EntityStore> store,
        Ref<EntityStore> ref
    ) {
        context.sendMessage(Message.raw(
            "§7Use /nexusweapon give <type> <rarity> to generate a weapon, " +
                "then swap your hotbar slot to trigger WeaponEquipSystem."
        ));
    }

    private WeaponTag parseTag(String s) {
        return switch (s.toLowerCase()) {
            case "melee" -> WeaponTag.MELEE;
            case "ranged" -> WeaponTag.RANGED;
            default -> null;
        };
    }

    private WeaponRarity parseRarity(String s) {
        return switch (s.toLowerCase()) {
            case "common" -> WeaponRarity.COMMON;
            case "rare" -> WeaponRarity.RARE;
            case "epic" -> WeaponRarity.EPIC;
            case "legendary" -> WeaponRarity.LEGENDARY;
            default -> null;
        };
    }
}
