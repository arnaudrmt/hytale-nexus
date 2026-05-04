package fr.arnaud.nexus.ui.inventory;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemQuality;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.item.weapon.component.PlayerWeaponStateComponent;
import fr.arnaud.nexus.util.QualityMapper;
import org.bson.BsonDocument;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class InventoryGridPage {

    static final int STORAGE_CAPACITY = 27;
    static final int HOTBAR_CAPACITY = 9;
    static final int LOCKED_HOTBAR_SLOT = 0;
    static final int COLS = 9;

    private static final String[] ALL_SLOT_BUTTON_IDS = {
        "#NoItemButton",
        "#SlotCommonButton",
        "#SlotUncommonButton",
        "#SlotRareButton",
        "#SlotEpicButton",
        "#SlotLegendaryButton"
    };

    private InventoryGridPage() {
    }

    static void appendInventorySlotBindings(@Nonnull UICommandBuilder cmd,
                                            @Nonnull UIEventBuilder event,
                                            @Nonnull String container,
                                            int capacity, int slotOffset) {
        int rowIndex = 0, col = 0;
        for (int i = 0; i < capacity; i++) {
            if (col == 0) {
                cmd.appendInline(container,
                    "Group { LayoutMode: Left; Anchor: (Bottom: 2); HitTestVisible: false; }");
            }
            cmd.append(container + "[" + rowIndex + "]", "Pages/InventorySlot.ui");
            String slotPath = container + "[" + rowIndex + "][" + col + "]";

            boolean isLockedSlot = (slotOffset == STORAGE_CAPACITY && i == LOCKED_HOTBAR_SLOT);
            if (!isLockedSlot) {
                for (String buttonId : ALL_SLOT_BUTTON_IDS) {
                    String buttonPath = slotPath + " " + buttonId;

                    event.addEventBinding(CustomUIEventBindingType.Activating,
                        buttonPath,
                        com.hypixel.hytale.server.core.ui.builder.EventData.of(
                            "SlotClick", "Storage:" + (slotOffset + i)),
                        false);

                    event.addEventBinding(CustomUIEventBindingType.RightClicking,
                        buttonPath,
                        com.hypixel.hytale.server.core.ui.builder.EventData.of(
                            "Action", "Drop:" + (slotOffset + i)),
                        false);
                }
            }

            col++;
            if (col >= COLS) {
                col = 0;
                rowIndex++;
            }
        }
    }

    static void populateInventorySlotItems(@Nonnull UICommandBuilder cmd,
                                           @Nonnull Ref<EntityStore> ref,
                                           @Nonnull Store<EntityStore> store) {
        ItemContainer storage = getInventoryContainerByType(ref, store, "Storage");
        ItemContainer hotbar = getInventoryContainerByType(ref, store, "Hotbar");
        populateInventoryGrid(cmd, "#InventorySlotCards", storage, STORAGE_CAPACITY, false);
        populateInventoryGrid(cmd, "#HotbarSlotCards", hotbar, HOTBAR_CAPACITY, true);
    }

    static void populateWeaponEquipSlotIcons(@Nonnull UICommandBuilder cmd,
                                             @Nonnull Ref<EntityStore> ref,
                                             @Nonnull Store<EntityStore> store) {
        PlayerWeaponStateComponent state = store.getComponent(
            ref, PlayerWeaponStateComponent.getComponentType());

        if (state == null) {
            cmd.setNull("#MeleeEquippedIcon.ItemId");
            cmd.setNull("#RangedEquippedIcon.ItemId");
            return;
        }

        applyWeaponDocumentToEquipSlotIcon(cmd, "#MeleeEquippedIcon", state.meleeDocument);
        applyWeaponDocumentToEquipSlotIcon(cmd, "#RangedEquippedIcon", state.rangedDocument);
    }

    private static void applyWeaponDocumentToEquipSlotIcon(@Nonnull UICommandBuilder cmd,
                                                           @Nonnull String iconSelector,
                                                           @Nullable BsonDocument doc) {
        if (doc != null && doc.containsKey("archetype_id")) {
            cmd.set(iconSelector + ".ItemId", doc.getString("archetype_id").getValue());
        } else {
            cmd.setNull(iconSelector + ".ItemId");
        }
    }

    static void swapInventorySlots(@Nonnull Ref<EntityStore> ref,
                                   @Nonnull Store<EntityStore> store,
                                   @Nonnull String from, @Nonnull String to) {
        int fromGlobal = Integer.parseInt(from.split(":")[1]);
        int toGlobal = Integer.parseInt(to.split(":")[1]);
        int lockedGlobal = STORAGE_CAPACITY + LOCKED_HOTBAR_SLOT;
        if (fromGlobal == lockedGlobal || toGlobal == lockedGlobal) return;

        ItemContainer fromContainer = resolveContainerForSlotKey(ref, store, from);
        ItemContainer toContainer = resolveContainerForSlotKey(ref, store, to);
        if (fromContainer == null || toContainer == null) return;

        short fromSlot = (short) (fromGlobal < STORAGE_CAPACITY
            ? fromGlobal : fromGlobal - STORAGE_CAPACITY);
        short toSlot = (short) (toGlobal < STORAGE_CAPACITY
            ? toGlobal : toGlobal - STORAGE_CAPACITY);

        ItemStack fromStack = fromContainer.getItemStack(fromSlot);
        if (fromStack == null || fromStack.isEmpty()) return;
        fromContainer.swapItems(fromSlot, toContainer, toSlot, (short) 1);
    }

    private static void populateInventoryGrid(@Nonnull UICommandBuilder cmd,
                                              @Nonnull String container,
                                              @Nullable ItemContainer source,
                                              int capacity,
                                              boolean isHotbar) {
        int rowIndex = 0, col = 0;
        for (int i = 0; i < capacity; i++) {
            String slotPath = container + "[" + rowIndex + "][" + col + "]";
            boolean isLockedSlot = isHotbar && i == LOCKED_HOTBAR_SLOT;

            if (isLockedSlot) {
                activateQualitySlotButton(cmd, slotPath, null);
                cmd.set(slotPath + " #NoItemButton #SlotToolOverlay.Visible", true);
            } else {
                ItemStack stack = source != null ? source.getItemStack((short) i) : null;
                if (stack != null && !stack.isEmpty()) {
                    String activeButton = activateQualitySlotButton(cmd, slotPath, stack.getItem());
                    cmd.set(slotPath + " " + activeButton + " #SlotItemIcon.ItemId", stack.getItem().getId());
                    cmd.set(slotPath + " #SlotAmount.Text",
                        stack.getQuantity() > 1 ? String.valueOf(stack.getQuantity()) : "");
                } else {
                    activateQualitySlotButton(cmd, slotPath, null);
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

    private static String activateQualitySlotButton(@Nonnull UICommandBuilder cmd,
                                                    @Nonnull String slotPath,
                                                    @Nullable Item item) {
        if (item == null) {
            for (int i = 0; i <= 5; i++) {
                cmd.set(slotPath + " " + QualityMapper.toSlotElementId(i) + ".Visible", false);
            }
            cmd.set(slotPath + " #NoItemButton.Visible", true);
            return "#NoItemButton";
        }

        ItemQuality quality = ItemQuality.getAssetMap().getAsset(item.getQualityIndex());
        int activeQuality = quality.getQualityValue();

        cmd.set(slotPath + " #NoItemButton.Visible", false);
        for (int i = 0; i <= 5; i++) {
            cmd.set(slotPath + " " + QualityMapper.toSlotElementId(i) + ".Visible", i == activeQuality);
        }

        return QualityMapper.toSlotElementId(activeQuality);
    }

    static String resolveSlotPath(int globalIndex) {
        boolean isHotbar = globalIndex >= STORAGE_CAPACITY;
        int localIndex = globalIndex % STORAGE_CAPACITY;
        int row = localIndex / COLS;
        int col = localIndex % COLS;
        String container = isHotbar ? "#HotbarSlotCards" : "#InventorySlotCards";
        return container + "[" + row + "][" + col + "]";
    }

    private static ItemContainer resolveContainerForSlotKey(@Nonnull Ref<EntityStore> ref,
                                                            @Nonnull Store<EntityStore> store,
                                                            @Nonnull String slotKey) {
        int slot = Integer.parseInt(slotKey.split(":")[1]);
        return slot < STORAGE_CAPACITY
            ? getInventoryContainerByType(ref, store, "Storage")
            : getInventoryContainerByType(ref, store, "Hotbar");
    }

    @Nullable
    static ItemContainer getInventoryContainerByType(@Nonnull Ref<EntityStore> ref,
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
