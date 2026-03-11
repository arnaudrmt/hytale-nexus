package fr.arnaud.nexus.component;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;

/**
 * Tracks live run statistics for a player, used for score calculation.
 * <p>
 * Persisted per-world so that logging back in resumes the run exactly where
 * the player left off. A fresh world instance naturally resets all values
 * via a new {@link EntityStore}, so no explicit reset is required.
 * <p>
 * See the scoring manual wiki for formula details.
 */
public final class RunSessionComponent implements Component<EntityStore> {

    public static final int KILL_BONUS = 50;
    public static final int SWITCH_STRIKE_BONUS = 200;
    public static final int DEATH_PENALTY = 100;

    private long startTimeMs;
    private float lucidityGained;
    private float totalDamageDealt;
    private float totalDamageTaken;
    private int switchStrikesUsed;
    private int killCount;
    private int deathCount;

    public RunSessionComponent() {
        startTimeMs = System.currentTimeMillis();
    }

    private RunSessionComponent(long startTimeMs, float lucidityGained,
                                float totalDamageDealt, float totalDamageTaken,
                                int switchStrikesUsed, int killCount, int deathCount) {
        this.startTimeMs = startTimeMs;
        this.lucidityGained = lucidityGained;
        this.totalDamageDealt = totalDamageDealt;
        this.totalDamageTaken = totalDamageTaken;
        this.switchStrikesUsed = switchStrikesUsed;
        this.killCount = killCount;
        this.deathCount = deathCount;
    }

    public static final BuilderCodec<RunSessionComponent> CODEC = BuilderCodec
        .builder(RunSessionComponent.class, RunSessionComponent::new)
        .append(
            new KeyedCodec<>("StartTimeMs", Codec.LONG),
            (c, v) -> c.startTimeMs = v,
            c -> c.startTimeMs
        ).add()
        .append(
            new KeyedCodec<>("LucidityGained", Codec.FLOAT),
            (c, v) -> c.lucidityGained = v,
            c -> c.lucidityGained
        ).add()
        .append(
            new KeyedCodec<>("TotalDamageDealt", Codec.FLOAT),
            (c, v) -> c.totalDamageDealt = v,
            c -> c.totalDamageDealt
        ).add()
        .append(
            new KeyedCodec<>("TotalDamageTaken", Codec.FLOAT),
            (c, v) -> c.totalDamageTaken = v,
            c -> c.totalDamageTaken
        ).add()
        .append(
            new KeyedCodec<>("SwitchStrikesUsed", Codec.INTEGER),
            (c, v) -> c.switchStrikesUsed = v,
            c -> c.switchStrikesUsed
        ).add()
        .append(
            new KeyedCodec<>("KillCount", Codec.INTEGER),
            (c, v) -> c.killCount = v,
            c -> c.killCount
        ).add()
        .append(
            new KeyedCodec<>("DeathCount", Codec.INTEGER),
            (c, v) -> c.deathCount = v,
            c -> c.deathCount
        ).add()
        .build();

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

    public int getKillCount() {
        return killCount;
    }

    public int getDeathCount() {
        return deathCount;
    }

    public int getSwitchStrikesUsed() {
        return switchStrikesUsed;
    }

    public float getLucidityGained() {
        return lucidityGained;
    }

    public float getTotalDamageDealt() {
        return totalDamageDealt;
    }

    public float getTotalDamageTaken() {
        return totalDamageTaken;
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
            startTimeMs, lucidityGained,
            totalDamageDealt, totalDamageTaken,
            switchStrikesUsed, killCount, deathCount);
    }
}
