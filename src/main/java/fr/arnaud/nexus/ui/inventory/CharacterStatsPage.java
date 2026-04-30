package fr.arnaud.nexus.ui.inventory;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.feature.resource.PlayerStatsManager;
import fr.arnaud.nexus.util.FormatUtil;

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
        cmd.set("#StatsHealth.Text", Math.round(health) + " / " + Math.round(maxHealth));

        float stamina = stats.getStamina(ref, store);
        float maxStamina = stats.getMaxStamina(ref, store);
        cmd.set("#StatsStamina.Text", FormatUtil.formatDecimal(stamina) + " / " + Math.round(maxStamina));

        float essence = stats.getEssenceDust(ref, store);
        cmd.set("#StatsEssence.Text", Math.round(essence));

        float speed = stats.getMovementSpeed(ref, store);
        cmd.set("#StatsMovementSpeed.Text", String.format("%.1f", speed));
    }
}
