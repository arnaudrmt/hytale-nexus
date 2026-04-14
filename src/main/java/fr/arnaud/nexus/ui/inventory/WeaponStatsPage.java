package fr.arnaud.nexus.ui.inventory;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.item.weapon.component.PlayerWeaponStateComponent;
import fr.arnaud.nexus.item.weapon.data.WeaponTag;
import org.bson.BsonDocument;

import javax.annotation.Nonnull;

public final class WeaponStatsPage {

    private WeaponStatsPage() {
    }

    static void populate(@Nonnull UICommandBuilder cmd,
                         @Nonnull Ref<EntityStore> ref,
                         @Nonnull Store<EntityStore> store,
                         @Nonnull WeaponTag activeTab) {

        PlayerWeaponStateComponent state = store.getComponent(
            ref, PlayerWeaponStateComponent.getComponentType());


        if (state == null) {
            renderEmpty(cmd);
            return;
        }

        BsonDocument doc = activeTab == WeaponTag.MELEE
            ? state.meleeDocument
            : state.rangedDocument;

        if (doc == null) {
            renderEmpty(cmd);
            return;
        }

        String archetypeId = activeTab == WeaponTag.MELEE
            ? "Nexus_Melee_Sword_Default"
            : "Nexus_Ranged_Staff_Default";

        cmd.set("#WeaponIcon.ItemId", archetypeId);
        cmd.set("#WeaponName.Text", archetypeId);
    }

    private static void renderEmpty(@Nonnull UICommandBuilder cmd) {
        cmd.setNull("#WeaponIcon.ItemId");
        cmd.set("#WeaponName.Text", "—");
        cmd.set("#WeaponQuality.Text", "—");
    }

    private static String colorToHex(com.hypixel.hytale.protocol.Color color) {
        if (color == null) return "#ffffff";
        return String.format("#%02x%02x%02x", color.red, color.green, color.blue);
    }
}
