package fr.arnaud.nexus.level;

import fr.arnaud.nexus.core.Nexus;

import java.util.List;

/**
 * Central authority for the current level's state.
 *
 * <p>All runtime queries go through this manager without specifying a level —
 * it always answers for the level currently loaded. Call {@link #loadLevel(String)}
 * at the start of each run to switch the active config.
 *
 * <p>Instantiated once in {@link Nexus} and accessible via
 * {@link Nexus#getLevelManager()}.
 */
public final class LevelManager {

    private LevelConfig currentLevel;

    public LevelManager() {
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Loads a level config by ID and makes it the active level.
     *
     * @param levelId the level identifier matching a file in {@code resources/levels/}
     * @return {@code true} if the config was found and loaded successfully
     */
    public boolean loadLevel(String levelId) {
        LevelConfig config = LevelConfigLoader.load(levelId);
        if (config == null) return false;
        this.currentLevel = config;
        return true;
    }

    /**
     * Directly sets the active config. Useful if you parsed the config yourself
     * (e.g. inside {@link NexusWorldLoadSystem}) and want to hand it over.
     */
    public void setCurrentLevel(LevelConfig config) {
        this.currentLevel = config;
    }

    /**
     * Returns {@code true} if a level is currently loaded.
     */
    public boolean isLevelLoaded() {
        return currentLevel != null;
    }

    /**
     * Clears the current level. Call on run-end or player death.
     */
    public void unloadLevel() {
        this.currentLevel = null;
    }

    // -------------------------------------------------------------------------
    // Getters — always for the current level, no level ID needed
    // -------------------------------------------------------------------------

    public String getLevelId() {
        return require().getId();
    }

    public String getLevelName() {
        return require().getName();
    }

    public float getDifficulty() {
        return require().getDifficulty();
    }

    public LevelConfig.Position getSpawnPoint() {
        return require().getSpawnPoint();
    }

    public LevelConfig.Position getFinishPoint() {
        return require().getFinishPoint();
    }

    public List<LevelConfig.SpawnerConfig> getSpawners() {
        return require().getSpawners();
    }

    public LevelConfig getCurrentConfig() {
        return require();
    }

    // -------------------------------------------------------------------------

    private LevelConfig require() {
        if (currentLevel == null) {
            throw new IllegalStateException("LevelManager: no level is currently loaded.");
        }
        return currentLevel;
    }
}
