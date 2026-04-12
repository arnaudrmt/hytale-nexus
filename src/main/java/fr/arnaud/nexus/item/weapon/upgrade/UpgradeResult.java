package fr.arnaud.nexus.item.weapon.upgrade;

public record UpgradeResult(boolean success, String failureReason, int essenceDustSpent) {

    public static UpgradeResult ok(int spent) {
        return new UpgradeResult(true, null, spent);
    }

    public static UpgradeResult fail(String reason) {
        return new UpgradeResult(false, reason, 0);
    }
}
