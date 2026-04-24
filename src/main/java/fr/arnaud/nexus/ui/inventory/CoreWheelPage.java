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

/**
 * Handles the Core Ability Wheel panel embedded in the character panel.
 * <p>
 * Slot layout (5 ability slots, clockwise from top, 72° steps):
 * <pre>
 *        [0]
 *    [4]     [1]
 *    [3]     [2]
 *        [X]   ← unequip
 * </pre>
 * Slots are filled in declaration order of {@link CoreAbility#values()}.
 * Slots beyond the number of registered abilities are hidden.
 */
public final class CoreWheelPage {

    /**
     * Maximum ability slots rendered in the wheel — matches the .ui file.
     */
    static final int WHEEL_SLOT_COUNT = 4;

    private CoreWheelPage() {
    }

    // --- Build-time: bindings + populate ---

    static void appendBindings(@Nonnull UIEventBuilder event) {
        // #CoreWheel is always Visible:true — only #CoreWheelContent is toggled.
        // This prevents the flicker race where toggling the element's own visibility
        // caused spurious MouseEntered/Exited on the element itself.
        event.addEventBinding(CustomUIEventBindingType.MouseEntered, "#CoreWheelSlot",
            EventData.of("CoreWheelHover", "Enter"), false);
        event.addEventBinding(CustomUIEventBindingType.MouseExited, "#CoreWheel",
            EventData.of("CoreWheelHover", "Leave"), false);

        for (int i = 0; i < WHEEL_SLOT_COUNT; i++) {
            final String slotId = "#CoreSlot" + i;
            event.addEventBinding(CustomUIEventBindingType.Activating, slotId,
                EventData.of("CoreSelect", String.valueOf(i)), false);
        }

        event.addEventBinding(CustomUIEventBindingType.Activating, "#CoreUnequipSlot",
            EventData.of("CoreSelect", "unequip"), false);
    }

    static void populate(@Nonnull UICommandBuilder cmd,
                         @Nonnull Ref<EntityStore> ref,
                         @Nonnull Store<EntityStore> store) {
        ActiveCoreComponent core = store.getComponent(ref, ActiveCoreComponent.getComponentType());

        // Flatten all known abilities for slot assignment
        CoreAbility[] all = CoreAbility.values();
        List<CoreAbility> unlocked = new ArrayList<>();
        if (core != null) {
            for (CoreAbility a : all) {
                if (core.isUnlocked(a)) unlocked.add(a);
            }
        }

        for (int i = 0; i < WHEEL_SLOT_COUNT; i++) {
            String slot = "#CoreSlot" + i;
            if (i < unlocked.size()) {
                CoreAbility ability = unlocked.get(i);
                boolean isEquipped = core != null && core.hasEquipped(ability);

                cmd.set(slot + ".Visible", true);
                cmd.set("#CoreSlot" + i + "BgEquipped.Visible", isEquipped);
            } else {
                cmd.set(slot + ".Visible", true);
                cmd.set("#CoreSlot" + i + "BgEquipped.Visible", false);
                cmd.setNull(slot + " #CoreSlot" + i + "Icon.Background");
            }
        }
    }

    // --- Event handlers ---

    /**
     * @return true if the wheel visibility changed and an update should be sent.
     */
    static boolean handleHover(@Nonnull UICommandBuilder cmd, @Nonnull String direction) {
        boolean visible = "Enter".equals(direction);
        cmd.set("#CoreWheelContent.Visible", visible);
        return true;
    }

    /**
     * Handles a slot click from the wheel.
     *
     * @param payload slot index as string, or "unequip"
     * @return true if the equipped core changed and a repopulate is needed.
     */
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
        // equip() guards against non-unlocked — redundant here but safe
        boolean changed = core.equip(selected);
        if (changed) {
            store.putComponent(ref, ActiveCoreComponent.getComponentType(), core);
        }
        return changed;
    }

    // --- Helpers ---

    private static String displayName(CoreAbility ability) {
        return switch (ability) {
            case DASH -> "Dash";
            case SWITCH_STRIKE -> "Switch Strike";
        };
    }
}
