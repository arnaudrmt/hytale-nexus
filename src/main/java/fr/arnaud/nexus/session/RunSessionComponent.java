package fr.arnaud.nexus.session;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class RunSessionComponent implements Component<EntityStore> {

    public static final int KILL_BONUS = 50;
    public static final int DEATH_PENALTY = 100;

    private long accumulatedPlayMs;
    private long sessionStartMs;

    private long accumulatedCurrentLevelMs;
    private long currentLevelSessionStartMs;

    // Immutable splits appended each time a level is completed, in order.
    private List<Long> levelSplitMs = new ArrayList<>();

    private float totalDamageDealt;
    private float totalDamageTaken;
    private int killCount;
    private int deathCount;
    private float essenceDustSnapshot;
    private boolean tutorialCompleted;

    @Nullable
    private static ComponentType<EntityStore, RunSessionComponent> componentType;

    public RunSessionComponent() {
        long now = System.currentTimeMillis();
        sessionStartMs = now;
        currentLevelSessionStartMs = now;
    }

    private RunSessionComponent(long accumulatedPlayMs, long accumulatedCurrentLevelMs,
                                List<Long> levelSplitMs,
                                float totalDamageDealt, float totalDamageTaken,
                                int killCount, int deathCount,
                                float essenceDustSnapshot, boolean tutorialCompleted) {
        this.accumulatedPlayMs = accumulatedPlayMs;
        this.accumulatedCurrentLevelMs = accumulatedCurrentLevelMs;
        this.levelSplitMs = new ArrayList<>(levelSplitMs);
        this.totalDamageDealt = totalDamageDealt;
        this.totalDamageTaken = totalDamageTaken;
        this.killCount = killCount;
        this.deathCount = deathCount;
        long now = System.currentTimeMillis();
        sessionStartMs = now;
        currentLevelSessionStartMs = now;
        this.essenceDustSnapshot = essenceDustSnapshot;
        this.tutorialCompleted = tutorialCompleted;
    }

    public void pauseSession() {
        if (sessionStartMs == -1) return;
        long now = System.currentTimeMillis();
        accumulatedPlayMs += now - sessionStartMs;
        accumulatedCurrentLevelMs += now - currentLevelSessionStartMs;
        sessionStartMs = -1;
        currentLevelSessionStartMs = -1;
    }

    public void resumeSession() {
        long now = System.currentTimeMillis();
        sessionStartMs = now;
        currentLevelSessionStartMs = now;
    }

    // Finalizes the current level's duration, appends it to splits.
    public void recordLevelSplit() {
        long levelMs = getCurrentLevelDurationMs();
        levelSplitMs.add(levelMs);
        accumulatedCurrentLevelMs = 0;
        if (sessionStartMs != -1) {
            currentLevelSessionStartMs = System.currentTimeMillis();
        }
    }

    public int computeFinalScore() {
        return Math.max(0,
            killCount * KILL_BONUS
                - deathCount * DEATH_PENALTY
        );
    }

    public long getTotalDurationMs() {
        if (sessionStartMs == -1) return accumulatedPlayMs;
        return accumulatedPlayMs + (System.currentTimeMillis() - sessionStartMs);
    }

    public long getCurrentLevelDurationMs() {
        if (currentLevelSessionStartMs == -1) return accumulatedCurrentLevelMs;
        return accumulatedCurrentLevelMs + (System.currentTimeMillis() - currentLevelSessionStartMs);
    }

    public List<Long> getLevelSplits() {
        return Collections.unmodifiableList(levelSplitMs);
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

    public void incrementDeathCount() {
        deathCount++;
    }

    public void setEssenceDustSnapshot(float value) {
        essenceDustSnapshot = value;
    }

    public void markTutorialCompleted(boolean value) {
        this.tutorialCompleted = value;
    }

    public void markTutorialCompleted() {
        this.tutorialCompleted = true;
    }

    public int getKillCount() {
        return killCount;
    }

    public int getDeathCount() {
        return deathCount;
    }

    public float getTotalDamageDealt() {
        return totalDamageDealt;
    }

    public float getTotalDamageTaken() {
        return totalDamageTaken;
    }

    public float getEssenceDustSnapshot() {
        return essenceDustSnapshot;
    }

    public boolean isTutorialCompleted() {
        return tutorialCompleted;
    }

    public static final BuilderCodec<RunSessionComponent> CODEC = BuilderCodec
        .builder(RunSessionComponent.class, RunSessionComponent::new)
        .append(new KeyedCodec<>("AccumulatedPlayMs", Codec.LONG),
            (c, v) -> c.accumulatedPlayMs = v, c -> c.accumulatedPlayMs).add()
        .append(new KeyedCodec<>("AccumulatedCurrentLevelMs", Codec.LONG),
            (c, v) -> c.accumulatedCurrentLevelMs = v, c -> c.accumulatedCurrentLevelMs).add()
        .append(new KeyedCodec<>("LevelSplits", new ArrayCodec<>(Codec.LONG, Long[]::new)),
            (c, v) -> c.levelSplitMs = v != null ? new ArrayList<>(Arrays.asList(v)) : new ArrayList<>(),
            c -> c.levelSplitMs.toArray(new Long[0])).add()
        .append(new KeyedCodec<>("TotalDamageDealt", Codec.FLOAT),
            (c, v) -> c.totalDamageDealt = v, c -> c.totalDamageDealt).add()
        .append(new KeyedCodec<>("TotalDamageTaken", Codec.FLOAT),
            (c, v) -> c.totalDamageTaken = v, c -> c.totalDamageTaken).add()
        .append(new KeyedCodec<>("KillCount", Codec.INTEGER),
            (c, v) -> c.killCount = v, c -> c.killCount).add()
        .append(new KeyedCodec<>("DeathCount", Codec.INTEGER),
            (c, v) -> c.deathCount = v, c -> c.deathCount).add()
        .append(new KeyedCodec<>("EssenceDustSnapshot", Codec.FLOAT),
            (c, v) -> c.essenceDustSnapshot = v, c -> c.essenceDustSnapshot).add()
        .append(new KeyedCodec<>("TutorialCompleted", Codec.BOOLEAN),
            (c, v) -> c.tutorialCompleted = v, c -> c.tutorialCompleted).add()
        .build();

    @NonNullDecl
    public static ComponentType<EntityStore, RunSessionComponent> getComponentType() {
        if (componentType == null) throw new IllegalStateException("RunSessionComponent not registered.");
        return componentType;
    }

    public static void setComponentType(@Nullable ComponentType<EntityStore, RunSessionComponent> type) {
        componentType = type;
    }

    @Override
    @NonNullDecl
    public RunSessionComponent clone() {
        return new RunSessionComponent(
            accumulatedPlayMs, accumulatedCurrentLevelMs, levelSplitMs,
            totalDamageDealt, totalDamageTaken,
            killCount, deathCount, essenceDustSnapshot, tutorialCompleted);
    }
}
