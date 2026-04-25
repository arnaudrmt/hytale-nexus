package fr.arnaud.nexus.ability.impl;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.ability.AbstractCoreAbilitySystem;
import fr.arnaud.nexus.ability.ActiveCoreComponent;
import fr.arnaud.nexus.ability.CoreAbility;
import fr.arnaud.nexus.feature.combat.strike.StrikeComponent;
import fr.arnaud.nexus.feature.combat.strike.StrikePendingComponent;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

/**
 * Core ability gate for Strike. Consumes {@link StrikePendingComponent} markers
 * only when this Core is equipped — preventing the strike from firing without it.
 */
public final class SwitchStrikeAbility extends AbstractCoreAbilitySystem {

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
            StrikeComponent.getComponentType()
        );
    }

    @Override
    public void tickCore(float deltaSeconds, int index, ArchetypeChunk<EntityStore> chunk,
                         Store<EntityStore> store, CommandBuffer<EntityStore> cmd,
                         Ref<EntityStore> ref, ActiveCoreComponent activeCore) {
    }
}
