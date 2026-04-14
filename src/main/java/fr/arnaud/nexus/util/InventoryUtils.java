package fr.arnaud.nexus.util;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class InventoryUtils {

    public static final int MAX_STORAGE_SLOTS = 27;

    private InventoryUtils() {
    }

    /**
     * Tries to add an item to the player's storage (slots 0–26 only).
     * If all 27 slots are full, drops the item at the player's position instead.
     * Returns true if the item was added, false if it was dropped.
     */
    public static boolean tryAddToStorage(@Nonnull Ref<EntityStore> ref,
                                          @Nonnull Store<EntityStore> store,
                                          @Nonnull ItemStack stack) {
        ItemContainer storage = getStorage(ref, store);
        if (storage == null) {
            dropItem(ref, store, stack);
            return false;
        }

        // Only consider the first 27 slots
        if (getStorageUsedSlots(ref, store) >= MAX_STORAGE_SLOTS) {
            dropItem(ref, store, stack);
            return false;
        }

        ItemStackTransaction transaction = storage.addItemStack(stack);
        ItemStack remainder = transaction.getRemainder();
        if (!ItemStack.isEmpty(remainder)) {
            dropItem(ref, store, remainder);
            return false;
        }
        return true;
    }

    /**
     * Counts how many of the first 27 storage slots are occupied.
     */
    public static int getStorageUsedSlots(@Nonnull Ref<EntityStore> ref,
                                          @Nonnull Store<EntityStore> store) {
        ItemContainer storage = getStorage(ref, store);
        if (storage == null) return MAX_STORAGE_SLOTS;

        int used = 0;
        for (short i = 0; i < MAX_STORAGE_SLOTS; i++) {
            ItemStack stack = storage.getItemStack(i);
            if (stack != null && !stack.isEmpty()) used++;
        }
        return used;
    }

    /**
     * Drops an item at the player's current world position.
     */
    public static void dropItem(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull ItemStack stack) {
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        Vector3d position = transform != null
            ? transform.getPosition()
            : new Vector3d(0, 0, 0);

        Holder<EntityStore> itemHolder = ItemComponent.generateItemDrop(
            store, stack, position, Vector3f.ZERO, 0f, 0.2f, 0f);
        if (itemHolder != null) {
            store.addEntity(itemHolder, AddReason.SPAWN);
        }
    }

    @Nullable
    private static ItemContainer getStorage(@Nonnull Ref<EntityStore> ref,
                                            @Nonnull Store<EntityStore> store) {
        InventoryComponent.Storage s = store.getComponent(
            ref, InventoryComponent.Storage.getComponentType());
        return s != null ? s.getInventory() : null;
    }
}
