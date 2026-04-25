package fr.arnaud.nexus.level;

import com.hypixel.hytale.server.core.universe.world.WorldConfig;

import java.time.Instant;

public final class NexusWorldSetup {

    private NexusWorldSetup() {
    }

    public static void apply(WorldConfig worldConfig) {
        worldConfig.setGameTimePaused(true);
        worldConfig.setGameTime(Instant.MAX);
        worldConfig.setForcedWeather("Clear");
        worldConfig.setBlockTicking(false);
        worldConfig.setSpawningNPC(false);
    }
}
