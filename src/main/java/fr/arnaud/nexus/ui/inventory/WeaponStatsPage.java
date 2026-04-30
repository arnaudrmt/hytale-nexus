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
import fr.arnaud.nexus.item.weapon.stats.WeaponStatBag;
import fr.arnaud.nexus.item.weapon.stats.WeaponStatBagBuilder;
import fr.arnaud.nexus.item.weapon.stats.WeaponStatCalculator;
import fr.arnaud.nexus.item.weapon.stats.WeaponStatRegistry;
import fr.arnaud.nexus.util.FormatUtil;
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

        WeaponStatBag current = WeaponStatBagBuilder.buildWeaponStatsFromBson(doc, level);
        cmd.set("#WeaponDamageMultiplier.Text", FormatUtil.formatDamage(current.damageMultiplier));
        cmd.set("#WeaponHealthAdder.Text", FormatUtil.formatFlat(current.healthBoost));
        cmd.set("#WeaponMovementSpeedAdder.Text", FormatUtil.formatFlat(current.movementSpeedBoost));

        PlayerStatsManager statsManager = Nexus.get().getPlayerStatsManager();
        float balance = statsManager.isReady()
            ? statsManager.getEssenceDust(ref, store) : 0f;
        float cost = WeaponStatCalculator.calculateUpgradeCost(doc);
        boolean canAfford = statsManager.isReady() && balance >= cost;

        cmd.set("#UpgradeCost.Text", FormatUtil.formatCost(cost));
        cmd.set("#UpgradeButton.Disabled", !canAfford);
        cmd.set("#UpgradeButton.TooltipText", buildWeaponUpgradeTooltip(doc, level));
    }

    private static String buildWeaponUpgradeTooltip(@Nonnull BsonDocument doc,
                                                    int currentLevel) {
        int nextLevel = currentLevel + 1;
        WeaponStatBag current = WeaponStatBagBuilder.buildWeaponStatsFromBson(doc, currentLevel);
        WeaponStatBag next = WeaponStatBagBuilder.buildWeaponStatsFromBson(doc, nextLevel);

        double dmgDelta = next.damageMultiplier - current.damageMultiplier;
        double healthDelta = next.healthBoost - current.healthBoost;
        double speedDelta = next.movementSpeedBoost - current.movementSpeedBoost;

        return Message.translation("nexus.damage_multiplier") + ": " + FormatUtil.formatDelta(dmgDelta, true) + "\n"
            + Message.translation("nexus.health_boost") + " " + FormatUtil.formatDelta(healthDelta, false) + "\n"
            + Message.translation("nexus.movement_speed") + ": " + FormatUtil.formatDelta(speedDelta, false);
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
