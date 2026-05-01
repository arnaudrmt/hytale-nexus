package fr.arnaud.nexus.ability.strike;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.ability.core.AbstractCoreAbilitySystem;
import fr.arnaud.nexus.ability.core.ActiveCoreComponent;
import fr.arnaud.nexus.ability.core.CoreAbility;
import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.feature.resource.PlayerStatsManager;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

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

    public void tryActivate(@NonNullDecl Ref<EntityStore> ref,
                            @NonNullDecl Store<EntityStore> store) {
        StrikeComponent strike = store.getComponent(ref, StrikeComponent.getComponentType());
        if (strike == null || strike.getState() != StrikeComponent.State.IDLE) return;

        PlayerStatsManager psm = Nexus.getInstance().getPlayerStatsManager();
        float max = psm.getMaxStamina(ref, store);
        float current = psm.getStamina(ref, store);
        if (max <= 0f || current < max * StrikeComponent.STAMINA_COST_RATIO) return;

        store.putComponent(ref, StrikePendingComponent.getComponentType(), new StrikePendingComponent());
    }
}
