package fr.arnaud.nexus.ui.inventory;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.feature.ressource.PlayerStatsManager;

import javax.annotation.Nonnull;

public final class CharacterStatsPage {

    private CharacterStatsPage() {
    }

    /**
     * Populates #StatsHealth, #StatsStamina, #StatsEssence, #StatsMovementSpeed.
     * Called from NexusInventoryPage.build() and after any event that mutates stats.
     */
    static void populate(@Nonnull UICommandBuilder cmd,
                         @Nonnull Ref<EntityStore> ref,
                         @Nonnull Store<EntityStore> store) {

        PlayerStatsManager stats = Nexus.get().getPlayerStatsManager();

        if (!stats.isReady()) {
            cmd.set("#StatsHealth.Text", "—");
            cmd.set("#StatsStamina.Text", "—");
            cmd.set("#StatsEssence.Text", "—");
            cmd.set("#StatsMovementSpeed.Text", "—");
            return;
        }

        // Health: current / max e.g. "87 / 100"
        float health = stats.getHealth(ref, store);
        float maxHealth = stats.getMaxHealth(ref, store);
        cmd.set("#StatsHealth.Text", formatWhole(health) + " / " + formatWhole(maxHealth));

        // Stamina: current / max e.g. "7.5 / 10"
        float stamina = stats.getStamina(ref, store);
        float maxStamina = stats.getMaxStamina(ref, store);
        cmd.set("#StatsStamina.Text", formatDecimal(stamina) + " / " + formatWhole(maxStamina));

        // Essence dust: whole number
        float essence = stats.getEssenceDust(ref, store);
        cmd.set("#StatsEssence.Text", formatWhole(essence));

        // Movement speed: one decimal e.g. "5.5"
        float speed = stats.getMovementSpeed(ref, store);
        cmd.set("#StatsMovementSpeed.Text", String.format("%.1f", speed));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Rounds to nearest integer for display.
     */
    private static String formatWhole(float value) {
        return String.valueOf(Math.round(value));
    }

    /**
     * One decimal place, dropping .0 if whole.
     */
    private static String formatDecimal(float value) {
        if (value == Math.floor(value) && !Float.isInfinite(value))
            return String.valueOf((int) value);
        return String.format("%.1f", value);
    }
}
