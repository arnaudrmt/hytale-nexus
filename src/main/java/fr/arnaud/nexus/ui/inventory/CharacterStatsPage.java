package fr.arnaud.nexus.ui.inventory;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.feature.resource.PlayerStatsManager;

import javax.annotation.Nonnull;

public final class CharacterStatsPage {

    private CharacterStatsPage() {
    }

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

        float health = stats.getHealth(ref, store);
        float maxHealth = stats.getMaxHealth(ref, store);
        cmd.set("#StatsHealth.Text", formatWhole(health) + " / " + formatWhole(maxHealth));

        float stamina = stats.getStamina(ref, store);
        float maxStamina = stats.getMaxStamina(ref, store);
        cmd.set("#StatsStamina.Text", formatDecimal(stamina) + " / " + formatWhole(maxStamina));

        float essence = stats.getEssenceDust(ref, store);
        cmd.set("#StatsEssence.Text", formatWhole(essence));

        float speed = stats.getMovementSpeed(ref, store);
        cmd.set("#StatsMovementSpeed.Text", String.format("%.1f", speed));
    }

    private static String formatWhole(float value) {
        return String.valueOf(Math.round(value));
    }

    private static String formatDecimal(float value) {
        if (value == Math.floor(value) && !Float.isInfinite(value))
            return String.valueOf((int) value);
        return String.format("%.1f", value);
    }
}
