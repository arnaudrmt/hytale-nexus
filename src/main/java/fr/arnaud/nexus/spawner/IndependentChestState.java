package fr.arnaud.nexus.spawner;

import com.hypixel.hytale.math.vector.Vector3d;
import fr.arnaud.nexus.level.LevelConfig;

import javax.annotation.Nullable;
import java.util.List;

public final class IndependentChestState {

    private final int id;
    private final LevelConfig.IndependentChestConfig config;

    private boolean triggered = false;
    private boolean chestSpawned = false;
    @Nullable
    private List<String> pendingLoot = null;
    @Nullable
    private Vector3d chestPosition = null;

    public IndependentChestState(int id, LevelConfig.IndependentChestConfig config) {
        this.id = id;
        this.config = config;
    }

    public int getId() {
        return id;
    }

    public LevelConfig.IndependentChestConfig getConfig() {
        return config;
    }

    public boolean isTriggered() {
        return triggered;
    }

    public void markTriggered() {
        triggered = true;
    }

    public boolean isChestSpawned() {
        return chestSpawned;
    }

    public void markChestSpawned() {
        chestSpawned = true;
    }

    public boolean hasPendingLoot() {
        return pendingLoot != null && !pendingLoot.isEmpty();
    }

    @Nullable
    public List<String> getPendingLoot() {
        return pendingLoot;
    }

    public void setPendingLoot(@Nullable List<String> loot) {
        this.pendingLoot = loot;
    }

    public void clearPendingLoot() {
        this.pendingLoot = null;
    }

    @Nullable
    public Vector3d getChestPosition() {
        return chestPosition;
    }

    public void setChestPosition(@Nullable Vector3d pos) {
        this.chestPosition = pos;
    }
}
