package fr.arnaud.nexus.tutorial;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.session.RunSessionComponent;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class TutorialManager {

    private List<TutorialStepConfig> steps = List.of();

    private final Map<Ref<EntityStore>, Integer> currentIndex = new ConcurrentHashMap<>();
    private final Map<Ref<EntityStore>, float[]> manualTimers = new ConcurrentHashMap<>();
    private final Set<Ref<EntityStore>> pendingAdvances = ConcurrentHashMap.newKeySet();

    public void loadSteps() {
        steps = TutorialConfigLoader.load();
    }

    public void onPlayerReady(Player player) {
        if (steps.isEmpty()) return;

        Ref<EntityStore> ref = player.getReference();
        if (ref == null || !ref.isValid()) return;

        Store<EntityStore> store = ref.getStore();
        RunSessionComponent session = store.getComponent(ref, RunSessionComponent.getComponentType());
        if (session == null || session.isTutorialCompleted()) return;

        currentIndex.put(ref, 0);
        TutorialStepConfig first = steps.getFirst();
        startTimerIfNeeded(ref, first);

        Nexus.get().getNexusHudSystem().showTutorialStep(store.getComponent(ref, Player.getComponentType()), first);
    }

    public void onTriggerFired(Ref<EntityStore> ref, TutorialTriggerType type) {
        TutorialStepConfig step = getCurrentStep(ref);
        if (step == null || step.triggerType() != type) return;
        pendingAdvances.add(ref);
    }

    public void onNextClicked(Ref<EntityStore> ref) {
        TutorialStepConfig step = getCurrentStep(ref);
        if (step == null || !step.isClickBased()) return;
        pendingAdvances.add(ref);
    }

    public void tickTimers(float dt, Ref<EntityStore> ref, CommandBuffer<EntityStore> cmd) {
        if (pendingAdvances.remove(ref)) {
            advance(ref, cmd);
            return;
        }

        float[] timer = manualTimers.get(ref);
        if (timer == null) return;
        timer[0] -= dt;
        if (timer[0] <= 0f) {
            manualTimers.remove(ref);
            advance(ref, cmd);
        }
    }

    private void startTimerIfNeeded(Ref<EntityStore> ref, TutorialStepConfig step) {
        if (!step.isTimerBased()) {
            manualTimers.remove(ref);
            return;
        }
        manualTimers.put(ref, new float[]{step.displaySeconds()});
    }

    private void advance(Ref<EntityStore> ref, CommandBuffer<EntityStore> cmd) {
        Integer idx = currentIndex.get(ref);
        if (idx == null) return;

        int next = idx + 1;
        if (next >= steps.size()) {
            complete(ref, cmd);
            return;
        }

        currentIndex.put(ref, next);
        TutorialStepConfig nextStep = steps.get(next);
        startTimerIfNeeded(ref, nextStep);

        Store<EntityStore> store = ref.getStore();
        store.getExternalData().getWorld().execute(() -> {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) return;
            Nexus.get().getNexusHudSystem().showTutorialStep(player, nextStep);
        });
    }

    private void complete(Ref<EntityStore> ref, CommandBuffer<EntityStore> cmd) {
        currentIndex.remove(ref);
        manualTimers.remove(ref);

        cmd.run(store -> {
            RunSessionComponent session = store.getComponent(ref, RunSessionComponent.getComponentType());
            if (session == null) return;
            session.markTutorialCompleted();
            store.putComponent(ref, RunSessionComponent.getComponentType(), session);
        });

        ref.getStore().getExternalData().getWorld().execute(() -> {
            Player player = ref.getStore().getComponent(ref, Player.getComponentType());
            if (player != null) Nexus.get().getNexusHudSystem().hideTutorial(player);
        });
    }

    public void removePlayer(Ref<EntityStore> ref) {
        currentIndex.remove(ref);
        manualTimers.remove(ref);
        pendingAdvances.remove(ref);
    }

    @Nullable
    public TutorialStepConfig getCurrentStep(Ref<EntityStore> ref) {
        Integer idx = currentIndex.get(ref);
        if (idx == null || idx >= steps.size()) return null;
        return steps.get(idx);
    }
}
