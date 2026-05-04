package fr.arnaud.nexus.item.weapon.level;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.feature.resource.PlayerStatsManager;
import fr.arnaud.nexus.item.weapon.data.WeaponBsonSchema;
import fr.arnaud.nexus.item.weapon.stats.WeaponStatCalculator;
import org.bson.BsonDocument;

public final class WeaponUpgradeService {

    private final PlayerStatsManager playerStatsManager;

    public WeaponUpgradeService(PlayerStatsManager playerStatsManager) {
        this.playerStatsManager = playerStatsManager;
    }

    public UpgradeResult attemptUpgrade(Ref<EntityStore> playerRef, BsonDocument weaponDoc, Store<EntityStore> store) {
        if (!playerStatsManager.isReady()) {
            return UpgradeResult.failure("Essence system not initialized");
        }

        float upgradeCost = WeaponStatCalculator.calculateUpgradeCost(weaponDoc);
        float playerBalance = playerStatsManager.getEssenceDust(playerRef, store);

        if (playerBalance < upgradeCost) {
            return UpgradeResult.insufficientFunds(upgradeCost, playerBalance);
        }

        int currentLevel = WeaponBsonSchema.readLevel(weaponDoc);
        WeaponBsonSchema.writeLevel(weaponDoc, currentLevel + 1);
        playerStatsManager.removeEssenceDust(playerRef, store, upgradeCost);

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

        public static UpgradeResult insufficientFunds(float required, float balance) {
            return new UpgradeResult(false, 0, 0f,
                Message.translation("nexus.insufficient.essence").getRawText(),
                required, balance);
        }
    }
}
