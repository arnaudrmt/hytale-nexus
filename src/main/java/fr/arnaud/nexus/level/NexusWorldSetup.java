package fr.arnaud.nexus.level;

import com.hypixel.hytale.server.core.universe.world.WorldConfig;

public final class NexusWorldSetup {

    private NexusWorldSetup() {}

    public static void apply(WorldConfig worldConfig) {
        worldConfig.setGameTimePaused(true);
        worldConfig.setForcedWeather("Clear");
        worldConfig.setBlockTicking(false);
        worldConfig.setSpawningNPC(false);
    }
}
