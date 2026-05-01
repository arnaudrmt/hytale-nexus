package fr.arnaud.nexus.tutorial;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.core.Nexus;
import org.jetbrains.annotations.NotNull;

public final class TutorialTimerSystem extends EntityTickingSystem<EntityStore> {

    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }

    @Override
    public void tick(float dt, int index, ArchetypeChunk<EntityStore> chunk,
                     @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> cmd) {
        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        Nexus.getInstance().getTutorialManager().tickTimers(dt, ref, cmd);
    }
}
