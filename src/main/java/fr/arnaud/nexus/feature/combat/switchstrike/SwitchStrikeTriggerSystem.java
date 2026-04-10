package fr.arnaud.nexus.feature.combat.switchstrike;

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
import fr.arnaud.nexus.feature.combat.switchstrike.SwitchStrikeComponent.State;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Watches the {@code SwitchStrike_Trigger} hidden stat written by JSON Ability1 on hit.
 * On detection, transitions the player to PENDING_OPEN and applies a soft slot lock.
 */
public final class SwitchStrikeTriggerSystem extends EntityTickingSystem<EntityStore> {

    private static final Logger LOGGER = Logger.getLogger(SwitchStrikeTriggerSystem.class.getName());

    private int triggerStatIndex = Integer.MIN_VALUE;

    public void onAssetsLoaded(
        LoadedAssetsEvent<String, EntityStatType,
            IndexedLookupTableAssetMap<String, EntityStatType>> event) {
        triggerStatIndex = event.getAssetMap().getIndex("SwitchStrike_Trigger");
        if (triggerStatIndex == Integer.MIN_VALUE) {
            LOGGER.log(Level.SEVERE,
                "[SwitchStrike] SwitchStrike_Trigger stat not found — check Asset Editor.");
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
            PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
            tryOpenWindow(ref, switchStrike, stats, playerRef, cmd);
        }

        cmd.run(s -> s.putComponent(ref, SwitchStrikeComponent.getComponentType(), switchStrike));
    }

    private void tryOpenWindow(@NonNullDecl Ref<EntityStore> ref,
                               @NonNullDecl SwitchStrikeComponent switchStrike,
                               @NonNullDecl EntityStatMap stats,
                               @javax.annotation.Nullable PlayerRef playerRef,
                               @NonNullDecl CommandBuffer<EntityStore> cmd) {
        EntityStatValue trigger = stats.get(triggerStatIndex);
        if (trigger == null || trigger.get() <= 0f) return;

        stats.subtractStatValue(Predictable.ALL, triggerStatIndex, trigger.get());
        cmd.run(s -> s.putComponent(ref, EntityStatMap.getComponentType(), stats));
        switchStrike.requestOpenWindow();
    }
}
