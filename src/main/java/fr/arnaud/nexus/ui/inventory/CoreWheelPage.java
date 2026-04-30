package fr.arnaud.nexus.ui.inventory;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.ability.ActiveCoreComponent;
import fr.arnaud.nexus.ability.CoreAbility;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public final class CoreWheelPage {


    static final int WHEEL_SLOT_COUNT = 4;

    private CoreWheelPage() {
    }

    static void appendBindings(@Nonnull UIEventBuilder event) {

        event.addEventBinding(CustomUIEventBindingType.MouseEntered, "#CoreWheelSlot",
            EventData.of("CoreWheelHover", "Enter"), false);

        for (int i = 0; i < WHEEL_SLOT_COUNT; i++) {
            event.addEventBinding(CustomUIEventBindingType.MouseExited, "#CoreSlot" + i,
                EventData.of("CoreWheelHover", "Leave"), false);
        }
        event.addEventBinding(CustomUIEventBindingType.MouseExited, "#CoreUnequipSlot",
            EventData.of("CoreWheelHover", "Leave"), false);

        event.addEventBinding(CustomUIEventBindingType.MouseExited, "#CoreWheelSlot",
            EventData.of("CoreWheelHover", "Leave"), false);

        for (int i = 0; i < WHEEL_SLOT_COUNT; i++) {
            event.addEventBinding(CustomUIEventBindingType.MouseEntered, "#CoreSlot" + i,
                EventData.of("CoreWheelHover", "Enter"), false);
        }
        event.addEventBinding(CustomUIEventBindingType.MouseEntered, "#CoreUnequipSlot",
            EventData.of("CoreWheelHover", "Enter"), false);

        for (int i = 0; i < WHEEL_SLOT_COUNT; i++) {
            event.addEventBinding(CustomUIEventBindingType.Activating, "#CoreSlot" + i,
                EventData.of("CoreSelect", String.valueOf(i)), false);
        }

        event.addEventBinding(CustomUIEventBindingType.Activating, "#CoreUnequipSlot",
            EventData.of("CoreSelect", "unequip"), false);
    }

    static void populate(@Nonnull UICommandBuilder cmd,
                         @Nonnull Ref<EntityStore> ref,
                         @Nonnull Store<EntityStore> store) {
        ActiveCoreComponent core = store.getComponent(ref, ActiveCoreComponent.getComponentType());

        List<CoreAbility> unlocked = new ArrayList<>();
        if (core != null) {
            for (CoreAbility a : CoreAbility.values()) {
                if (core.isUnlocked(a)) unlocked.add(a);
            }
        }

        for (int i = 0; i < WHEEL_SLOT_COUNT; i++) {
            if (i < unlocked.size()) {
                CoreAbility ability = unlocked.get(i);
                boolean isEquipped = core.hasEquipped(ability);
                cmd.set("#CoreSlot" + i + ".Visible", true);
                cmd.set("#CoreSlot" + i + "BgEquipped.Visible", isEquipped);
                cmd.set("#CoreSlot" + i + " #CoreSlot" + i + "Icon.Visible", true);
                cmd.set("#CoreSlot" + i + " #CoreSlot" + i + "Lock.Visible", false);
                cmd.set("#CoreSlot" + i + ".TooltipText",
                    ability.getDisplayName() + "\n" + ability.getDescription());
            } else {
                cmd.set("#CoreSlot" + i + ".Visible", true);
                cmd.set("#CoreSlot" + i + "BgEquipped.Visible", false);
                cmd.set("#CoreSlot" + i + " #CoreSlot" + i + "Icon.Visible", false);
                cmd.set("#CoreSlot" + i + " #CoreSlot" + i + "Lock.Visible", true);
                cmd.set("#CoreSlot" + i + ".TooltipText", "Not unlocked yet");
                cmd.setNull("#CoreSlot" + i + " #CoreSlot" + i + "Icon.Background");
            }
        }

        CoreAbility equipped = core != null ? core.getEquippedCore() : null;
        cmd.set("#CoreWheelSlotEmpty.Visible", equipped == null);
        cmd.set("#CoreWheelSlotDash.Visible", equipped == CoreAbility.DASH);
        cmd.set("#CoreWheelSlotSwitchStrike.Visible", equipped == CoreAbility.SWITCH_STRIKE);
    }

    static void handleHover(@Nonnull UICommandBuilder cmd, @Nonnull String direction) {
        cmd.set("#CoreWheelContent.Visible", "Enter".equals(direction));
    }

    static boolean handleSelect(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull String payload) {
        ActiveCoreComponent core = store.getComponent(ref, ActiveCoreComponent.getComponentType());
        if (core == null) return false;

        if ("unequip".equals(payload)) {
            if (core.isEmpty()) return false;
            core.unequip();
            store.putComponent(ref, ActiveCoreComponent.getComponentType(), core);
            return true;
        }

        int slotIndex;
        try {
            slotIndex = Integer.parseInt(payload);
        } catch (NumberFormatException e) {
            return false;
        }

        List<CoreAbility> unlocked = new ArrayList<>();
        for (CoreAbility a : CoreAbility.values()) {
            if (core.isUnlocked(a)) unlocked.add(a);
        }

        if (slotIndex < 0 || slotIndex >= unlocked.size()) return false;

        CoreAbility selected = unlocked.get(slotIndex);
        boolean changed = core.equip(selected);
        if (changed) {
            store.putComponent(ref, ActiveCoreComponent.getComponentType(), core);
        }
        return changed;
    }
}
