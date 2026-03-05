package fr.arnaud.nexus.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.component.AwakeningMarkerComponent;
import fr.arnaud.nexus.component.FlowComponent;
import fr.arnaud.nexus.component.LucidityComponent;
import fr.arnaud.nexus.i18n.I18n;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/**
 * ECS damage system — handles all Flow and Lucidity mutations triggered by combat.
 *
 * Damage received (target entity has Nexus components):
 *   - Lose 1 Flow segment (Retention roll inside FlowComponent.removeSegments).
 *   - Drain Lucidity flat per hit; trigger Awakening if depleted.
 *
 * Damage dealt (attacker entity has Nexus components):
 *   - Called explicitly from ability/attack handlers via {@link #onDamageDealt}.
 *   - Fills Flow proportional to damage dealt.
 *
 * GDD refs: § Bar de Flow / Sanction & Accumulation, § Fiche Joueur / Stabilité Onirique.
 *
 * Registration in Nexus#registerSystems():
 *   getEntityStoreRegistry().registerSystem(new DamageSystem());
 *
 * manifest.json dependency required:
 *   "Dependencies": { "Hytale:DamageModule": "*" }
 */
public final class DamageSystem extends DamageEventSystem {

    /** Flow gain per point of damage dealt (tuning constant). */
    private static final float FLOW_PER_DAMAGE_POINT  = 0.02f;

    /** Lucidity drained per hit received (flat; balance pass needed). */
    private static final float LUCIDITY_DRAIN_PER_HIT = 1.0f;

    // -------------------------------------------------------------------------
    // ECS query — process all entities; Nexus component presence checked inside.
    // -------------------------------------------------------------------------

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    // -------------------------------------------------------------------------
    // Damage received — called by the Inspect Damage Group (after health loss)
    // -------------------------------------------------------------------------

    @Override
    public void handle(
            int index,
            @NonNullDecl ArchetypeChunk<EntityStore> chunk,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl CommandBuffer<EntityStore> commandBuffer,
            @NonNullDecl Damage damage
    ) {
        Ref<EntityStore> targetRef = chunk.getReferenceTo(index);

        FlowComponent flow = store.getComponent(targetRef, FlowComponent.getComponentType());
        LucidityComponent lucidity = store.getComponent(targetRef, LucidityComponent.getComponentType());
        Player player = store.getComponent(targetRef, Player.getComponentType());

        // Only react for player entities that have Nexus components attached.
        if (flow == null || lucidity == null || player == null) return;

        // --- Flow segment loss (GDD: 1 segment per hit received) -------------
        int lost = flow.removeSegments(1, /* applyRetention= */ true);
        // Persist mutated component back to the store.
        store.putComponent(targetRef, FlowComponent.getComponentType(), flow);

        if (lost > 0) {
            player.sendMessage(Message.raw(I18n.t("flow.segment.lost", lost)));
        }

        // --- Lucidity drain --------------------------------------------------
        boolean awakened = lucidity.drain(LUCIDITY_DRAIN_PER_HIT);
        store.putComponent(targetRef, LucidityComponent.getComponentType(), lucidity);

        if (awakened) {
            triggerAwakening(player, targetRef, store, commandBuffer);
        } else if (lucidity.getCurrent() < lucidity.getMax() * 0.25f) {
            player.sendMessage(Message.raw(I18n.t("lucidity.low")));
        }
    }

    // -------------------------------------------------------------------------
    // Damage dealt — call explicitly from ability / attack handlers
    // -------------------------------------------------------------------------

    /**
     * Invoked from combat/ability code after final damage value is known.
     * Not an ECS event listener — must be called directly.
     *
     * @param attackerRef ECS ref of the attacking player.
     * @param world       world the attacker resides in.
     * @param damageDealt final post-modifier damage value.
     */
    public static void onDamageDealt(
            @NonNullDecl Ref<EntityStore> attackerRef,
            @NonNullDecl World world,
            float damageDealt
    ) {
        world.execute(() -> {
            Store<EntityStore> store = world.getEntityStore().getStore();
            if (!attackerRef.isValid()) return;

            FlowComponent flow = store.getComponent(attackerRef, FlowComponent.getComponentType());
            Player player = store.getComponent(attackerRef, Player.getComponentType());

            if (flow == null || player == null) return;

            boolean justFilled = flow.addFlow(damageDealt * FLOW_PER_DAMAGE_POINT);
            store.putComponent(attackerRef, FlowComponent.getComponentType(), flow);

            if (justFilled) {
                player.sendMessage(Message.raw(I18n.t("switch_strike.ready")));
            }
        });
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static void triggerAwakening(
            @NonNullDecl Player player,
            @NonNullDecl Ref<EntityStore> ref,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl CommandBuffer<EntityStore> commandBuffer
    ) {
        player.sendMessage(Message.raw(I18n.t("lucidity.zero")));

        // Guard: only enqueue once; AwakeningSystem processes it next frame.
        if (store.getComponent(ref, AwakeningMarkerComponent.getComponentType()) == null) {
            commandBuffer.addComponent(
                    ref,
                    AwakeningMarkerComponent.getComponentType(),
                    new AwakeningMarkerComponent()
            );
        }
    }
}
