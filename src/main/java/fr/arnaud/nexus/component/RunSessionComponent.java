package fr.arnaud.nexus.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;

/**
 * Tracks live run statistics for a player, used for score calculation.
 * <p>
 * Data includes kills, damage dealt/taken, and run-specific metrics.
 * See the <a href="https://github.com/YourRepo/Wiki/Scoring-Manual">Scoring Manual</a>
 * for detailed scoring formulas and game mode transition rules.
 */
public final class RunSessionComponent implements Component<EntityStore> {

    public static final int KILL_BONUS = 50;
    public static final int SWITCH_STRIKE_BONUS = 200;
    public static final int DEATH_PENALTY = 100;
    public static final float DREAM_DUST_DEATH_PENALTY_FRACTION = 0.25f;

    private long startTimeMs;
    private boolean hardMode;
    private float lucidityGained, totalDamageDealt, totalDamageTaken;
    private int switchStrikesUsed, killCount, deathCount;

    public RunSessionComponent() {
        resetRun();
        this.hardMode = false;
    }

    private RunSessionComponent(long startTimeMs, boolean hardMode, float lucidityGained,
                                float totalDamageDealt, float totalDamageTaken,
                                int switchStrikesUsed, int killCount, int deathCount) {
        this.startTimeMs = startTimeMs;
        this.hardMode = hardMode;
        this.lucidityGained = lucidityGained;
        this.totalDamageDealt = totalDamageDealt;
        this.totalDamageTaken = totalDamageTaken;
        this.switchStrikesUsed = switchStrikesUsed;
        this.killCount = killCount;
        this.deathCount = deathCount;
    }

    // --- Lifecycle ---

    public void resetRun() {
        startTimeMs = System.currentTimeMillis();
        lucidityGained = totalDamageDealt = totalDamageTaken = 0f;
        switchStrikesUsed = killCount = deathCount = 0;
    }

    public void startRun(boolean hardMode) {
        this.hardMode = hardMode;
        resetRun();
    }

    // --- Mutations ---

    public void addLucidityGained(float amount) {
        lucidityGained += Math.max(0f, amount);
    }

    public void addDamageDealt(float amount) {
        totalDamageDealt += Math.max(0f, amount);
    }

    public void addDamageTaken(float amount) {
        totalDamageTaken += Math.max(0f, amount);
    }

    public void incrementKillCount() {
        killCount++;
    }

    public void incrementSwitchStrikes() {
        switchStrikesUsed++;
    }

    public void incrementDeathCount() {
        deathCount++;
    }

    // --- Queries ---

    public int computeFinalScore() {
        return Math.max(0,
            (int) lucidityGained
                + killCount * KILL_BONUS
                + switchStrikesUsed * SWITCH_STRIKE_BONUS
                - deathCount * DEATH_PENALTY
        );
    }

    public long getRunDurationMs() {
        return System.currentTimeMillis() - startTimeMs;
    }

    // --- ECS Boilerplate ---

    @Nullable
    private static ComponentType<EntityStore, RunSessionComponent> componentType;

    @NonNullDecl
    public static ComponentType<EntityStore, RunSessionComponent> getComponentType() {
        if (componentType == null) {
            throw new IllegalStateException("RunSessionComponent not yet registered.");
        }
        return componentType;
    }

    public static void setComponentType(
        @Nullable ComponentType<EntityStore, RunSessionComponent> type) {
        componentType = type;
    }

    @Override
    @NonNullDecl
    public RunSessionComponent clone() {
        return new RunSessionComponent(
            startTimeMs, hardMode, lucidityGained,
            totalDamageDealt, totalDamageTaken,
            switchStrikesUsed, killCount, deathCount);
    }
}
