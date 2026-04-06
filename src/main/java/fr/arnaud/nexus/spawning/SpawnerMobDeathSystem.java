package fr.arnaud.nexus.spawning;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.Nexus;

public class SpawnerMobDeathSystem extends DeathSystems.OnDeathSystem {

    // Only trigger on entities that have BOTH a SpawnerTag and a DeathComponent
    private final Query<EntityStore> query = Query.and(SpawnerTagComponent.getComponentType());

    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Override
    public void onComponentAdded(Ref<EntityStore> ref, DeathComponent component, Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {
        SpawnerTagComponent tag = store.getComponent(ref, SpawnerTagComponent.getComponentType());

        if (tag != null) {
            // Safely notify the Spawn Manager
            Nexus.get().getMobSpawnManager().onMobDied(tag.getSpawnerId());
        }
    }
}
