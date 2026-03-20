package fr.arnaud.nexus.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.component.SwitchStrikeComponent;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Captures the hit target ref the moment an Ability1 strike connects and
 * stages it on the attacker's {@link SwitchStrikeComponent}.
 * <p>
 * This runs before {@link SwitchStrikeTriggerSystem} ticks, so by the time
 * the trigger stat is consumed and the window opens, the target ref is already
 * available for the execution phase's boss check.
 */
public final class SwitchStrikeDamageInterceptor extends DamageEventSystem {

    private static final Logger LOGGER = Logger.getLogger(SwitchStrikeDamageInterceptor.class.getName());

    @NonNullDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void handle(int index, @NonNullDecl ArchetypeChunk<EntityStore> chunk,
                       @NonNullDecl Store<EntityStore> store,
                       @NonNullDecl CommandBuffer<EntityStore> cmd,
                       @NonNullDecl Damage damage) {
        if (!(damage.getSource() instanceof Damage.EntitySource entitySource)) return;

        Ref<EntityStore> attackerRef = entitySource.getRef();
        if (attackerRef == null || !attackerRef.isValid()) return;

        SwitchStrikeComponent switchStrike =
            store.getComponent(attackerRef, SwitchStrikeComponent.getComponentType());
        if (switchStrike == null) return;

        Ref<EntityStore> targetRef = chunk.getReferenceTo(index);
        switchStrike.setPendingTriggerTarget(targetRef);
        cmd.run(s -> s.putComponent(attackerRef, SwitchStrikeComponent.getComponentType(), switchStrike));

        LOGGER.log(Level.INFO, "[SwitchStrike] DamageInterceptor staged target ref for attacker.");
    }
}
