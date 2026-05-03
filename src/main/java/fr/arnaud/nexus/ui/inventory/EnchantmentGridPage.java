package fr.arnaud.nexus.ui.inventory;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.feature.resource.PlayerStatsManager;
import fr.arnaud.nexus.item.weapon.component.PlayerWeaponStateComponent;
import fr.arnaud.nexus.item.weapon.data.EnchantmentSlot;
import fr.arnaud.nexus.item.weapon.data.WeaponBsonSchema;
import fr.arnaud.nexus.item.weapon.data.WeaponTag;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentDefinition;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentRegistry;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentStatDefinition;
import fr.arnaud.nexus.math.StatMath;
import fr.arnaud.nexus.util.FormatUtil;
import org.bson.BsonDocument;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public final class EnchantmentGridPage {

    static final int ENCHANT_SLOT_COUNT = 3;

    private EnchantmentGridPage() {
    }

    static void appendEnchantmentSlotBindings(@Nonnull UICommandBuilder cmd,
                                              @Nonnull UIEventBuilder event) {
        cmd.appendInline("#EnchantSlotCards",
            "Group { LayoutMode: Left; HitTestVisible: false; FlexWeight: 1; }");

        for (int i = 0; i < ENCHANT_SLOT_COUNT; i++) {
            cmd.append("#EnchantSlotCards[0]", "Pages/EnchantSlot.ui");

            String base = "#EnchantSlotCards[0][" + i + "]";

            event.addEventBinding(CustomUIEventBindingType.Activating,
                base + " #ChoiceAButton",
                com.hypixel.hytale.server.core.ui.builder.EventData.of("EnchantChoose", i + ":A"),
                false);

            event.addEventBinding(CustomUIEventBindingType.Activating,
                base + " #ChoiceBButton",
                com.hypixel.hytale.server.core.ui.builder.EventData.of("EnchantChoose", i + ":B"),
                false);

            event.addEventBinding(CustomUIEventBindingType.Activating,
                base + " #UpgradeEnchantButton",
                com.hypixel.hytale.server.core.ui.builder.EventData.of("EnchantUpgrade", String.valueOf(i)),
                false);
        }
    }

    static void populateEnchantmentSlots(@Nonnull UICommandBuilder cmd,
                                         @Nonnull Ref<EntityStore> ref,
                                         @Nonnull Store<EntityStore> store,
                                         @Nonnull WeaponTag activeTab) {

        List<EnchantmentSlot> slots = getEnchantmentSlots(ref, store, activeTab);

        PlayerStatsManager statsManager = Nexus.getInstance().getPlayerStatsManager();
        float essence = statsManager.isReady()
            ? statsManager.getEssenceDust(ref, store) : 0f;

        for (int i = 0; i < ENCHANT_SLOT_COUNT; i++) {
            if (slots == null || i >= slots.size()) {
                setEnchantmentSlotStateVisibility(cmd, i, true, false, false);
                continue;
            }
            populateEnchantmentSlot(cmd, i, slots.get(i), essence);
        }
    }

    @Nullable
    private static List<EnchantmentSlot> getEnchantmentSlots(@Nonnull Ref<EntityStore> ref,
                                                             @Nonnull Store<EntityStore> store,
                                                             @Nonnull WeaponTag activeTab) {
        PlayerWeaponStateComponent weaponState = store.getComponent(
            ref, PlayerWeaponStateComponent.getComponentType());
        if (weaponState == null) return null;

        BsonDocument doc = activeTab == WeaponTag.RANGED
            ? weaponState.rangedDocument
            : weaponState.meleeDocument;
        if (doc == null) return null;

        return WeaponBsonSchema.readEnchantmentSlots(doc);
    }

    private static void populateEnchantmentSlot(@Nonnull UICommandBuilder cmd,
                                                int i,
                                                @Nonnull EnchantmentSlot slot,
                                                float essence) {
        if (slot.chosen() == null) {
            setEnchantmentSlotStateVisibility(cmd, i, false, true, false);
            populateEnchantmentChoiceState(cmd, i, slot, essence);
        } else {
            setEnchantmentSlotStateVisibility(cmd, i, false, false, true);
            populateActiveEnchantmentState(cmd, i, slot, essence);
        }
    }

    private static void populateEnchantmentChoiceState(@Nonnull UICommandBuilder cmd,
                                                       int i,
                                                       @Nonnull EnchantmentSlot slot,
                                                       float essence) {
        String base = enchantmentSlotSelector(i);

        EnchantmentDefinition defA = EnchantmentRegistry.getInstance().getDefinition(slot.choiceA());
        EnchantmentDefinition defB = EnchantmentRegistry.getInstance().getDefinition(slot.choiceB());

        if (defA != null) {
            cmd.set(base + " #ChoiceATitle.Text", Message.translation(defA.key_name()).param("level", ""));
            cmd.set(base + " #ChoiceADesc.Text", Message.translation(defA.key_description()));
            int costA = defA.costForLevel(1);
            cmd.set(base + " #ChoiceAButton.Text",
                Message.translation("nexus.enchant.cost").param("amount", FormatUtil.formatGroupedInteger(costA)));
            cmd.set(base + " #ChoiceAButton.Disabled", essence < costA);
        }

        if (defB != null) {
            cmd.set(base + " #ChoiceBTitle.Text", Message.translation(defB.key_name()).param("level", ""));
            cmd.set(base + " #ChoiceBDesc.Text", Message.translation(defB.key_description()));
            int costB = defB.costForLevel(1);
            cmd.set(base + " #ChoiceBButton.Text",
                Message.translation("nexus.enchant.cost").param("amount", FormatUtil.formatGroupedInteger(costB)));
            cmd.set(base + " #ChoiceBButton.Disabled", essence < costB);
        }
    }

    private static void populateActiveEnchantmentState(@Nonnull UICommandBuilder cmd,
                                                       int i,
                                                       @Nonnull EnchantmentSlot slot,
                                                       float essence) {
        String base = enchantmentSlotSelector(i);
        EnchantmentDefinition def = EnchantmentRegistry.getInstance().getDefinition(slot.chosen());
        if (def == null) return;

        int currentLevel = slot.currentLevel();

        cmd.set(base + " #EnchantName.Text", Message.translation(def.key_name()).param("level", FormatUtil.toRoman(currentLevel)));
        cmd.set(base + " #EnchantLevel.Text",
            Message.translation("nexus.enchant.level").param("level", FormatUtil.toRoman(currentLevel)));
        cmd.set(base + " #EnchantDesc.Text", Message.translation(def.key_description()));

        for (EnchantmentDefinition e : EnchantmentRegistry.getInstance().getAllDefinitions()) {
            cmd.set(base + " #EnchantIcon " + enchantmentIconElementSelector(e.id()) + ".Visible", false);
        }

        if (slot.chosen() != null)
            cmd.set(base + " #EnchantIcon " + enchantmentIconElementSelector(slot.chosen()) + ".Visible", true);

        List<EnchantmentStatDefinition> stats = def.stats();
        String statsContainer = base + " #EnchantStatRows";
        cmd.clear(statsContainer);
        for (int s = 0; s < stats.size(); s++) {
            EnchantmentStatDefinition stat = stats.get(s);
            cmd.appendInline(statsContainer,
                "Group { LayoutMode: Left; Anchor: (Height: 28, Top: 4); }");
            String rowBase = statsContainer + "[" + s + "]";
            cmd.appendInline(rowBase,
                "Label { Style: (TextColor: #99c995, FontSize: 14, RenderBold: true); }");
            cmd.appendInline(rowBase,
                "Label { Anchor: (Left: 8); Style: (TextColor: #ffffff, FontSize: 13); }");
            cmd.set(rowBase + "[0].Text", formatStatValue(stat, currentLevel));
            cmd.set(rowBase + "[1].Text", Message.translation(stat.getDisplayName()).param("amount", ""));
        }

        boolean maxed = currentLevel >= def.maxLevel();
        if (maxed) {
            cmd.set(base + " #EnchantCost.Text", Message.translation("nexus.enchant.maxLevel"));
            cmd.set(base + " #UpgradeEnchantButton.Disabled", true);
            cmd.set(base + " #UpgradeEnchantButton.TooltipText", Message.translation("nexus.enchant.MaxLevelReached"));
        } else {
            int nextLevel = currentLevel + 1;
            int cost = def.costForLevel(nextLevel);
            cmd.set(base + " #EnchantCost.Text", FormatUtil.formatGroupedInteger(cost));
            cmd.set(base + " #UpgradeEnchantButton.Disabled", essence < cost);
        }
    }

    static boolean commitEnchantmentChoice(@Nonnull Ref<EntityStore> ref,
                                           @Nonnull Store<EntityStore> store,
                                           @Nonnull String payload,
                                           @Nonnull WeaponTag activeTab) {
        String[] parts = payload.split(":");
        if (parts.length != 2) return false;

        int slotIndex;
        try {
            slotIndex = Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            return false;
        }
        boolean pickA = parts[1].equals("A");

        PlayerWeaponStateComponent weaponState = store.getComponent(
            ref, PlayerWeaponStateComponent.getComponentType());
        if (weaponState == null) return false;

        BsonDocument doc = activeTab == WeaponTag.RANGED
            ? weaponState.rangedDocument : weaponState.meleeDocument;
        if (doc == null) return false;

        List<EnchantmentSlot> slots = WeaponBsonSchema.readEnchantmentSlots(doc);
        if (slotIndex >= slots.size()) return false;

        EnchantmentSlot slot = slots.get(slotIndex);
        if (slot.chosen() != null) return false;

        String chosenId = pickA ? slot.choiceA() : slot.choiceB();
        EnchantmentDefinition def = EnchantmentRegistry.getInstance().getDefinition(chosenId);
        if (def == null) return false;

        PlayerStatsManager statsManager = Nexus.getInstance().getPlayerStatsManager();
        int cost = def.costForLevel(1);
        if (!statsManager.isReady() || statsManager.getEssenceDust(ref, store) < cost) return false;

        statsManager.removeEssenceDust(ref, store, cost);

        slots.set(slotIndex, slot.withChoice(chosenId));
        WeaponBsonSchema.writeEnchantmentSlots(doc, slots);
        return true;
    }

    static boolean upgradeEnchantment(@Nonnull Ref<EntityStore> ref,
                                      @Nonnull Store<EntityStore> store,
                                      @Nonnull String payload,
                                      @Nonnull WeaponTag activeTab) {
        int slotIndex;
        try {
            slotIndex = Integer.parseInt(payload);
        } catch (NumberFormatException e) {
            return false;
        }

        PlayerWeaponStateComponent weaponState = store.getComponent(
            ref, PlayerWeaponStateComponent.getComponentType());
        if (weaponState == null) return false;

        BsonDocument doc = activeTab == WeaponTag.RANGED
            ? weaponState.rangedDocument : weaponState.meleeDocument;
        if (doc == null) return false;

        List<EnchantmentSlot> slots = WeaponBsonSchema.readEnchantmentSlots(doc);
        if (slotIndex >= slots.size()) return false;

        EnchantmentSlot slot = slots.get(slotIndex);
        if (slot.chosen() == null) return false;

        EnchantmentDefinition def = EnchantmentRegistry.getInstance().getDefinition(slot.chosen());
        if (def == null) return false;

        int currentLevel = slot.currentLevel();
        if (currentLevel >= def.maxLevel()) return false;

        int nextLevel = currentLevel + 1;
        int cost = def.costForLevel(nextLevel);

        PlayerStatsManager statsManager = Nexus.getInstance().getPlayerStatsManager();
        if (!statsManager.isReady() || statsManager.getEssenceDust(ref, store) < cost) return false;

        statsManager.removeEssenceDust(ref, store, cost);

        slots.set(slotIndex, slot.withLevel(nextLevel));
        WeaponBsonSchema.writeEnchantmentSlots(doc, slots);
        return true;
    }

    private static void setEnchantmentSlotStateVisibility(@Nonnull UICommandBuilder cmd,
                                                          int i,
                                                          boolean locked,
                                                          boolean choice,
                                                          boolean active) {
        String base = enchantmentSlotSelector(i);
        cmd.set(base + " #LockedState.Visible", locked);
        cmd.set(base + " #ChoiceState.Visible", choice);
        cmd.set(base + " #ActiveState.Visible", active);
    }

    private static String enchantmentSlotSelector(int i) {
        return "#EnchantSlotCards[0][" + i + "]";
    }

    private static String formatStatValue(EnchantmentStatDefinition stat, int level) {
        double raw = stat.getStatValueForLevel(level);
        return stat.getType() == StatMath.GrowthType.SCALAR
            ? FormatUtil.formatChange(raw, true)
            : FormatUtil.formatSignedSmart(raw);
    }

    private static String enchantmentIconElementSelector(String enchantmentId) {
        return "#Icon" + enchantmentId.replace("_", "");
    }
}
