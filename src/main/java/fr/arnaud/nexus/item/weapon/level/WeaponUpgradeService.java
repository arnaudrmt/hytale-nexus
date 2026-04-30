package fr.arnaud.nexus.item.weapon.level;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.feature.resource.PlayerStatsManager;
import fr.arnaud.nexus.item.weapon.data.WeaponBsonSchema;
import fr.arnaud.nexus.item.weapon.stats.WeaponStatCalculator;
import fr.arnaud.nexus.util.MessageUtil;
import org.bson.BsonDocument;

public final class WeaponUpgradeService {

    private final PlayerStatsManager statsManager;

    public WeaponUpgradeService(PlayerStatsManager playerStatsManager) {
        this.statsManager = playerStatsManager;
    }

    public UpgradeResult attemptUpgrade(Ref<EntityStore> playerRef, BsonDocument weaponDoc, Store<EntityStore> store) {
        if (!statsManager.isReady()) {
            return UpgradeResult.failure(MessageUtil.componentNotRegistered("Essence system"));
        }

        float upgradeCost = WeaponStatCalculator.calculateUpgradeCost(weaponDoc);
        float playerBalance = statsManager.getEssenceDust(playerRef, store);

        if (playerBalance < upgradeCost) {
            return UpgradeResult.failure(Message.translation("nexus.insufficient.essence").getRawText(), upgradeCost, playerBalance);
        }

        int currentLevel = WeaponBsonSchema.readLevel(weaponDoc);
        WeaponBsonSchema.writeLevel(weaponDoc, currentLevel + 1);
        statsManager.removeEssenceDust(playerRef, store, upgradeCost);

        return UpgradeResult.success(currentLevel + 1, upgradeCost);
    }

    public record UpgradeResult(
        boolean success,
        int newLevel,
        float essenceSpent,
        String failureReason,
        float requiredEssence,
        float currentBalance
    ) {
        public static UpgradeResult success(int newLevel, float spent) {
            return new UpgradeResult(true, newLevel, spent, null, 0f, 0f);
        }

        public static UpgradeResult failure(String reason) {
            return new UpgradeResult(false, 0, 0f, reason, 0f, 0f);
        }

        public static UpgradeResult failure(String reason, float required, float balance) {
            return new UpgradeResult(false, 0, 0f, reason, required, balance);
        }
    }
}
