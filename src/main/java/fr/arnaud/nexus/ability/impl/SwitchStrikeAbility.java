package fr.arnaud.nexus.ability.impl;

import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap.Predictable;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.ability.AbstractCoreAbilitySystem;
import fr.arnaud.nexus.ability.ActiveCoreComponent;
import fr.arnaud.nexus.ability.CoreAbility;
import fr.arnaud.nexus.feature.combat.switchstrike.SwitchStrikeComponent;
import fr.arnaud.nexus.feature.combat.switchstrike.SwitchStrikeComponent.State;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Core ability: Switch Strike.
 *
 * <p>Replaces {@link fr.arnaud.nexus.feature.combat.switchstrike.SwitchStrikeTriggerSystem}.
 * Polls the {@code SwitchStrike_Trigger} hidden stat written by JSON Ability1 on hit,
 * and opens the Switch Strike window when the player has this Core equipped.
 */
public final class SwitchStrikeAbility extends AbstractCoreAbilitySystem {

    private static final Logger LOGGER = Logger.getLogger(SwitchStrikeAbility.class.getName());

    private int triggerStatIndex = Integer.MIN_VALUE;

    @NonNullDecl
    @Override
    public CoreAbility getAbility() {
        return CoreAbility.SWITCH_STRIKE;
    }

    @NonNullDecl
    @Override
    protected Query<EntityStore> buildQuery() {
        return Query.and(
            super.buildQuery(),
            SwitchStrikeComponent.getComponentType(),
            EntityStatMap.getComponentType()
        );
    }

    public void onAssetsLoaded(
        LoadedAssetsEvent<String, EntityStatType,
            IndexedLookupTableAssetMap<String, EntityStatType>> event) {
        triggerStatIndex = event.getAssetMap().getIndex("SwitchStrike_Trigger");
        if (triggerStatIndex == Integer.MIN_VALUE) {
            LOGGER.log(Level.SEVERE,
                "[SwitchStrike] SwitchStrike_Trigger stat not found — check Asset Editor.");
        }
    }

    @Override
    public void tickCore(float deltaSeconds, int index, ArchetypeChunk<EntityStore> chunk,
                         Store<EntityStore> store, CommandBuffer<EntityStore> cmd,
                         Ref<EntityStore> ref, ActiveCoreComponent activeCore) {
        if (triggerStatIndex == Integer.MIN_VALUE) return;

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

        stats.subtractStatValue(Predictable.ALL, triggerStatIndex, trigger.get());
        cmd.run(s -> s.putComponent(ref, EntityStatMap.getComponentType(), stats));
        switchStrike.requestOpenWindow();
    }
}
