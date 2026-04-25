package fr.arnaud.nexus.ui.inventory;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.feature.ressource.PlayerStatsManager;
import fr.arnaud.nexus.item.weapon.component.PlayerWeaponStateComponent;
import fr.arnaud.nexus.item.weapon.data.WeaponBsonSchema;
import fr.arnaud.nexus.item.weapon.data.WeaponTag;
import fr.arnaud.nexus.item.weapon.level.WeaponConfigCalculator;
import fr.arnaud.nexus.item.weapon.stats.WeaponStatBag;
import fr.arnaud.nexus.item.weapon.stats.WeaponStatBagBuilder;
import fr.arnaud.nexus.item.weapon.stats.WeaponStatRegistry;
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

        BsonDocument doc = activeTab == WeaponTag.RANGED
            ? state.rangedDocument : state.meleeDocument;

        if (doc == null || !doc.containsKey("archetype_id")) {
            renderEmpty(cmd);
            return;
        }

        String archetypeId = doc.getString("archetype_id").getValue();
        int level = WeaponBsonSchema.readLevel(doc);
        int quality = WeaponBsonSchema.readQuality(doc);
        String name = WeaponBsonSchema.readName(doc);

        cmd.set("#WeaponIcon.ItemId", archetypeId);
        cmd.set("#WeaponLevel.Text", "[Lvl. " + level + "]");
        cmd.set("#WeaponName.Text", Message.translation(name));

        String qualityColor = QualityMapper.toColor(quality);
        String qualityName = QualityMapper.toName(quality);
        cmd.set("#WeaponRarity.Text", qualityName);
        cmd.set("#WeaponRarity.Style.TextColor", qualityColor);

        if (!WeaponStatRegistry.get().isReady()) {
            renderEmpty(cmd);
            return;
        }

        WeaponStatBag current = WeaponStatBagBuilder.buildFromBson(doc, level);
        cmd.set("#WeaponDamageMultiplier.Text", formatDamage(current.damageMultiplier));
        cmd.set("#WeaponHealthAdder.Text", formatFlat(current.healthBoost));
        cmd.set("#WeaponMovementSpeedAdder.Text", formatFlat(current.movementSpeedBoost));

        PlayerStatsManager statsManager = Nexus.get().getPlayerStatsManager();
        float balance = statsManager.isReady()
            ? statsManager.getEssenceDust(ref, store) : 0f;
        float cost = WeaponConfigCalculator.calculateUpgradeCost(doc);
        boolean canAfford = statsManager.isReady() && balance >= cost;

        cmd.set("#UpgradeCost.Text", formatCost(cost));
        cmd.set("#UpgradeButton.Disabled", !canAfford);
        cmd.set("#UpgradeButton.TooltipText", buildWeaponUpgradeTooltip(doc, level, cost));
    }

    private static String buildWeaponUpgradeTooltip(@Nonnull BsonDocument doc,
                                                    int currentLevel,
                                                    float cost) {
        int nextLevel = currentLevel + 1;
        WeaponStatBag current = WeaponStatBagBuilder.buildFromBson(doc, currentLevel);
        WeaponStatBag next = WeaponStatBagBuilder.buildFromBson(doc, nextLevel);

        double dmgDelta = next.damageMultiplier - current.damageMultiplier;
        double healthDelta = next.healthBoost - current.healthBoost;
        double speedDelta = next.movementSpeedBoost - current.movementSpeedBoost;

        return "Damage Multiplier: " + formatDelta(dmgDelta, true) + "\n"
            + "Health Boost: " + formatDelta(healthDelta, false) + "\n"
            + "Movement Speed: " + formatDelta(speedDelta, false);
    }

    private static void renderEmpty(@Nonnull UICommandBuilder cmd) {
        cmd.setNull("#WeaponIcon.ItemId");
        cmd.set("#WeaponLevel.Text", "");
        cmd.set("#WeaponName.Text", "—");
        cmd.set("#WeaponRarity.Text", "—");
        cmd.set("#WeaponRarity.Style", "(TextColor: #878e9c, FontSize: 13)");
        cmd.set("#WeaponDamageMultiplier.Text", "—");
        cmd.set("#WeaponHealthAdder.Text", "—");
        cmd.set("#WeaponMovementSpeedAdder.Text", "—");
        cmd.set("#UpgradeButton.Disabled", true);
    }

    private static String formatDamage(double multiplier) {
        double pct = (multiplier - 1.0) * 100.0;
        if (pct == 0) return "0%";
        return (pct > 0 ? "+" : "") + Math.round(pct) + "%";
    }

    private static String formatFlat(double value) {
        if (value == 0) return "0";
        if (value == Math.floor(value))
            return (value > 0 ? "+" : "") + (int) value;
        return String.format("%+.1f", value);
    }

    private static String formatDelta(double delta, boolean isMultiplier) {
        if (isMultiplier) {
            return String.format("%+.1f%%", delta * 100.0);
        }
        if (delta == Math.floor(delta))
            return String.format("%+d", (int) delta);
        return String.format("%+.1f", delta);
    }

    private static String formatCost(float cost) {
        int c = Math.round(cost);
        if (c < 1000) return String.valueOf(c);
        return (c / 1000) + " " + String.format("%03d", c % 1000);
    }
}
