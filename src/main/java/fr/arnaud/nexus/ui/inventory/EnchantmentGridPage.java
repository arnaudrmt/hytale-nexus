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
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentCostCalculator;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentDefinition;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentRegistry;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentStatDefinition;
import org.bson.BsonDocument;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public final class EnchantmentGridPage {

    static final int ENCHANT_SLOT_COUNT = 3;

    private EnchantmentGridPage() {
    }

    static void appendSlotBindings(@Nonnull UICommandBuilder cmd,
                                   @Nonnull UIEventBuilder event) {
        cmd.appendInline("#EnchantSlotCards",
            "Group { LayoutMode: Left; HitTestVisible: false; FlexWeight: 1; }");

        for (int i = 0; i < ENCHANT_SLOT_COUNT; i++) {
            cmd.append("#EnchantSlotCards[0]", "Pages/NexusEnchantSlot.ui");

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

    static void populateSlots(@Nonnull UICommandBuilder cmd,
                              @Nonnull Ref<EntityStore> ref,
                              @Nonnull Store<EntityStore> store,
                              @Nonnull WeaponTag activeTab) {

        List<EnchantmentSlot> slots = getEnchantmentSlots(ref, store, activeTab);

        PlayerStatsManager statsManager = Nexus.get().getPlayerStatsManager();
        float essence = statsManager.isReady()
            ? statsManager.getEssenceDust(ref, store) : 0f;

        for (int i = 0; i < ENCHANT_SLOT_COUNT; i++) {
            if (slots == null || i >= slots.size()) {
                setSlotVisible(cmd, i, true, false, false);
                continue;
            }
            populateSlot(cmd, i, slots.get(i), essence);
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

    private static void populateSlot(@Nonnull UICommandBuilder cmd,
                                     int i,
                                     @Nonnull EnchantmentSlot slot,
                                     float essence) {
        if (slot.chosen() == null) {
            setSlotVisible(cmd, i, false, true, false);
            populateChoiceState(cmd, i, slot, essence);
        } else {
            setSlotVisible(cmd, i, false, false, true);
            populateActiveState(cmd, i, slot, essence);
        }
    }

    private static void populateChoiceState(@Nonnull UICommandBuilder cmd,
                                            int i,
                                            @Nonnull EnchantmentSlot slot,
                                            float essence) {
        String base = slotBase(i);

        EnchantmentDefinition defA = EnchantmentRegistry.get().getDefinition(slot.choiceA());
        EnchantmentDefinition defB = EnchantmentRegistry.get().getDefinition(slot.choiceB());

        if (defA != null) {
            cmd.set(base + " #ChoiceATitle.Text", defA.getName());
            cmd.set(base + " #ChoiceADesc.Text", defA.getDescription());
            int costA = EnchantmentCostCalculator.costForLevel(defA, 1);
            cmd.set(base + " #ChoiceAButton.Text", costA + " ESSENCES");
            cmd.set(base + " #ChoiceAButton.Disabled", essence < costA);
        }

        if (defB != null) {
            cmd.set(base + " #ChoiceBTitle.Text", defB.getName());
            cmd.set(base + " #ChoiceBDesc.Text", defB.getDescription());
            int costB = EnchantmentCostCalculator.costForLevel(defB, 1);
            cmd.set(base + " #ChoiceBButton.Text", costB + " ESSENCES");
            cmd.set(base + " #ChoiceBButton.Disabled", essence < costB);
        }
    }

    private static void populateActiveState(@Nonnull UICommandBuilder cmd,
                                            int i,
                                            @Nonnull EnchantmentSlot slot,
                                            float essence) {
        String base = slotBase(i);
        EnchantmentDefinition def = EnchantmentRegistry.get().getDefinition(slot.chosen());
        if (def == null) return;

        int currentLevel = slot.currentLevel();

        cmd.set(base + " #EnchantName.Text", def.getName() + " " + toRoman(currentLevel));
        cmd.set(base + " #EnchantLevel.Text", "Level " + currentLevel);
        cmd.set(base + " #EnchantDesc.Text", def.getDescription());

        for (EnchantmentDefinition e : EnchantmentRegistry.get().getAllDefinitions()) {
            cmd.set(base + " #EnchantIcon " + enchantIconElementId(e.getId()) + ".Visible", false);
        }
        if (slot.chosen() != null)
            cmd.set(base + " #EnchantIcon " + enchantIconElementId(slot.chosen()) + ".Visible", true);

        List<EnchantmentStatDefinition> stats = def.getStats();
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
            cmd.set(rowBase + "[1].Text", stat.getDisplayName());
        }

        boolean maxed = currentLevel >= def.getMaxLevel();
        if (maxed) {
            cmd.set(base + " #EnchantCost.Text", Message.translation("nexus.max.item.level.display"));
            cmd.set(base + " #UpgradeEnchantButton.Disabled", true);
            cmd.set(base + " #UpgradeEnchantButton.TooltipText", Message.translation("nexus.maximum.item.level.reached"));
        } else {
            int nextLevel = currentLevel + 1;
            int cost = EnchantmentCostCalculator.costForLevel(def, nextLevel);
            cmd.set(base + " #EnchantCost.Text", formatNumber(cost));
            cmd.set(base + " #UpgradeEnchantButton.Disabled", essence < cost);
            cmd.set(base + " #UpgradeEnchantButton.TooltipText",
                buildEnchantUpgradeTooltip(def, currentLevel, nextLevel));
        }
    }

    private static String buildEnchantUpgradeTooltip(@Nonnull EnchantmentDefinition def,
                                                     int currentLevel,
                                                     int nextLevel) {
        StringBuilder sb = new StringBuilder();

        for (EnchantmentStatDefinition stat : def.getStats()) {
            double currentVal = stat.getStatValueForLevel(currentLevel);
            double nextVal = stat.getStatValueForLevel(nextLevel);
            double delta = nextVal - currentVal;

            if (stat.getType() == EnchantmentStatDefinition.StatType.CURVE) {
                sb.append(stat.getDisplayName()).append(":")
                  .append("  ×").append(String.format("%.2f", currentVal))
                  .append(" -> ×").append(String.format("%.2f", nextVal))
                  .append(" (").append(String.format("%+.2f", delta)).append(")\n");
            } else {
                sb.append(stat.getDisplayName())
                  .append("  ")
                  .append(formatStatValue(stat, currentLevel))
                  .append(" -> ")
                  .append(formatStatValue(stat, nextLevel))
                  .append(" (")
                  .append(delta > 0 ? "+" : "")
                  .append(delta == Math.floor(delta) ? String.valueOf((int) delta) : String.format("%.1f", delta))
                  .append(")\n");
            }
        }
        return sb.toString().stripTrailing();
    }

    static boolean handleChoose(@Nonnull Ref<EntityStore> ref,
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
        EnchantmentDefinition def = EnchantmentRegistry.get().getDefinition(chosenId);
        if (def == null) return false;

        PlayerStatsManager statsManager = Nexus.get().getPlayerStatsManager();
        int cost = EnchantmentCostCalculator.costForLevel(def, 1);
        if (!statsManager.isReady() || statsManager.getEssenceDust(ref, store) < cost) return false;

        statsManager.removeEssenceDust(ref, store, cost);

        slots.set(slotIndex, slot.withChoice(chosenId));
        WeaponBsonSchema.writeEnchantmentSlots(doc, slots);
        return true;
    }

    static boolean handleUpgrade(@Nonnull Ref<EntityStore> ref,
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

        EnchantmentDefinition def = EnchantmentRegistry.get().getDefinition(slot.chosen());
        if (def == null) return false;

        int currentLevel = slot.currentLevel();
        if (currentLevel >= def.getMaxLevel()) return false;

        int nextLevel = currentLevel + 1;
        int cost = EnchantmentCostCalculator.costForLevel(def, nextLevel);

        PlayerStatsManager statsManager = Nexus.get().getPlayerStatsManager();
        if (!statsManager.isReady() || statsManager.getEssenceDust(ref, store) < cost) return false;

        statsManager.removeEssenceDust(ref, store, cost);

        slots.set(slotIndex, slot.withLevel(nextLevel));
        WeaponBsonSchema.writeEnchantmentSlots(doc, slots);
        return true;
    }

    private static void setSlotVisible(@Nonnull UICommandBuilder cmd,
                                       int i,
                                       boolean locked,
                                       boolean choice,
                                       boolean active) {
        String base = slotBase(i);
        cmd.set(base + " #LockedState.Visible", locked);
        cmd.set(base + " #ChoiceState.Visible", choice);
        cmd.set(base + " #ActiveState.Visible", active);
    }

    private static String slotBase(int i) {
        return "#EnchantSlotCards[0][" + i + "]";
    }

    private static String formatStatValue(@Nonnull EnchantmentStatDefinition stat, int level) {
        double raw = stat.getStatValueForLevel(level);
        if (stat.getType() == EnchantmentStatDefinition.StatType.CURVE) {
            return String.format("×%.2f", raw);
        } else {
            return raw == Math.floor(raw) ? "+" + (int) raw : String.format("+%.1f", raw);
        }
    }

    private static String formatNumber(int n) {
        if (n < 1000) return String.valueOf(n);
        return (n / 1000) + " " + String.format("%03d", n % 1000);
    }

    private static String toRoman(int level) {
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(level);
        };
    }

    private static String enchantIconElementId(String enchantmentId) {
        return "#Icon" + enchantmentId.replace("_", "");
    }
}
