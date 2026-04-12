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
import fr.arnaud.nexus.item.weapon.upgrade.WeaponUpgradeProvider;
import org.bson.BsonDocument;

import javax.annotation.Nonnull;

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

        // Drop button
        event.addEventBinding(CustomUIEventBindingType.Activating, "#DropItemButton",
            com.hypixel.hytale.server.core.ui.builder.EventData.of("Action", "Drop"), false);

        // Inventory grid bindings
        InventoryGridPage.appendSlotBindings(cmd, event, "#InventorySlotCards",
            InventoryGridPage.STORAGE_CAPACITY, 0);
        InventoryGridPage.appendSlotBindings(cmd, event, "#HotbarSlotCards",
            InventoryGridPage.HOTBAR_CAPACITY, InventoryGridPage.STORAGE_CAPACITY);

        // Upgrade button
        event.addEventBinding(CustomUIEventBindingType.Activating, "#UpgradeButton",
            com.hypixel.hytale.server.core.ui.builder.EventData.of("Action", "Upgrade"), false);

        applyTabVisuals(cmd, activeTab);
        InventoryGridPage.populateSlotItems(cmd, ref, store);
        InventoryGridPage.populateEquipSlots(cmd, ref, store);
        WeaponStatsPage.populate(cmd, ref, store, activeTab);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull EventData data) {
        World world = store.getExternalData().getWorld();

        // ── Tab switch ────────────────────────────────────────────────────────
        if (data.tabClick != null) {
            activeTab = data.tabClick.equals("Ranged") ? WeaponTag.RANGED : WeaponTag.MELEE;
            selectedSlot = null;
            world.execute(() -> {
                UICommandBuilder update = new UICommandBuilder();
                applyTabVisuals(update, activeTab);
                WeaponStatsPage.populate(update, ref, ref.getStore(), activeTab);
                sendUpdate(update, null, false);
            });
            return;
        }

        // ── Actions (Upgrade / Drop) ──────────────────────────────────────────
        if ("Upgrade".equals(data.action)) {
            world.execute(() -> {
                WeaponUpgradeProvider.get().upgradeWeaponRarity(ref, ref.getStore());
                UICommandBuilder update = new UICommandBuilder();
                WeaponStatsPage.populate(update, ref, ref.getStore(), activeTab);
                sendUpdate(update, null, false);
            });
            return;
        }

        if ("Drop".equals(data.action)) {
            // Clear the pending selection — item stays in inventory
            selectedSlot = null;
            return;
        }

        // ── Equip slot click ──────────────────────────────────────────────────
        if (data.equipSlotClick != null && selectedSlot != null) {
            WeaponTag targetTag = data.equipSlotClick.equals("Ranged")
                ? WeaponTag.RANGED : WeaponTag.MELEE;
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
                WeaponStatsPage.populate(update, ref, ref.getStore(), activeTab);
                sendUpdate(update, null, false);
            });
            return;
        }

        // If equip slot clicked but nothing selected yet, ignore it —
        // equip slots are not valid first-click selections
        if (data.equipSlotClick != null) return;

        // ── Inventory slot click (select / swap) ──────────────────────────────
        if (data.slotClick == null) return;

        if (selectedSlot == null) {
            selectedSlot = data.slotClick;
            return;
        }

        String capturedFrom = selectedSlot;
        String capturedTo = data.slotClick;
        selectedSlot = null;

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

        ItemStack stack = container.getItemStack(slotIndex);
        if (stack == null || stack.isEmpty()) return;

        BsonDocument doc = stack.getMetadata();
        if (doc == null || !doc.containsKey("nexus_rarity")) return;

        WeaponTag itemTag = WeaponBsonSchema.readWeaponTag(doc);
        if (!itemTag.isCompatibleWith(targetTag)) return;

        // Update the component document for this weapon type
        PlayerWeaponStateComponent weaponState = store.getComponent(
            ref, PlayerWeaponStateComponent.getComponentType());
        if (weaponState == null) return;

        weaponState.setDocument(targetTag, doc.clone());

        // If equipping the currently active tab's weapon, also update activeTag
        // and call the equip system so stats/enchantments apply
        if (targetTag == activeTab) {
            weaponState.activeTag = targetTag;
            Nexus.get().getWeaponEquipSystem().onWeaponEquipped(ref, stack, store);
        }
    }

    // ── Tab visuals ───────────────────────────────────────────────────────────

    private void applyTabVisuals(@Nonnull UICommandBuilder cmd, @Nonnull WeaponTag tab) {
        boolean isMelee = tab == WeaponTag.MELEE;

        cmd.set("#UpgradeTitleLabel.Text",
            isMelee ? "ENCHANTMENT STATION > MELEE" : "ENCHANTMENT STATION > RANGED");

        cmd.set("#EnchantmentMeleePanel.Visible", isMelee);
        cmd.set("#EnchantmentRangedPanel.Visible", !isMelee);

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
            .addField(new KeyedCodec<>("SlotClick", Codec.STRING),
                (d, v) -> d.slotClick = v, d -> d.slotClick)
            .addField(new KeyedCodec<>("TabClick", Codec.STRING),
                (d, v) -> d.tabClick = v, d -> d.tabClick)
            .addField(new KeyedCodec<>("Action", Codec.STRING),
                (d, v) -> d.action = v, d -> d.action)
            .addField(new KeyedCodec<>("EquipSlotClick", Codec.STRING),
                (d, v) -> d.equipSlotClick = v, d -> d.equipSlotClick)
            .build();

        public String slotClick;
        public String tabClick;
        public String action;
        public String equipSlotClick;

        public EventData() {
        }
    }

    void pushUpdate(@Nonnull UICommandBuilder cmd) {
        sendUpdate(cmd, null, false);
    }
}
