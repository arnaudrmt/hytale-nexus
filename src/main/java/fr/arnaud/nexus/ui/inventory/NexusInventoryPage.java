package fr.arnaud.nexus.ui.inventory;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.item.weapon.component.PlayerWeaponStateComponent;
import fr.arnaud.nexus.item.weapon.data.WeaponBsonSchema;
import fr.arnaud.nexus.item.weapon.data.WeaponTag;
import fr.arnaud.nexus.util.InventoryUtils;
import org.bson.BsonDocument;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;

public class NexusInventoryPage extends InteractiveCustomUIPage<NexusInventoryPage.EventData> {

    private volatile String pendingInventorySlotKey = null;
    private volatile String hoveredSlotPath = null;
    private volatile WeaponTag activeWeaponTab;

    private volatile ScheduledFuture<?> pendingCoreWheelCloseTask = null;
    private static final ScheduledExecutorService CORE_WHEEL_CLOSE_SCHEDULER = Executors.newSingleThreadScheduledExecutor();

    public NexusInventoryPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, EventData.CODEC);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder event, @Nonnull Store<EntityStore> store) {

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerWeaponStateComponent weaponState = store.getComponent(
            ref, PlayerWeaponStateComponent.getComponentType());

        activeWeaponTab = weaponState != null ? weaponState.activeTag : WeaponTag.MELEE;

        cmd.append("Pages/Inventory.ui");
        cmd.append("#CharacterPanel", "Pages/CharacterPanel.ui");
        cmd.append("#EnchantmentPanel", "Pages/EnchantmentPanel.ui");
        cmd.append("#InventoryPanel", "Pages/InventoryPanel.ui");
        cmd.append("#WeaponPanel", "Pages/WeaponPanel.ui");
        cmd.set("#InventorySlotSection.Visible", true);

        if (player != null) {
            cmd.set("#PlayerName.Text", player.getDisplayName());
        }

        // Tab buttons
        event.addEventBinding(CustomUIEventBindingType.Activating, "#MeleeDisabledButton",
            com.hypixel.hytale.server.core.ui.builder.EventData.of("TabClick", "Melee"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#RangedDisabledButton",
            com.hypixel.hytale.server.core.ui.builder.EventData.of("TabClick", "Ranged"), false);

        // Equip slot buttons
        event.addEventBinding(CustomUIEventBindingType.Activating, "#MeleeEquipButton",
            com.hypixel.hytale.server.core.ui.builder.EventData.of("EquipSlotClick", "Melee"), false);
        event.addEventBinding(CustomUIEventBindingType.Activating, "#RangedEquipButton",
            com.hypixel.hytale.server.core.ui.builder.EventData.of("EquipSlotClick", "Ranged"), false);

        event.addEventBinding(CustomUIEventBindingType.Activating, "#UpgradeButton",
            com.hypixel.hytale.server.core.ui.builder.EventData.of("WeaponUpgrade", ""), false);

        // Inventory grid bindings
        InventoryGridPage.appendInventorySlotBindings(cmd, event, "#InventorySlotCards",
            InventoryGridPage.STORAGE_CAPACITY, 0);
        InventoryGridPage.appendInventorySlotBindings(cmd, event, "#HotbarSlotCards",
            InventoryGridPage.HOTBAR_CAPACITY, InventoryGridPage.STORAGE_CAPACITY);

        // Enchantment slot bindings (also registers choose/upgrade event bindings)
        EnchantmentGridPage.appendEnchantmentSlotBindings(cmd, event);

        // Core wheel bindings
        CoreWheelPage.appendCoreWheelEventBindings(event);

        applyWeaponTabVisuals(cmd, activeWeaponTab);
        EnchantmentGridPage.populateEnchantmentSlots(cmd, ref, store, activeWeaponTab);
        InventoryGridPage.populateInventorySlotItems(cmd, ref, store);
        InventoryGridPage.populateWeaponEquipSlotIcons(cmd, ref, store);
        CharacterStatsPage.populate(cmd, ref, store);
        WeaponStatsPage.populateWeaponStats(cmd, ref, store, activeWeaponTab);
        CoreWheelPage.populateCoreWheelSlots(cmd, ref, store);
        cmd.set("#CoreWheelContent.Visible", false);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull EventData data) {
        World world = store.getExternalData().getWorld();

        if (data.weaponTabClick != null) {
            activeWeaponTab = data.weaponTabClick.equals("Ranged") ? WeaponTag.RANGED : WeaponTag.MELEE;
            pendingInventorySlotKey = null;
            world.execute(() -> {
                UICommandBuilder update = new UICommandBuilder();
                applyWeaponTabVisuals(update, activeWeaponTab);
                EnchantmentGridPage.populateEnchantmentSlots(update, ref, ref.getStore(), activeWeaponTab);
                WeaponStatsPage.populateWeaponStats(update, ref, ref.getStore(), activeWeaponTab);
                sendUpdate(update, null, false);
            });
            return;
        }

        if (data.inventoryAction != null && data.inventoryAction.startsWith("Drop:")) {
            int globalIndex = Integer.parseInt(data.inventoryAction.split(":")[1]);
            boolean isHotbar = globalIndex >= InventoryGridPage.STORAGE_CAPACITY;
            short slotIndex = (short) (isHotbar
                ? globalIndex - InventoryGridPage.STORAGE_CAPACITY
                : globalIndex);

            ItemContainer container = InventoryGridPage.getInventoryContainerByType(ref, store,
                isHotbar ? "Hotbar" : "Storage");
            if (container == null) return;

            world.execute(() -> {
                InventoryUtils.dropItemFromInventory(ref, ref.getStore(), container, slotIndex);
                UICommandBuilder update = new UICommandBuilder();
                InventoryGridPage.populateInventorySlotItems(update, ref, ref.getStore());
                sendUpdate(update, null, false);
            });
            return;
        }

        if (data.coreWheelHoverDirection != null) {
            if ("Enter".equals(data.coreWheelHoverDirection)) {
                if (pendingCoreWheelCloseTask != null) {
                    pendingCoreWheelCloseTask.cancel(false);
                    pendingCoreWheelCloseTask = null;
                }
                UICommandBuilder update = new UICommandBuilder();
                CoreWheelPage.applyCoreWheelHoverVisibility(update, "Enter");
                sendUpdate(update, null, false);
            } else {
                pendingCoreWheelCloseTask = CORE_WHEEL_CLOSE_SCHEDULER.schedule(() -> {
                    UICommandBuilder update = new UICommandBuilder();
                    CoreWheelPage.applyCoreWheelHoverVisibility(update, "Leave");
                    sendUpdate(update, null, false);
                }, 200, java.util.concurrent.TimeUnit.MILLISECONDS);
            }
            return;
        }

        if (data.coreAbilitySelection != null) {
            String captured = data.coreAbilitySelection;
            world.execute(() -> {
                boolean changed = CoreWheelPage.selectCoreAbility(ref, ref.getStore(), captured);
                if (changed) {
                    UICommandBuilder update = new UICommandBuilder();
                    CoreWheelPage.populateCoreWheelSlots(update, ref, ref.getStore());
                    sendUpdate(update, null, false);
                }
            });
            return;
        }

        if (data.enchantmentChoiceSelection != null) {
            String capturedPayload = data.enchantmentChoiceSelection;
            world.execute(() -> {
                boolean changed = EnchantmentGridPage.commitEnchantmentChoice(
                    ref, ref.getStore(), capturedPayload, activeWeaponTab);
                if (changed) {
                    UICommandBuilder update = new UICommandBuilder();
                    EnchantmentGridPage.populateEnchantmentSlots(update, ref, ref.getStore(), activeWeaponTab);
                    sendUpdate(update, null, false);
                    refreshAllStatsDisplays(ref);
                }
            });
            return;
        }

        if (data.enchantmentUpgradeRequest != null) {
            String capturedPayload = data.enchantmentUpgradeRequest;
            world.execute(() -> {
                boolean changed = EnchantmentGridPage.upgradeEnchantment(
                    ref, ref.getStore(), capturedPayload, activeWeaponTab);
                if (changed) {
                    UICommandBuilder update = new UICommandBuilder();
                    EnchantmentGridPage.populateEnchantmentSlots(update, ref, ref.getStore(), activeWeaponTab);
                    sendUpdate(update, null, false);
                    refreshAllStatsDisplays(ref);
                }
            });
            return;
        }

        if (data.weaponEquipSlotClick != null) {

            WeaponTag targetTag = data.weaponEquipSlotClick.equals("Ranged")
                ? WeaponTag.RANGED : WeaponTag.MELEE;

            if (!isSelectedSlotEquippableToWeaponTag(ref, store, pendingInventorySlotKey, targetTag)) {
                pendingInventorySlotKey = null;
                return;
            }

            String capturedFrom = pendingInventorySlotKey;
            pendingInventorySlotKey = null;

            world.execute(() -> {
                try {
                    equipWeaponFromInventorySlot(ref, ref.getStore(), capturedFrom, targetTag);
                } catch (Exception e) {
                    HytaleLogger.getLogger().at(Level.WARNING).log("Error handling equipment slot", e);
                }
                UICommandBuilder update = new UICommandBuilder();
                InventoryGridPage.populateInventorySlotItems(update, ref, ref.getStore());
                InventoryGridPage.populateWeaponEquipSlotIcons(update, ref, ref.getStore());
                EnchantmentGridPage.populateEnchantmentSlots(update, ref, ref.getStore(), activeWeaponTab);
                WeaponStatsPage.populateWeaponStats(update, ref, ref.getStore(), activeWeaponTab);
                sendUpdate(update, null, false);
                refreshAllStatsDisplays(ref);
            });
            return;
        }

        if (data.weaponUpgradeRequest != null) {
            world.execute(() -> {
                PlayerWeaponStateComponent state = ref.getStore().getComponent(
                    ref, PlayerWeaponStateComponent.getComponentType());
                if (state == null) return;
                BsonDocument doc = activeWeaponTab == WeaponTag.RANGED
                    ? state.rangedDocument : state.meleeDocument;
                if (doc == null) return;
                Nexus.getInstance().getWeaponUpgradeService().attemptUpgrade(ref, doc, ref.getStore());
                UICommandBuilder update = new UICommandBuilder();
                WeaponStatsPage.populateWeaponStats(update, ref, ref.getStore(), activeWeaponTab);
                sendUpdate(update, null, false);
                refreshAllStatsDisplays(ref);
            });
            return;
        }

        if (data.slotHover != null) {
            String incomingSlotPath = InventoryGridPage.resolveSlotPath(Integer.parseInt(data.slotHover));

            UICommandBuilder update = new UICommandBuilder();
            if (hoveredSlotPath != null) {
                update.set(hoveredSlotPath + " #SlotHoverOverlay.Visible", false);
            }
            hoveredSlotPath = incomingSlotPath;
            update.set(incomingSlotPath + " #SlotHoverOverlay.Visible", true);
            sendUpdate(update, null, false);
            return;
        }

        if (data.inventorySlotClick == null) return;

        int clickedGlobal = Integer.parseInt(data.inventorySlotClick.split(":")[1]);
        int lockedGlobal = InventoryGridPage.STORAGE_CAPACITY + InventoryGridPage.LOCKED_HOTBAR_SLOT;

        if (clickedGlobal == lockedGlobal) {
            pendingInventorySlotKey = null;
            return;
        }

        if (pendingInventorySlotKey == null) {
            if (!inventorySlotHasItem(ref, store, data.inventorySlotClick)) return;
            pendingInventorySlotKey = data.inventorySlotClick;
            return;
        }

        String capturedFrom = pendingInventorySlotKey;
        String capturedTo = data.inventorySlotClick;
        pendingInventorySlotKey = null;

        world.execute(() -> {
            try {
                if (!capturedFrom.equals(capturedTo)) {
                    InventoryGridPage.swapInventorySlots(ref, ref.getStore(), capturedFrom, capturedTo);
                }
            } catch (Exception e) {
                HytaleLogger.getLogger().at(Level.WARNING).log("Could not swap equipment slot item", e);
            }
            UICommandBuilder update = new UICommandBuilder();
            InventoryGridPage.populateInventorySlotItems(update, ref, ref.getStore());
            sendUpdate(update, null, false);
        });
    }

    private boolean inventorySlotHasItem(@Nonnull Ref<EntityStore> ref,
                                         @Nonnull Store<EntityStore> store,
                                         @Nonnull String slotKey) {
        int globalIndex = Integer.parseInt(slotKey.split(":")[1]);
        boolean isHotbar = globalIndex >= InventoryGridPage.STORAGE_CAPACITY;
        short slotIndex = (short) (isHotbar
            ? globalIndex - InventoryGridPage.STORAGE_CAPACITY
            : globalIndex);
        ItemContainer container = InventoryGridPage.getInventoryContainerByType(ref, store,
            isHotbar ? "Hotbar" : "Storage");
        if (container == null) return false;
        ItemStack stack = container.getItemStack(slotIndex);
        return stack != null && !stack.isEmpty();
    }

    private void equipWeaponFromInventorySlot(@Nonnull Ref<EntityStore> ref,
                                              @Nonnull Store<EntityStore> store,
                                              @Nonnull String fromSlotKey,
                                              @Nonnull WeaponTag targetTag) {

        int globalIndex = Integer.parseInt(fromSlotKey.split(":")[1]);
        boolean isHotbar = globalIndex >= InventoryGridPage.STORAGE_CAPACITY;
        short slotIndex = (short) (isHotbar
            ? globalIndex - InventoryGridPage.STORAGE_CAPACITY
            : globalIndex);

        ItemContainer container = InventoryGridPage.getInventoryContainerByType(ref, store,
            isHotbar ? "Hotbar" : "Storage");
        if (container == null) return;

        ItemStack incomingStack = container.getItemStack(slotIndex);
        if (incomingStack == null || incomingStack.isEmpty()) return;

        BsonDocument incomingDoc = incomingStack.getMetadata();
        if (incomingDoc == null || !incomingDoc.containsKey("nexus_quality_value")) return;

        WeaponTag itemTag = WeaponBsonSchema.readWeaponTag(incomingDoc);
        if (!itemTag.isCompatibleWith(targetTag)) return;

        PlayerWeaponStateComponent weaponState = store.getComponent(
            ref, PlayerWeaponStateComponent.getComponentType());
        if (weaponState == null) return;

        BsonDocument oldDoc = targetTag == WeaponTag.MELEE
            ? weaponState.meleeDocument
            : weaponState.rangedDocument;

        if (oldDoc != null && oldDoc.containsKey("archetype_id")) {
            ItemStack oldStack = new ItemStack(
                oldDoc.getString("archetype_id").getValue(), 1, oldDoc.clone());
            InventoryUtils.tryAddToStorage(ref, store, oldStack);
        }

        container.removeItemStackFromSlot(slotIndex, (short) 1);
        weaponState.setDocument(targetTag, incomingDoc.clone());

        if (targetTag == weaponState.activeTag) {
            String archetypeId = incomingDoc.containsKey("archetype_id")
                ? incomingDoc.getString("archetype_id").getValue() : "Nexus_Default_Melee_Sword";
            InventoryComponent.Hotbar hotbar = store.getComponent(
                ref, InventoryComponent.Hotbar.getComponentType());
            if (hotbar != null) {
                hotbar.getInventory().setItemStackForSlot(
                    (short) 0, new ItemStack(archetypeId, 1, incomingDoc.clone()));
                hotbar.markDirty();
            }
            weaponState.activeTag = targetTag;
            Nexus.getInstance().getWeaponEquipSystem().onWeaponEquipped(ref, incomingStack, store);
        }
    }

    private boolean isSelectedSlotEquippableToWeaponTag(@Nonnull Ref<EntityStore> ref,
                                                        @Nonnull Store<EntityStore> store,
                                                        @Nullable String pendingSlot,
                                                        @Nonnull WeaponTag targetTag) {
        if (pendingSlot == null) return false;

        int globalIndex = Integer.parseInt(pendingSlot.split(":")[1]);
        boolean isHotbar = globalIndex >= InventoryGridPage.STORAGE_CAPACITY;
        short slotIndex = (short) (isHotbar
            ? globalIndex - InventoryGridPage.STORAGE_CAPACITY
            : globalIndex);

        ItemContainer container = InventoryGridPage.getInventoryContainerByType(ref, store,
            isHotbar ? "Hotbar" : "Storage");
        if (container == null) return false;

        ItemStack stack = container.getItemStack(slotIndex);
        if (stack == null || stack.isEmpty()) return false;

        BsonDocument doc = stack.getMetadata();
        if (doc == null || !doc.containsKey("nexus_quality_value")) return false;

        WeaponTag itemTag = WeaponBsonSchema.readWeaponTag(doc);
        return itemTag.isCompatibleWith(targetTag);
    }

    private void applyWeaponTabVisuals(@Nonnull UICommandBuilder cmd, @Nonnull WeaponTag tab) {
        boolean isMelee = tab == WeaponTag.MELEE;

        cmd.set("#UpgradeTitleLabel.Text",
            isMelee ? Message.translation("inventory.enchantment.title.melee") :
                Message.translation("inventory.enchantment.title.ranged"));

        cmd.set("#MeleeActive.Visible", isMelee);
        cmd.set("#MeleeDisabledButton.Visible", !isMelee);
        cmd.set("#MeleeButtonBgDefault.Visible", !isMelee);
        cmd.set("#MeleeButtonBgActive.Visible", isMelee);

        cmd.set("#RangedActive.Visible", !isMelee);
        cmd.set("#RangedDisabledButton.Visible", isMelee);
        cmd.set("#RangedButtonBgDefault.Visible", isMelee);
        cmd.set("#RangedButtonBgActive.Visible", !isMelee);
    }

    public static class EventData {
        public static final BuilderCodec<EventData> CODEC = BuilderCodec
            .builder(EventData.class, EventData::new)
            .append(new KeyedCodec<>("SlotClick", Codec.STRING),
                (d, v) -> d.inventorySlotClick = v, d -> d.inventorySlotClick)
            .add()
            .append(new KeyedCodec<>("TabClick", Codec.STRING),
                (d, v) -> d.weaponTabClick = v, d -> d.weaponTabClick)
            .add()
            .append(new KeyedCodec<>("Action", Codec.STRING),
                (d, v) -> d.inventoryAction = v, d -> d.inventoryAction)
            .add()
            .append(new KeyedCodec<>("EquipSlotClick", Codec.STRING),
                (d, v) -> d.weaponEquipSlotClick = v, d -> d.weaponEquipSlotClick)
            .add()
            .append(new KeyedCodec<>("EnchantChoose", Codec.STRING),
                (d, v) -> d.enchantmentChoiceSelection = v, d -> d.enchantmentChoiceSelection)
            .add()
            .append(new KeyedCodec<>("EnchantUpgrade", Codec.STRING),
                (d, v) -> d.enchantmentUpgradeRequest = v, d -> d.enchantmentUpgradeRequest)
            .add()
            .append(new KeyedCodec<>("WeaponUpgrade", Codec.STRING),
                (d, v) -> d.weaponUpgradeRequest = v, d -> d.weaponUpgradeRequest)
            .add()
            .append(new KeyedCodec<>("CoreWheelHover", Codec.STRING),
                (d, v) -> d.coreWheelHoverDirection = v, d -> d.coreWheelHoverDirection)
            .add()
            .append(new KeyedCodec<>("CoreSelect", Codec.STRING),
                (d, v) -> d.coreAbilitySelection = v, d -> d.coreAbilitySelection)
            .add()
            .build();

        public String inventorySlotClick;
        public String weaponTabClick;
        public String inventoryAction;
        public String weaponEquipSlotClick;
        public String enchantmentChoiceSelection;
        public String enchantmentUpgradeRequest;
        public String weaponUpgradeRequest;
        public String coreWheelHoverDirection;
        public String coreAbilitySelection;
        public String slotHover;

        public EventData() {
        }
    }

    private void refreshAllStatsDisplays(@Nonnull Ref<EntityStore> ref) {
        reapplyActiveWeaponEquipEffects(ref, ref.getStore());
        ref.getStore().getExternalData().getWorld().execute(() -> {
            if (!ref.isValid()) return;
            UICommandBuilder update = new UICommandBuilder();
            WeaponStatsPage.populateWeaponStats(update, ref, ref.getStore(), activeWeaponTab);
            EnchantmentGridPage.populateEnchantmentSlots(update, ref, ref.getStore(), activeWeaponTab);
            CharacterStatsPage.populate(update, ref, ref.getStore());
            sendUpdate(update, null, false);
        });
    }

    private void reapplyActiveWeaponEquipEffects(@Nonnull Ref<EntityStore> ref,
                                                 @Nonnull Store<EntityStore> store) {
        PlayerWeaponStateComponent state = store.getComponent(
            ref, PlayerWeaponStateComponent.getComponentType());
        if (state == null) return;

        BsonDocument doc = activeWeaponTab == WeaponTag.RANGED
            ? state.rangedDocument : state.meleeDocument;
        if (doc == null || !doc.containsKey("archetype_id")) return;

        String archetypeId = doc.getString("archetype_id").getValue();
        Nexus.getInstance().getWeaponEquipSystem().onWeaponEquipped(
            ref,
            new ItemStack(archetypeId, 1, doc),
            store);
    }
}
