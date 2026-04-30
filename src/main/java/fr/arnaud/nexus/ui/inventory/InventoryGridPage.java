package fr.arnaud.nexus.ui.inventory;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.item.weapon.component.PlayerWeaponStateComponent;
import org.bson.BsonDocument;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class InventoryGridPage {

    static final int STORAGE_CAPACITY = 27;
    static final int HOTBAR_CAPACITY = 9;
    static final int LOCKED_HOTBAR_SLOT = 0;
    private static final int COLS = 9;

    private InventoryGridPage() {
    }

    static void appendSlotBindings(@Nonnull UICommandBuilder cmd,
                                   @Nonnull UIEventBuilder event,
                                   @Nonnull String container,
                                   int capacity, int slotOffset) {
        int rowIndex = 0, col = 0;
        for (int i = 0; i < capacity; i++) {
            if (col == 0) {
                cmd.appendInline(container,
                    "Group { LayoutMode: Left; Anchor: (Bottom: 2); HitTestVisible: false; }");
            }
            cmd.append(container + "[" + rowIndex + "]", "Pages/NexusInventorySlot.ui");
            String slotPath = container + "[" + rowIndex + "][" + col + "]";

            boolean isLockedSlot = (slotOffset == STORAGE_CAPACITY && i == LOCKED_HOTBAR_SLOT);
            if (!isLockedSlot) {
                event.addEventBinding(CustomUIEventBindingType.Activating,
                    slotPath,
                    com.hypixel.hytale.server.core.ui.builder.EventData.of(
                        "SlotClick", "Storage:" + (slotOffset + i)),
                    false);

                event.addEventBinding(CustomUIEventBindingType.RightClicking,
                    slotPath,
                    com.hypixel.hytale.server.core.ui.builder.EventData.of(
                        "Action", "Drop:" + (slotOffset + i)),
                    false);
            }

            col++;
            if (col >= COLS) {
                col = 0;
                rowIndex++;
            }
        }
    }

    static void populateSlotItems(@Nonnull UICommandBuilder cmd,
                                  @Nonnull Ref<EntityStore> ref,
                                  @Nonnull Store<EntityStore> store) {
        ItemContainer storage = getContainer(ref, store, "Storage");
        ItemContainer hotbar = getContainer(ref, store, "Hotbar");
        populateGrid(cmd, "#InventorySlotCards", storage, STORAGE_CAPACITY, false);
        populateGrid(cmd, "#HotbarSlotCards", hotbar, HOTBAR_CAPACITY, true);
    }

    static void populateEquipSlots(@Nonnull UICommandBuilder cmd,
                                   @Nonnull Ref<EntityStore> ref,
                                   @Nonnull Store<EntityStore> store) {
        PlayerWeaponStateComponent state = store.getComponent(
            ref, PlayerWeaponStateComponent.getComponentType());

        if (state == null) {
            cmd.setNull("#MeleeEquippedIcon.ItemId");
            cmd.setNull("#RangedEquippedIcon.ItemId");
            return;
        }

        setEquipSlotIcon(cmd, "#MeleeEquippedIcon", state.meleeDocument);
        setEquipSlotIcon(cmd, "#RangedEquippedIcon", state.rangedDocument);
    }

    private static void setEquipSlotIcon(@Nonnull UICommandBuilder cmd,
                                         @Nonnull String iconSelector,
                                         @Nullable BsonDocument doc) {
        if (doc != null && doc.containsKey("archetype_id")) {
            cmd.set(iconSelector + ".ItemId", doc.getString("archetype_id").getValue());
        } else {
            cmd.setNull(iconSelector + ".ItemId");
        }
    }

    static void swapSlots(@Nonnull Ref<EntityStore> ref,
                          @Nonnull Store<EntityStore> store,
                          @Nonnull String from, @Nonnull String to) {

        int fromGlobal = Integer.parseInt(from.split(":")[1]);
        int toGlobal = Integer.parseInt(to.split(":")[1]);
        int lockedGlobal = STORAGE_CAPACITY + LOCKED_HOTBAR_SLOT;
        if (fromGlobal == lockedGlobal || toGlobal == lockedGlobal) return;

        ItemContainer fromContainer = resolveContainer(ref, store, from);
        ItemContainer toContainer = resolveContainer(ref, store, to);
        if (fromContainer == null || toContainer == null) return;

        short fromSlot = (short) (fromGlobal < STORAGE_CAPACITY
            ? fromGlobal : fromGlobal - STORAGE_CAPACITY);
        short toSlot = (short) (toGlobal < STORAGE_CAPACITY
            ? toGlobal : toGlobal - STORAGE_CAPACITY);

        ItemStack fromStack = fromContainer.getItemStack(fromSlot);
        if (fromStack == null || fromStack.isEmpty()) return;
        fromContainer.swapItems(fromSlot, toContainer, toSlot, (short) 1);
    }

    private static void populateGrid(@Nonnull UICommandBuilder cmd,
                                     @Nonnull String container,
                                     @Nullable ItemContainer source,
                                     int capacity,
                                     boolean isHotbar) {
        int rowIndex = 0, col = 0;
        for (int i = 0; i < capacity; i++) {
            String slotPath = container + "[" + rowIndex + "][" + col + "]";
            boolean isLockedSlot = isHotbar && i == LOCKED_HOTBAR_SLOT;

            if (isLockedSlot) {
                cmd.setNull(slotPath + " #SlotItemIcon.ItemId");
                cmd.set(slotPath + " #SlotAmount.Text", "");
                cmd.set(slotPath + " #SlotNumber.Text", "");
                cmd.set(slotPath + " #SlotToolOverlay.Visible", true);
            } else {
                ItemStack stack = source != null ? source.getItemStack((short) i) : null;
                if (stack != null && !stack.isEmpty()) {
                    cmd.set(slotPath + " #SlotItemIcon.ItemId", stack.getItem().getId());
                    cmd.set(slotPath + " #SlotAmount.Text",
                        stack.getQuantity() > 1 ? String.valueOf(stack.getQuantity()) : "");
                } else {
                    cmd.setNull(slotPath + " #SlotItemIcon.ItemId");
                    cmd.set(slotPath + " #SlotAmount.Text", "");
                }

                if (isHotbar) {
                    cmd.set(slotPath + " #SlotNumber.Text", String.valueOf(i + 1));
                }
            }

            col++;
            if (col >= COLS) {
                col = 0;
                rowIndex++;
            }
        }
    }

    private static ItemContainer resolveContainer(@Nonnull Ref<EntityStore> ref,
                                                  @Nonnull Store<EntityStore> store,
                                                  @Nonnull String slotKey) {
        int slot = Integer.parseInt(slotKey.split(":")[1]);
        return slot < STORAGE_CAPACITY
            ? getContainer(ref, store, "Storage")
            : getContainer(ref, store, "Hotbar");
    }

    @Nullable
    static ItemContainer getContainer(@Nonnull Ref<EntityStore> ref,
                                      @Nonnull Store<EntityStore> store,
                                      @Nonnull String type) {
        return switch (type) {
            case "Storage" -> {
                InventoryComponent.Storage s = store.getComponent(
                    ref, InventoryComponent.Storage.getComponentType());
                yield s != null ? s.getInventory() : null;
            }
            case "Hotbar" -> {
                InventoryComponent.Hotbar h = store.getComponent(
                    ref, InventoryComponent.Hotbar.getComponentType());
                yield h != null ? h.getInventory() : null;
            }
            default -> null;
        };
    }
}
