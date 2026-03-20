package fr.arnaud.nexus.breach;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.Nexus;
import fr.arnaud.nexus.breach.BreachVisualComponent.Phase;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Drives Breach visual effects each tick:
 * - Sky: day time lerped toward midnight on FADE_IN, restored instantly on beginFadeOut
 * - Time dilation: applied only while HELD (camera fully in), restored on beginFadeOut
 */
public final class BreachVisualSystem extends EntityTickingSystem<EntityStore> {

    private static final float MIDNIGHT = 0.0f;

    private static final Map<Ref<EntityStore>, Float> savedDayTimeByRef = new ConcurrentHashMap<>();

    @NonNullDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
            PlayerRef.getComponentType(),
            BreachVisualComponent.getComponentType()
        );
    }

    @Override
    public void tick(float deltaSeconds, int index, @NonNullDecl ArchetypeChunk<EntityStore> chunk,
                     @NonNullDecl Store<EntityStore> store,
                     @NonNullDecl CommandBuffer<EntityStore> cmd) {

        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        BreachVisualComponent visual = chunk.getComponent(index, BreachVisualComponent.getComponentType());
        PlayerRef pr = chunk.getComponent(index, PlayerRef.getComponentType());

        if (visual == null || pr == null || visual.getPhase() == Phase.IDLE) return;

        float blend = visual.tick(deltaSeconds);

        driveDayTime(ref, store, visual, blend);
        driveTimeDilation(store, visual);

        cmd.run(s -> s.putComponent(ref, BreachVisualComponent.getComponentType(), visual));
    }

    // --- Static API ---

    public static void beginFadeIn(@NonNullDecl Ref<EntityStore> ref,
                                   @NonNullDecl Store<EntityStore> store,
                                   @NonNullDecl CommandBuffer<EntityStore> cmd) {
        snapshotDayTime(ref, store);
        mutateVisual(ref, store, cmd, BreachVisualComponent::beginFadeIn);
    }

    public static void beginFadeOut(@NonNullDecl Ref<EntityStore> ref,
                                    @NonNullDecl Store<EntityStore> store,
                                    @NonNullDecl CommandBuffer<EntityStore> cmd) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null && player.getWorld() != null) {
            restoreDayTime(ref, store, player.getWorld());
            try {
                World.setTimeDilation(BreachVisualComponent.TIME_DILATION_NORMAL, store);
            } catch (Exception e) {
                Nexus.get().getLogger().at(Level.WARNING).log("[Breach] restoreTimeDilation failed: " + e.getMessage());
            }
        }
        mutateVisual(ref, store, cmd, BreachVisualComponent::beginFadeOut);
    }

    public static void removePlayer(@NonNullDecl Ref<EntityStore> ref) {
        savedDayTimeByRef.remove(ref);
    }

    // --- Per-tick drivers ---

    private static void driveDayTime(@NonNullDecl Ref<EntityStore> ref,
                                     @NonNullDecl Store<EntityStore> store,
                                     @NonNullDecl BreachVisualComponent visual,
                                     float blend) {
        if (visual.getPhase() != Phase.FADE_IN && visual.getPhase() != Phase.HELD) return;

        Float savedTime = savedDayTimeByRef.get(ref);
        if (savedTime == null) return;

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;
        World world = player.getWorld();
        if (world == null) return;

        WorldTimeResource timeResource = store.getResource(WorldTimeResource.getResourceType());
        if (timeResource == null) return;

        float target = visual.getPhase() == Phase.HELD
            ? MIDNIGHT
            : lerp(savedTime, MIDNIGHT, blend);

        timeResource.setDayTime(target, world, store);
    }

    private static void driveTimeDilation(@NonNullDecl Store<EntityStore> store,
                                          @NonNullDecl BreachVisualComponent visual) {
        if (visual.getPhase() != Phase.HELD) return;
        try {
            World.setTimeDilation(BreachVisualComponent.TIME_DILATION_BREACH, store);
        } catch (Exception e) {
            Nexus.get().getLogger().at(Level.WARNING).log("[Breach] setTimeDilation failed: " + e.getMessage());
        }
    }

    // --- Day time snapshot/restore ---

    private static void snapshotDayTime(@NonNullDecl Ref<EntityStore> ref,
                                        @NonNullDecl Store<EntityStore> store) {
        if (savedDayTimeByRef.containsKey(ref)) return;
        WorldTimeResource timeResource = store.getResource(WorldTimeResource.getResourceType());
        if (timeResource == null) return;
        savedDayTimeByRef.put(ref, timeResource.getDayProgress());
    }

    private static void restoreDayTime(@NonNullDecl Ref<EntityStore> ref,
                                       @NonNullDecl Store<EntityStore> store,
                                       @NonNullDecl World world) {
        Float savedTime = savedDayTimeByRef.remove(ref);
        if (savedTime == null) return;
        WorldTimeResource timeResource = store.getResource(WorldTimeResource.getResourceType());
        if (timeResource == null) return;
        timeResource.setDayTime(savedTime, world, store);
    }

    // --- Helpers ---

    private static void mutateVisual(@NonNullDecl Ref<EntityStore> ref,
                                     @NonNullDecl Store<EntityStore> store,
                                     @NonNullDecl CommandBuffer<EntityStore> cmd,
                                     @NonNullDecl java.util.function.Consumer<BreachVisualComponent> mutation) {
        BreachVisualComponent visual = store.getComponent(ref, BreachVisualComponent.getComponentType());
        if (visual == null) {
            Nexus.get().getLogger().at(Level.WARNING).log("[Breach] BreachVisualComponent missing — skipped.");
            return;
        }
        mutation.accept(visual);
        cmd.run(s -> s.putComponent(ref, BreachVisualComponent.getComponentType(), visual));
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
