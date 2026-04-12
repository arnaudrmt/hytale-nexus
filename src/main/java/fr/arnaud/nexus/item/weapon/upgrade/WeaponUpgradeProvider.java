package fr.arnaud.nexus.item.weapon.upgrade;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.item.weapon.component.PlayerWeaponStateComponent;
import fr.arnaud.nexus.item.weapon.data.EnchantmentSlot;
import fr.arnaud.nexus.item.weapon.data.WeaponBsonSchema;
import fr.arnaud.nexus.item.weapon.data.WeaponRarity;
import fr.arnaud.nexus.item.weapon.data.WeaponTag;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentDefinition;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentRegistry;
import fr.arnaud.nexus.item.weapon.generator.WeaponRarityTableLoader;
import org.bson.BsonDocument;

import java.util.List;

public final class WeaponUpgradeProvider {

    private static final WeaponUpgradeProvider INSTANCE = new WeaponUpgradeProvider();

    private WeaponUpgradeProvider() {
    }

    public static WeaponUpgradeProvider get() {
        return INSTANCE;
    }

    /**
     * Unlocks a slot on the player's active weapon, choosing one of the two
     * rolled options. Charges the level-1 base cost of the chosen enchantment.
     */
    public UpgradeResult unlockEnchantmentSlot(
        Ref<EntityStore> playerRef,
        int slotIndex,
        String chosenBaseFamily,
        Store<EntityStore> store
    ) {
        PlayerWeaponStateComponent state = getState(playerRef, store);
        if (state == null) return UpgradeResult.fail("No weapon state found.");

        BsonDocument activeDoc = state.getActiveDocument();
        if (activeDoc == null) return UpgradeResult.fail("No active weapon.");

        List<EnchantmentSlot> slots = WeaponBsonSchema.readEnchantmentSlots(activeDoc);
        if (slotIndex < 0 || slotIndex >= slots.size())
            return UpgradeResult.fail("Slot " + slotIndex + " does not exist.");

        EnchantmentSlot slot = slots.get(slotIndex);
        if (slot.isUnlocked())
            return UpgradeResult.fail("Slot " + slotIndex + " is already unlocked.");

        if (!chosenBaseFamily.equals(slot.choiceA())
            && !chosenBaseFamily.equals(slot.choiceB())) {
            return UpgradeResult.fail(chosenBaseFamily
                + " is not a valid choice for slot " + slotIndex + ".");
        }

        String level1Id = chosenBaseFamily + "_1";
        EnchantmentDefinition def = EnchantmentRegistry.get().getDefinition(level1Id);
        if (def == null) return UpgradeResult.fail("Unknown enchantment: " + level1Id);

        int cost = def.computeCost(1);
        UpgradeResult dustCheck = chargeEssenceDust(playerRef, store, cost);
        if (!dustCheck.success()) return dustCheck;

        EnchantmentSlot updated = slot.withChoice(chosenBaseFamily);
        slots.set(slotIndex, updated);
        WeaponBsonSchema.writeEnchantmentSlots(activeDoc, slots);
        persistAndReequip(playerRef, state, activeDoc, store);

        return UpgradeResult.ok(cost);
    }

    /**
     * Upgrades an already-unlocked enchantment on the active weapon by one level.
     * Charges cost = baseCost * curve^(currentLevel).
     */
    public UpgradeResult upgradeEnchantmentLevel(
        Ref<EntityStore> playerRef,
        int slotIndex,
        Store<EntityStore> store
    ) {
        PlayerWeaponStateComponent state = getState(playerRef, store);
        if (state == null) return UpgradeResult.fail("No weapon state found.");

        BsonDocument activeDoc = state.getActiveDocument();
        if (activeDoc == null) return UpgradeResult.fail("No active weapon.");

        List<EnchantmentSlot> slots = WeaponBsonSchema.readEnchantmentSlots(activeDoc);
        if (slotIndex < 0 || slotIndex >= slots.size())
            return UpgradeResult.fail("Slot " + slotIndex + " does not exist.");

        EnchantmentSlot slot = slots.get(slotIndex);
        if (!slot.isUnlocked())
            return UpgradeResult.fail("Slot " + slotIndex + " is not unlocked.");

        if (slot.isMaxLevel())
            return UpgradeResult.fail("Slot " + slotIndex
                + " is already at max level (" + slot.getMaxLevel() + ").");

        int nextLevel = slot.currentLevel() + 1;
        String nextId = slot.chosenBase() + "_" + nextLevel;
        EnchantmentDefinition def = EnchantmentRegistry.get()
                                                       .getDefinition(slot.chosenBase() + "_1");
        if (def == null) return UpgradeResult.fail(
            "No definition found for " + slot.chosenBase());

        int cost = def.computeCost(nextLevel);
        UpgradeResult dustCheck = chargeEssenceDust(playerRef, store, cost);
        if (!dustCheck.success()) return dustCheck;

        EnchantmentSlot upgraded = slot.withUpgradedLevel();
        slots.set(slotIndex, upgraded);
        WeaponBsonSchema.writeEnchantmentSlots(activeDoc, slots);
        persistAndReequip(playerRef, state, activeDoc, store);

        return UpgradeResult.ok(cost);
    }

    /**
     * Upgrades the active weapon's rarity by one tier.
     * Charges the upgrade cost defined in WeaponRarityTable for the current rarity.
     */
    public UpgradeResult upgradeWeaponRarity(
        Ref<EntityStore> playerRef,
        Store<EntityStore> store
    ) {
        PlayerWeaponStateComponent state = getState(playerRef, store);
        if (state == null) return UpgradeResult.fail("No weapon state found.");

        BsonDocument activeDoc = state.getActiveDocument();
        if (activeDoc == null) return UpgradeResult.fail("No active weapon.");

        WeaponRarity current = WeaponBsonSchema.readRarity(activeDoc);
        WeaponRarity next = nextRarity(current);
        if (next == null) return UpgradeResult.fail(
            "Weapon is already at maximum rarity.");

        int cost = WeaponRarityTableLoader.getUpgradeCost(current);
        if (cost <= 0) return UpgradeResult.fail(
            "No upgrade cost defined for " + current.name() + ".");

        UpgradeResult dustCheck = chargeEssenceDust(playerRef, store, cost);
        if (!dustCheck.success()) return dustCheck;

        float newMultiplier = WeaponRarityTableLoader.getDamageMultiplier(next);
        int newSlots = WeaponRarityTableLoader.getEnchantmentSlots(next);

        WeaponBsonSchema.writeRarity(activeDoc, next);

        List<EnchantmentSlot> slots = WeaponBsonSchema.readEnchantmentSlots(activeDoc);
        WeaponTag tag = WeaponBsonSchema.readWeaponTag(activeDoc);
        slots = expandSlotsTo(slots, newSlots, tag);
        WeaponBsonSchema.writeEnchantmentSlots(activeDoc, slots);

        persistAndReequip(playerRef, state, activeDoc, store);
        return UpgradeResult.ok(cost);
    }

    // --- Internals ---

    private UpgradeResult chargeEssenceDust(
        Ref<EntityStore> playerRef,
        Store<EntityStore> store,
        int amount
    ) {
        float balance = Nexus.get().getEssenceDustManager()
                             .getBalance(playerRef, store);
        if (balance < amount) {
            return UpgradeResult.fail("Not enough Essence Dust. Need "
                + amount + ", have " + (int) balance + ".");
        }
        Nexus.get().getEssenceDustManager()
             .removeEssenceDust(playerRef, store, amount);
        return UpgradeResult.ok(amount);
    }

    private void persistAndReequip(
        Ref<EntityStore> playerRef,
        PlayerWeaponStateComponent state,
        BsonDocument updatedDoc,
        Store<EntityStore> store
    ) {
        state.setDocument(state.activeTag, updatedDoc);
        store.getExternalData().getWorld().execute(() ->
            store.putComponent(playerRef,
                PlayerWeaponStateComponent.getComponentType(), state)
        );

        WeaponTag tag = WeaponBsonSchema.readWeaponTag(updatedDoc);
        String archetypeId = tag == WeaponTag.MELEE
            ? "Nexus_Weapon_Sword_Default"
            : "Nexus_Weapon_Staff_Default";
        ItemStack newStack = new ItemStack(archetypeId, 1, updatedDoc);

        InventoryComponent.Hotbar hotbar = store.getComponent(
            playerRef, InventoryComponent.Hotbar.getComponentType());
        if (hotbar != null) {
            hotbar.getInventory().setItemStackForSlot((short) 0, newStack);
            hotbar.markDirty();
        }

        Nexus.get().getWeaponEquipSystem()
             .onWeaponEquipped(playerRef, newStack, store);
    }

    private List<EnchantmentSlot> expandSlotsTo(
        List<EnchantmentSlot> existing,
        int targetCount,
        WeaponTag tag
    ) {
        java.util.List<EnchantmentSlot> result = new java.util.ArrayList<>(existing);
        while (result.size() < targetCount) {
            int slotIndex = result.size();
            List<EnchantmentDefinition> pool = Nexus.get()
                                                    .getEnchantmentPoolService().getPoolForTag(tag, 1);
            if (pool.size() < 2) break;
            java.util.Random rng = new java.util.Random();
            EnchantmentDefinition a = pool.get(rng.nextInt(pool.size()));
            EnchantmentDefinition b;
            do {
                b = pool.get(rng.nextInt(pool.size()));
            } while (b.getBaseFamily().equals(a.getBaseFamily()));
            result.add(new EnchantmentSlot(slotIndex,
                a.getBaseFamily(), b.getBaseFamily(), null, null, 0));
        }
        return result;
    }

    private PlayerWeaponStateComponent getState(
        Ref<EntityStore> playerRef, Store<EntityStore> store) {
        return store.getComponent(playerRef,
            PlayerWeaponStateComponent.getComponentType());
    }

    private WeaponRarity nextRarity(WeaponRarity current) {
        return switch (current) {
            case COMMON -> WeaponRarity.RARE;
            case RARE -> WeaponRarity.EPIC;
            case EPIC -> WeaponRarity.LEGENDARY;
            case LEGENDARY -> null;
        };
    }
}
