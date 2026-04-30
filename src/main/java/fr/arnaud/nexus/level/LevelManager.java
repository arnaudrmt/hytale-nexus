package fr.arnaud.nexus.level;

import java.util.List;

public final class LevelManager {

    private LevelConfig currentLevel;

    public LevelManager() {
    }

    public boolean loadLevel(String levelId) {
        LevelConfig config = LevelConfigLoader.loadAndParseLevelConfig(levelId);
        if (config == null) return false;
        this.currentLevel = config;
        return true;
    }

    public void setCurrentLevel(LevelConfig config) {
        this.currentLevel = config;
    }

    public boolean isLevelLoaded() {
        return currentLevel != null;
    }

    public void unloadLevel() {
        this.currentLevel = null;
    }

    public String getLevelId() {
        return getCurrentLevel().getId();
    }

    public String getLevelName() {
        return getCurrentLevel().getName();
    }

    public float getDifficulty() {
        return getCurrentLevel().getDifficulty();
    }

    public LevelConfig.Position getSpawnPoint() {
        return getCurrentLevel().getSpawnPoint();
    }

    public LevelConfig.Position getFinishPoint() {
        return getCurrentLevel().getFinishPoint();
    }

    public List<LevelConfig.SpawnerConfig> getSpawners() {
        return getCurrentLevel().getSpawners();
    }

    public LevelConfig getCurrentConfig() {
        return getCurrentLevel();
    }

    private LevelConfig getCurrentLevel() {
        if (currentLevel == null) {
            throw new IllegalStateException("LevelManager: no level is currently loaded.");
        }
        return currentLevel;
    }
}
