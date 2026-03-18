package fr.arnaud.nexus.system;

import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap.Predictable;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.component.SwitchStrikeComponent;
import fr.arnaud.nexus.component.SwitchStrikeComponent.State;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Watches the {@code SwitchStrike_Trigger} hidden stat on every player.
 * When the JSON writes to it via {@code EntityStatsOnHit Target:Self} on
 * an Ability1 hit, this system opens the Switch Strike window and consumes
 * the trigger immediately so it cannot re-fire on the same tick.
 */
public final class SwitchStrikeTriggerSystem extends EntityTickingSystem<EntityStore> {

    private static final Logger LOGGER = Logger.getLogger(SwitchStrikeTriggerSystem.class.getName());

    private int triggerStatIndex = Integer.MIN_VALUE;

    public void onAssetsLoaded(
        LoadedAssetsEvent<String, EntityStatType,
            IndexedLookupTableAssetMap<String, EntityStatType>> event) {
        triggerStatIndex = event.getAssetMap().getIndex("SwitchStrike_Trigger");

        if (triggerStatIndex != Integer.MIN_VALUE) {
            LOGGER.log(Level.INFO,
                "[SwitchStrike] STEP 0 — SwitchStrike_Trigger stat resolved. Index: " + triggerStatIndex);
        } else {
            LOGGER.log(Level.SEVERE,
                "[SwitchStrike] STEP 0 FAIL — SwitchStrike_Trigger stat not found in Asset registry. " +
                    "The system will not function. Check that SwitchStrike_Trigger.json exists in the Asset Editor.");
        }
    }

    @NonNullDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
            PlayerRef.getComponentType(),
            SwitchStrikeComponent.getComponentType(),
            EntityStatMap.getComponentType()
        );
    }

    @Override
    public void tick(float deltaSeconds, int index, ArchetypeChunk<EntityStore> chunk,
                     @NonNullDecl Store<EntityStore> store,
                     @NonNullDecl CommandBuffer<EntityStore> cmd) {
        if (triggerStatIndex == Integer.MIN_VALUE) return;

        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        SwitchStrikeComponent switchStrike =
            chunk.getComponent(index, SwitchStrikeComponent.getComponentType());
        EntityStatMap stats = chunk.getComponent(index, EntityStatMap.getComponentType());

        if (switchStrike == null || stats == null) return;

        if (switchStrike.getState() == State.IDLE) {
            tryOpenWindow(ref, switchStrike, stats, cmd);
        }

        cmd.run(s -> s.putComponent(ref, SwitchStrikeComponent.getComponentType(), switchStrike));
    }

    private void tryOpenWindow(@NonNullDecl Ref<EntityStore> ref,
                               @NonNullDecl SwitchStrikeComponent switchStrike,
                               @NonNullDecl EntityStatMap stats,
                               @NonNullDecl CommandBuffer<EntityStore> cmd) {
        EntityStatValue trigger = stats.get(triggerStatIndex);
        if (trigger == null || trigger.get() <= 0f) return;

        LOGGER.log(Level.INFO,
            "[SwitchStrike] STEP 1 — Ability1 hit detected (trigger stat = " + trigger.get() + "). Opening 1s window.");

        stats.subtractStatValue(Predictable.ALL, triggerStatIndex, trigger.get());
        cmd.run(s -> s.putComponent(ref, EntityStatMap.getComponentType(), stats));
        switchStrike.openWindow(null);

        LOGGER.log(Level.INFO, "[SwitchStrike] STEP 1 OK — Window is now OPEN. Player has 1 second to swap.");
    }
}
