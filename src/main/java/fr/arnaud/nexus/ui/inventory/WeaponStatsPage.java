package fr.arnaud.nexus.ui.inventory;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.feature.resource.PlayerStatsManager;
import fr.arnaud.nexus.item.weapon.component.PlayerWeaponStateComponent;
import fr.arnaud.nexus.item.weapon.data.WeaponBsonSchema;
import fr.arnaud.nexus.item.weapon.data.WeaponTag;
import fr.arnaud.nexus.item.weapon.stats.WeaponStatCalculator;
import fr.arnaud.nexus.item.weapon.stats.WeaponStatRegistry;
import fr.arnaud.nexus.util.FormatUtil;
import org.bson.BsonDocument;

import javax.annotation.Nonnull;

public final class WeaponStatsPage {

    private WeaponStatsPage() {
    }

    static void populate(@Nonnull UICommandBuilder cmd, @Nonnull Ref<EntityStore> ref,
                         @Nonnull Store<EntityStore> store, @Nonnull WeaponTag activeTab) {

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
        int quality = WeaponBsonSchema.readQualityValue(doc);
        String name = WeaponBsonSchema.readName(doc);

        cmd.set("#WeaponIcon.ItemId", archetypeId);
        cmd.set("#WeaponLevel.Text", "[Lvl. " + level + "]");
        cmd.set("#WeaponName.Text", Message.translation(name));

        String qualityColor = QualityMapper.toColor(quality);
        Message qualityName = Message.translation(QualityMapper.toNameKey(quality));

        cmd.set("#WeaponRarity.Text", qualityName);
        cmd.set("#WeaponRarity.Style.TextColor", qualityColor);

        if (!WeaponStatRegistry.getInstance().isReady()) {
            renderEmpty(cmd);
            return;
        }

        WeaponStatCalculator.PassiveStats current = WeaponStatCalculator.calculatePassiveStats(doc, level);
        cmd.set("#WeaponDamageMultiplier.Text", FormatUtil.formatPercentModifier(current.damageMultiplier()));
        cmd.set("#WeaponHealthAdder.Text", FormatUtil.formatSignedSmart(current.healthBonus()));
        cmd.set("#WeaponMovementSpeedAdder.Text", FormatUtil.formatSignedSmart(current.movementSpeedBonus()));

        PlayerStatsManager statsManager = Nexus.getInstance().getPlayerStatsManager();
        float balance = statsManager.isReady()
            ? statsManager.getEssenceDust(ref, store) : 0f;
        float cost = WeaponStatCalculator.calculateUpgradeCost(doc);
        boolean canAfford = statsManager.isReady() && balance >= cost;

        cmd.set("#UpgradeCost.Text", FormatUtil.formatGroupedInteger(cost));
        cmd.set("#UpgradeButton.Disabled", !canAfford);
        cmd.set("#UpgradeButton.TooltipText", buildWeaponUpgradeTooltip(doc, level));
    }

    private static String buildWeaponUpgradeTooltip(@Nonnull BsonDocument doc,
                                                    int currentLevel) {
        int nextLevel = currentLevel + 1;
        WeaponStatCalculator.PassiveStats current = WeaponStatCalculator.calculatePassiveStats(doc, currentLevel);
        WeaponStatCalculator.PassiveStats next = WeaponStatCalculator.calculatePassiveStats(doc, nextLevel);

        double dmgDelta = next.damageMultiplier() - current.damageMultiplier();
        double healthDelta = next.healthBonus() - current.healthBonus();
        double speedDelta = next.movementSpeedBonus() - current.movementSpeedBonus();

        return "Damage Multiplier" + ": " + FormatUtil.formatChange(dmgDelta, true) + "\n"
            + "Health Boost" + " :" + FormatUtil.formatChange(healthDelta, false) + "\n"
            + "Movement Speed" + ": " + FormatUtil.formatChange(speedDelta, false);
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
}
