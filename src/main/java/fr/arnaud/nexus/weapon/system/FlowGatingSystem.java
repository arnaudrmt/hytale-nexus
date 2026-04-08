package fr.arnaud.nexus.weapon.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.handler.FlowHandler;
import fr.arnaud.nexus.weapon.component.WeaponInstanceComponent;
import fr.arnaud.nexus.weapon.component.WeaponInstanceComponent.ActiveEnchantmentState;
import fr.arnaud.nexus.weapon.enchantment.EnchantmentHandler;
import fr.arnaud.nexus.weapon.enchantment.EnchantmentRegistry;

import java.util.ArrayList;
import java.util.List;

public final class FlowGatingSystem extends EntityTickingSystem<EntityStore> {

    private final FlowHandler flowHandler;

    public FlowGatingSystem(FlowHandler flowHandler) {
        this.flowHandler = flowHandler;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
            PlayerRef.getComponentType(),
            WeaponInstanceComponent.getComponentType()
        );
    }

    @Override
    public void tick(
        float dt,
        int index,
        ArchetypeChunk<EntityStore> chunk,
        Store<EntityStore> store,
        CommandBuffer<EntityStore> cmd
    ) {
        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        WeaponInstanceComponent instance = chunk.getComponent(
            index, WeaponInstanceComponent.getComponentType()
        );

        if (instance.activeStates.isEmpty()) return;

        int filledSegments = flowHandler.getFilledSegments(ref, store);
        List<ActiveEnchantmentState> updatedStates = resolveGateTransitions(
            ref, instance, filledSegments, store, cmd
        );

        if (updatedStates != null) {
            WeaponInstanceComponent updated = instance.clone();
            updated.activeStates = updatedStates;
            cmd.run(s -> s.putComponent(ref, WeaponInstanceComponent.getComponentType(), updated));
        }
    }

    private List<ActiveEnchantmentState> resolveGateTransitions(
        Ref<EntityStore> ref,
        WeaponInstanceComponent instance,
        int filledSegments,
        Store<EntityStore> store,
        CommandBuffer<EntityStore> cmd
    ) {
        List<ActiveEnchantmentState> updated = null;

        for (int i = 0; i < instance.activeStates.size(); i++) {
            ActiveEnchantmentState state = instance.activeStates.get(i);
            boolean shouldBeActive = filledSegments >= state.level();

            if (shouldBeActive == state.flowGateActive()) continue;

            EnchantmentHandler handler = EnchantmentRegistry.get().getHandler(state.enchantmentId());

            if (shouldBeActive) {
                if (handler != null) handler.onActivate(ref, state.level(), store, cmd);
            } else {
                if (handler != null) handler.onDeactivate(ref, state.level(), store, cmd);
            }

            if (updated == null) {
                updated = new ArrayList<>(instance.activeStates);
            }
            updated.set(i, state.withGateActive(shouldBeActive));
        }

        return updated;
    }
}
