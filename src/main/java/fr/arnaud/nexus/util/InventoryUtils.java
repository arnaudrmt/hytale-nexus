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
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class InventoryUtils {

    public static final int MAX_STORAGE_SLOTS = 27;

    private InventoryUtils() {
    }

    public static void tryAddToStorage(@Nonnull Ref<EntityStore> ref,
                                       @Nonnull Store<EntityStore> store,
                                       @Nonnull ItemStack stack) {
        ItemContainer storage = getStorage(ref, store);
        if (storage == null) {
            spawnItemDrop(ref, store, stack);
            return;
        }

        if (getStorageUsedSlots(ref, store) >= MAX_STORAGE_SLOTS) {
            spawnItemDrop(ref, store, stack);
            return;
        }

        ItemStackTransaction transaction = storage.addItemStack(stack);
        ItemStack remainder = transaction.getRemainder();
        if (!ItemStack.isEmpty(remainder)) {
            spawnItemDrop(ref, store, remainder);
        }
    }

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

    public static void spawnItemDrop(@Nonnull Ref<EntityStore> ref,
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

    public static void dropItemFromInventory(@Nonnull Ref<EntityStore> ref,
                                             @Nonnull Store<EntityStore> store,
                                             @Nonnull ItemContainer container,
                                             short slotIndex) {
        ItemStack stack = container.getItemStack(slotIndex);
        if (stack == null || stack.isEmpty()) return;

        container.removeItemStackFromSlot(slotIndex, stack.getQuantity());

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        Vector3d base = transform != null ? transform.getPosition() : new Vector3d(0, 0, 0);

        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        float yaw = playerRef != null ? playerRef.getHeadRotation().getX() : 0f;

        double throwDistance = 2.5;
        Vector3d position = new Vector3d(
            base.getX() - Math.sin(yaw) * throwDistance,
            base.getY() + 0.5,
            base.getZ() + Math.cos(yaw) * throwDistance
        );

        Holder<EntityStore> itemHolder = ItemComponent.generateItemDrop(
            store, stack, position, Vector3f.ZERO, 1.5f, 0.2f, 0f);
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
