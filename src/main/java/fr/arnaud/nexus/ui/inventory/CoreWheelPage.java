package fr.arnaud.nexus.ui.inventory;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.ability.core.ActiveCoreComponent;
import fr.arnaud.nexus.ability.core.CoreAbility;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public final class CoreWheelPage {


    static final int WHEEL_SLOT_COUNT = 4;

    private CoreWheelPage() {
    }

    static void appendCoreWheelEventBindings(@Nonnull UIEventBuilder event) {

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

    static void populateCoreWheelSlots(@Nonnull UICommandBuilder cmd,
                                       @Nonnull Ref<EntityStore> ref,
                                       @Nonnull Store<EntityStore> store) {
        ActiveCoreComponent core = store.getComponent(ref, ActiveCoreComponent.getComponentType());

        List<CoreAbility> unlocked = new ArrayList<>();
        if (core != null) {
            for (CoreAbility a : CoreAbility.values()) {
                if (core.isUnlocked(a)) unlocked.add(a);
            }
        }

        for (CoreAbility ability : CoreAbility.values()) {
            String iconSelector = coreIconElementSelector(ability);
            for (int i = 0; i < WHEEL_SLOT_COUNT; i++) {
                cmd.set("#CoreSlot" + i + " #CoreSlot" + i + "Icon " + iconSelector + ".Visible", false);
            }
            cmd.set("#CoreWheelEquippedIcon " + iconSelector + ".Visible", false);
        }

        for (int i = 0; i < WHEEL_SLOT_COUNT; i++) {
            boolean hasAbility = i < unlocked.size();
            cmd.set("#CoreSlot" + i + "BgEquipped.Visible", hasAbility && core.hasEquipped(unlocked.get(i)));
            cmd.set("#CoreSlot" + i + " #CoreSlot" + i + "Icon.Visible", hasAbility);
            cmd.set("#CoreSlot" + i + " #CoreSlot" + i + "Lock.Visible", !hasAbility);

            if (hasAbility) {
                CoreAbility ability = unlocked.get(i);
                cmd.set("#CoreSlot" + i + " #CoreSlot" + i + "Icon " + coreIconElementSelector(ability) + ".Visible", true);
                cmd.set("#CoreSlot" + i + ".TooltipText", Message.translation(ability.getTooltipKey()));
            } else {
                cmd.set("#CoreSlot" + i + ".TooltipText", Message.translation("inventory.character.core.slotLocked"));
            }
        }

        CoreAbility equipped = core != null ? core.getEquippedCore() : null;
        cmd.set("#CoreWheelSlotEmpty.Visible", equipped == null);
        if (equipped != null) {
            cmd.set("#CoreWheelEquippedIcon " + coreIconElementSelector(equipped) + ".Visible", true);
        }
    }

    static void applyCoreWheelHoverVisibility(@Nonnull UICommandBuilder cmd, @Nonnull String direction) {
        cmd.set("#CoreWheelContent.Visible", "Enter".equals(direction));
    }

    static boolean selectCoreAbility(@Nonnull Ref<EntityStore> ref,
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

    private static String coreIconElementSelector(CoreAbility ability) {
        return "#IconCore" + ability.name().charAt(0) + ability.name().substring(1).toLowerCase()
                                                               .replace("_", "");
    }
}
