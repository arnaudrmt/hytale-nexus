package fr.arnaud.nexus.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap.Predictable;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.component.SwitchStrikeComponent;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.jetbrains.annotations.NotNull;

/**
 * Watches for the {@code SwitchStrike_Trigger} hidden stat
 * (bumped to 1 by the Razorstrike JSON ability on hitting an enemy) and opens
 * the Switch Strike confirmation window on the attacking player.
 * <p>
 * The stat lands on the <em>hit entity</em> (the enemy), not the attacker —
 * the window is therefore opened here by reading the player's own copy of the
 * stat. The Razorstrike JSON places {@code EntityStatsOnHit} with target
 * {@code SwitchStrike_Trigger} specifically so this system can detect it on
 * any entity that just got hit, but in practice only the attacker's player
 * entity will have this stat defined in their {@code EntityStatMap}.
 * <p>
 * The stat is reset to 0 immediately after detection to prevent re-triggering.
 */
public final class SwitchStrikeTriggerSystem extends EntityTickingSystem<EntityStore> {

    private final int switchStrikeTriggerIndex;

    public SwitchStrikeTriggerSystem(int switchStrikeTriggerIndex) {
        this.switchStrikeTriggerIndex = switchStrikeTriggerIndex;
    }

    @NonNullDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
            PlayerRef.getComponentType(),
            EntityStatMap.getComponentType(),
            SwitchStrikeComponent.getComponentType()
        );
    }

    @Override
    public void tick(float deltaSeconds, int index, ArchetypeChunk<EntityStore> chunk,
                     @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> commandBuffer) {

        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        EntityStatMap stats = chunk.getComponent(index, EntityStatMap.getComponentType());
        SwitchStrikeComponent switchStrike = chunk.getComponent(index, SwitchStrikeComponent.getComponentType());

        if (stats == null || switchStrike == null) return;

        switchStrike.tick(deltaSeconds);

        EntityStatValue trigger = stats.get(switchStrikeTriggerIndex);
        if (trigger != null && trigger.get() > 0f) {
            switchStrike.openWindow(SwitchStrikeComponent.WINDOW_DURATION_SEC);
            stats.subtractStatValue(Predictable.ALL, switchStrikeTriggerIndex, trigger.get());

            commandBuffer.run(s -> {
                s.putComponent(ref, EntityStatMap.getComponentType(), stats);
                s.putComponent(ref, SwitchStrikeComponent.getComponentType(), switchStrike);
            });
            return;
        }

        commandBuffer.run(s -> s.putComponent(ref, SwitchStrikeComponent.getComponentType(), switchStrike));
    }
}
