package fr.arnaud.nexus.ui.inventory;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
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

public class NexusInventoryPage extends InteractiveCustomUIPage<NexusInventoryPage.EventData> {

    private volatile String selectedSlot = null;
    private volatile WeaponTag activeTab;

    public NexusInventoryPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, EventData.CODEC);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder event, @Nonnull Store<EntityStore> store) {

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerWeaponStateComponent weaponState = store.getComponent(
            ref, PlayerWeaponStateComponent.getComponentType());

        activeTab = weaponState != null ? weaponState.activeTag : WeaponTag.MELEE;

        cmd.append("Pages/NexusInventory.ui");
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
        InventoryGridPage.appendSlotBindings(cmd, event, "#InventorySlotCards",
            InventoryGridPage.STORAGE_CAPACITY, 0);
        InventoryGridPage.appendSlotBindings(cmd, event, "#HotbarSlotCards",
            InventoryGridPage.HOTBAR_CAPACITY, InventoryGridPage.STORAGE_CAPACITY);

        // Enchantment slot bindings (also registers choose/upgrade event bindings)
        EnchantmentGridPage.appendSlotBindings(cmd, event);

        // Core wheel bindings
        CoreWheelPage.appendBindings(event);

        applyTabVisuals(cmd, activeTab);
        EnchantmentGridPage.populateSlots(cmd, ref, store, activeTab);
        InventoryGridPage.populateSlotItems(cmd, ref, store);
        InventoryGridPage.populateEquipSlots(cmd, ref, store);
        CharacterStatsPage.populate(cmd, ref, store);
        WeaponStatsPage.populate(cmd, ref, store, activeTab);
        CoreWheelPage.populate(cmd, ref, store);
        cmd.set("#CoreWheelContent.Visible", false);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull EventData data) {
        World world = store.getExternalData().getWorld();

        System.out.println("[Nexus] EVENT >> slotClick=" + data.slotClick
            + " | equipSlotClick=" + data.equipSlotClick
            + " | tabClick=" + data.tabClick
            + " | weaponUpgrade=" + data.weaponUpgrade);

        // ── Tab switch ────────────────────────────────────────────────────────
        if (data.tabClick != null) {
            activeTab = data.tabClick.equals("Ranged") ? WeaponTag.RANGED : WeaponTag.MELEE;
            selectedSlot = null;
            world.execute(() -> {
                UICommandBuilder update = new UICommandBuilder();
                applyTabVisuals(update, activeTab);
                EnchantmentGridPage.populateSlots(update, ref, ref.getStore(), activeTab);
                WeaponStatsPage.populate(update, ref, ref.getStore(), activeTab);
                sendUpdate(update, null, false);
            });
            return;
        }

        if ("Drop".equals(data.action)) {
            selectedSlot = null;
            // TODO: Implement Drop System
            return;
        }

        if (data.coreWheelHover != null) {
            UICommandBuilder update = new UICommandBuilder();
            CoreWheelPage.handleHover(update, data.coreWheelHover);
            sendUpdate(update, null, false);
            return;
        }

        if (data.coreSelect != null) {
            String captured = data.coreSelect;
            world.execute(() -> {
                boolean changed = CoreWheelPage.handleSelect(ref, ref.getStore(), captured);
                if (changed) {
                    UICommandBuilder update = new UICommandBuilder();
                    CoreWheelPage.populate(update, ref, ref.getStore());
                    sendUpdate(update, null, false);
                }
            });
            return;
        }

        if (data.enchantChoose != null) {
            String capturedPayload = data.enchantChoose;
            world.execute(() -> {
                boolean changed = EnchantmentGridPage.handleChoose(
                    ref, ref.getStore(), capturedPayload, activeTab);
                if (changed) {
                    UICommandBuilder update = new UICommandBuilder();
                    EnchantmentGridPage.populateSlots(update, ref, ref.getStore(), activeTab);
                    sendUpdate(update, null, false);
                    pushFullStatsUpdate(ref);
                }
            });
            return;
        }

        if (data.enchantUpgrade != null) {
            String capturedPayload = data.enchantUpgrade;
            world.execute(() -> {
                boolean changed = EnchantmentGridPage.handleUpgrade(
                    ref, ref.getStore(), capturedPayload, activeTab);
                if (changed) {
                    UICommandBuilder update = new UICommandBuilder();
                    EnchantmentGridPage.populateSlots(update, ref, ref.getStore(), activeTab);
                    sendUpdate(update, null, false);
                    pushFullStatsUpdate(ref);
                }
            });
            return;
        }

        if (data.equipSlotClick != null) {
            System.out.println("[Nexus] EQUIP BRANCH >> selectedSlot=" + selectedSlot);
            WeaponTag targetTag = data.equipSlotClick.equals("Ranged")
                ? WeaponTag.RANGED : WeaponTag.MELEE;

            if (!isValidWeaponSelectionForEquip(ref, store, selectedSlot, targetTag)) {
                System.out.println("[Nexus] EQUIP INVALID >> resetting selectedSlot");
                selectedSlot = null;
                return;
            }

            String capturedFrom = selectedSlot;
            selectedSlot = null;

            world.execute(() -> {
                try {
                    handleEquip(ref, ref.getStore(), capturedFrom, targetTag);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                UICommandBuilder update = new UICommandBuilder();
                InventoryGridPage.populateSlotItems(update, ref, ref.getStore());
                InventoryGridPage.populateEquipSlots(update, ref, ref.getStore());
                EnchantmentGridPage.populateSlots(update, ref, ref.getStore(), activeTab);
                WeaponStatsPage.populate(update, ref, ref.getStore(), activeTab);
                sendUpdate(update, null, false);
                pushFullStatsUpdate(ref);
            });
            return;
        }

        if (data.weaponUpgrade != null) {
            world.execute(() -> {
                PlayerWeaponStateComponent state = ref.getStore().getComponent(
                    ref, PlayerWeaponStateComponent.getComponentType());
                if (state == null) return;
                BsonDocument doc = activeTab == WeaponTag.RANGED
                    ? state.rangedDocument : state.meleeDocument;
                if (doc == null) return;
                Nexus.get().getWeaponUpgradeService().attemptUpgrade(ref, doc, ref.getStore());
                UICommandBuilder update = new UICommandBuilder();
                WeaponStatsPage.populate(update, ref, ref.getStore(), activeTab);
                sendUpdate(update, null, false);
                pushFullStatsUpdate(ref);
            });
            return;
        }

        if (data.slotClick == null) {
            System.out.println("[Nexus] SLOT NULL >> no slotClick, no other field matched, doing nothing");
            return;
        }

        int clickedGlobal = Integer.parseInt(data.slotClick.split(":")[1]);
        int lockedGlobal = InventoryGridPage.STORAGE_CAPACITY + InventoryGridPage.LOCKED_HOTBAR_SLOT;
        System.out.println("[Nexus] SLOT CLICK >> clickedGlobal=" + clickedGlobal + " | lockedGlobal=" + lockedGlobal);

        if (clickedGlobal == lockedGlobal) {
            System.out.println("[Nexus] LOCKED SLOT HIT >> resetting selectedSlot");
            selectedSlot = null;
            return;
        }

        if (selectedSlot == null) {
            if (!hasItem(ref, store, data.slotClick)) return;
            selectedSlot = data.slotClick;
            return;
        }

        String capturedFrom = selectedSlot;
        String capturedTo = data.slotClick;
        selectedSlot = null;
        System.out.println("[Nexus] SWAP >> from=" + capturedFrom + " to=" + capturedTo);

        world.execute(() -> {
            try {
                if (!capturedFrom.equals(capturedTo)) {
                    InventoryGridPage.swapSlots(ref, ref.getStore(), capturedFrom, capturedTo);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            UICommandBuilder update = new UICommandBuilder();
            InventoryGridPage.populateSlotItems(update, ref, ref.getStore());
            sendUpdate(update, null, false);
        });
    }

    private boolean hasItem(@Nonnull Ref<EntityStore> ref,
                            @Nonnull Store<EntityStore> store,
                            @Nonnull String slotKey) {
        int globalIndex = Integer.parseInt(slotKey.split(":")[1]);
        boolean isHotbar = globalIndex >= InventoryGridPage.STORAGE_CAPACITY;
        short slotIndex = (short) (isHotbar
            ? globalIndex - InventoryGridPage.STORAGE_CAPACITY
            : globalIndex);
        ItemContainer container = InventoryGridPage.getContainer(ref, store,
            isHotbar ? "Hotbar" : "Storage");
        if (container == null) return false;
        ItemStack stack = container.getItemStack(slotIndex);
        return stack != null && !stack.isEmpty();
    }

    // ── Equip helper ──────────────────────────────────────────────────────────

    private void handleEquip(@Nonnull Ref<EntityStore> ref,
                             @Nonnull Store<EntityStore> store,
                             @Nonnull String fromSlotKey,
                             @Nonnull WeaponTag targetTag) {

        int globalIndex = Integer.parseInt(fromSlotKey.split(":")[1]);
        boolean isHotbar = globalIndex >= InventoryGridPage.STORAGE_CAPACITY;
        short slotIndex = (short) (isHotbar
            ? globalIndex - InventoryGridPage.STORAGE_CAPACITY
            : globalIndex);

        ItemContainer container = InventoryGridPage.getContainer(ref, store,
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

        // If this weapon tag is the one currently held in hotbar slot 0, update it
        if (targetTag == weaponState.activeTag) {
            String archetypeId = incomingDoc.containsKey("archetype_id")
                ? incomingDoc.getString("archetype_id").getValue() : "unknown";
            InventoryComponent.Hotbar hotbar = store.getComponent(
                ref, InventoryComponent.Hotbar.getComponentType());
            if (hotbar != null) {
                hotbar.getInventory().setItemStackForSlot(
                    (short) 0, new ItemStack(archetypeId, 1, incomingDoc.clone()));
                hotbar.markDirty();
            }
            weaponState.activeTag = targetTag;
            Nexus.get().getWeaponEquipSystem().onWeaponEquipped(ref, incomingStack, store);
        }
    }

    private boolean isValidWeaponSelectionForEquip(@Nonnull Ref<EntityStore> ref,
                                                   @Nonnull Store<EntityStore> store,
                                                   @Nullable String pendingSlot,
                                                   @Nonnull WeaponTag targetTag) {
        if (pendingSlot == null) return false;

        int globalIndex = Integer.parseInt(pendingSlot.split(":")[1]);
        boolean isHotbar = globalIndex >= InventoryGridPage.STORAGE_CAPACITY;
        short slotIndex = (short) (isHotbar
            ? globalIndex - InventoryGridPage.STORAGE_CAPACITY
            : globalIndex);

        ItemContainer container = InventoryGridPage.getContainer(ref, store,
            isHotbar ? "Hotbar" : "Storage");
        if (container == null) return false;

        ItemStack stack = container.getItemStack(slotIndex);
        if (stack == null || stack.isEmpty()) return false;

        BsonDocument doc = stack.getMetadata();
        if (doc == null || !doc.containsKey("nexus_quality_value")) return false;

        WeaponTag itemTag = WeaponBsonSchema.readWeaponTag(doc);
        return itemTag.isCompatibleWith(targetTag);
    }

    // ── Tab visuals ───────────────────────────────────────────────────────────

    private void applyTabVisuals(@Nonnull UICommandBuilder cmd, @Nonnull WeaponTag tab) {
        boolean isMelee = tab == WeaponTag.MELEE;

        cmd.set("#UpgradeTitleLabel.Text",
            isMelee ? "ENCHANTMENT STATION > MELEE" : "ENCHANTMENT STATION > RANGED");

        cmd.set("#MeleeActive.Visible", isMelee);
        cmd.set("#MeleeDisabledButton.Visible", !isMelee);
        cmd.set("#MeleeButtonBgDefault.Visible", !isMelee);
        cmd.set("#MeleeButtonBgActive.Visible", isMelee);

        cmd.set("#RangedActive.Visible", !isMelee);
        cmd.set("#RangedDisabledButton.Visible", isMelee);
        cmd.set("#RangedButtonBgDefault.Visible", isMelee);
        cmd.set("#RangedButtonBgActive.Visible", !isMelee);
    }

    // ── EventData ─────────────────────────────────────────────────────────────

    public static class EventData {
        public static final BuilderCodec<EventData> CODEC = BuilderCodec
            .builder(EventData.class, EventData::new)
            .append(new KeyedCodec<>("SlotClick", Codec.STRING),
                (d, v) -> d.slotClick = v, d -> d.slotClick)
            .add()
            .append(new KeyedCodec<>("TabClick", Codec.STRING),
                (d, v) -> d.tabClick = v, d -> d.tabClick)
            .add()
            .append(new KeyedCodec<>("Action", Codec.STRING),
                (d, v) -> d.action = v, d -> d.action)
            .add()
            .append(new KeyedCodec<>("EquipSlotClick", Codec.STRING),
                (d, v) -> d.equipSlotClick = v, d -> d.equipSlotClick)
            .add()
            .append(new KeyedCodec<>("EnchantChoose", Codec.STRING),
                (d, v) -> d.enchantChoose = v, d -> d.enchantChoose)
            .add()
            .append(new KeyedCodec<>("EnchantUpgrade", Codec.STRING),
                (d, v) -> d.enchantUpgrade = v, d -> d.enchantUpgrade)
            .add()
            .append(new KeyedCodec<>("WeaponUpgrade", Codec.STRING),
                (d, v) -> d.weaponUpgrade = v, d -> d.weaponUpgrade)
            .add()
            .append(new KeyedCodec<>("CoreWheelHover", Codec.STRING),
                (d, v) -> d.coreWheelHover = v, d -> d.coreWheelHover)
            .add()
            .append(new KeyedCodec<>("CoreSelect", Codec.STRING),
                (d, v) -> d.coreSelect = v, d -> d.coreSelect)
            .add()
            .build();

        public String slotClick;
        public String tabClick;
        public String action;
        public String equipSlotClick;
        public String enchantChoose;
        public String enchantUpgrade;
        public String weaponUpgrade;
        public String coreWheelHover;
        public String coreSelect;

        public EventData() {
        }
    }

    private void pushFullStatsUpdate(@Nonnull Ref<EntityStore> ref) {
        reequipActiveWeapon(ref, ref.getStore());
        // Defer UI populate to the next world execute so WeaponPassiveApplicator.apply
        // (which runs inside its own world.execute) has fully completed before we read stats
        ref.getStore().getExternalData().getWorld().execute(() -> {
            if (!ref.isValid()) return;
            UICommandBuilder update = new UICommandBuilder();
            WeaponStatsPage.populate(update, ref, ref.getStore(), activeTab);
            EnchantmentGridPage.populateSlots(update, ref, ref.getStore(), activeTab);
            CharacterStatsPage.populate(update, ref, ref.getStore());
            sendUpdate(update, null, false);
        });
    }

    private void reequipActiveWeapon(@Nonnull Ref<EntityStore> ref,
                                     @Nonnull Store<EntityStore> store) {
        PlayerWeaponStateComponent state = store.getComponent(
            ref, PlayerWeaponStateComponent.getComponentType());
        if (state == null) return;

        BsonDocument doc = activeTab == WeaponTag.RANGED
            ? state.rangedDocument : state.meleeDocument;
        if (doc == null || !doc.containsKey("archetype_id")) return;

        String archetypeId = doc.getString("archetype_id").getValue();
        Nexus.get().getWeaponEquipSystem().onWeaponEquipped(
            ref,
            new ItemStack(archetypeId, 1, doc),
            store);
    }
}
