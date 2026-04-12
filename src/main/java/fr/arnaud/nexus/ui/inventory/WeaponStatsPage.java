package fr.arnaud.nexus.ui.inventory;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.item.weapon.component.PlayerWeaponStateComponent;
import fr.arnaud.nexus.item.weapon.data.WeaponBsonSchema;
import fr.arnaud.nexus.item.weapon.data.WeaponRarity;
import fr.arnaud.nexus.item.weapon.data.WeaponTag;
import org.bson.BsonDocument;

import javax.annotation.Nonnull;

/**
 * Stateless helper — builds and refreshes the #WeaponStatsPanel section
 * and the #WeaponTabBar selection indicator.
 */
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

        WeaponRarity rarity = WeaponBsonSchema.readRarity(doc);
        float damageMultiplier = WeaponBsonSchema.readDamageMultiplier(doc);

        String archetypeId = activeTab == WeaponTag.MELEE
            ? "Nexus_Weapon_Sword_Default"
            : "Nexus_Weapon_Staff_Default";

        cmd.set("#WeaponIcon.ItemId", archetypeId);
        cmd.set("#WeaponName.Text", archetypeId);
        cmd.set("#WeaponRarity.Text", capitalize(rarity.name()));
        cmd.set("#WeaponRarity.Style.TextColor", rarityColor(rarity));
        cmd.set("#WeaponDamageMultiplier.Text", String.format("%.2fx", damageMultiplier));
        cmd.set("#UpgradeButton.Disabled", rarity == WeaponRarity.LEGENDARY);
    }

    private static void renderEmpty(@Nonnull UICommandBuilder cmd) {
        cmd.setNull("#WeaponIcon.ItemId");
        cmd.set("#WeaponName.Text", "—");
        cmd.set("#WeaponRarity.Text", "—");
        cmd.set("#WeaponDamageMultiplier.Text", "—");
        cmd.set("#UpgradeButton.Disabled", true);
    }

    private static String rarityColor(WeaponRarity rarity) {
        return switch (rarity) {
            case COMMON -> "#9ca3af";
            case RARE -> "#60a5fa";
            case EPIC -> "#c084fc";
            case LEGENDARY -> "#fbbf24";
        };
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.charAt(0) + s.substring(1).toLowerCase();
    }
}
