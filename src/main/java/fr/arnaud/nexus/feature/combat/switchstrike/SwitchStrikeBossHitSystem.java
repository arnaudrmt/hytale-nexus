package fr.arnaud.nexus.feature.combat.switchstrike;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.Set;

public final class SwitchStrikeBossHitSystem extends DamageEventSystem {

    static final Set<String> BOSS_ROLE_IDS = Set.of("Nexus_TestBoss_NPC_Role");

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
        NPCEntity npc = store.getComponent(targetRef, NPCEntity.getComponentType());
        if (!isBoss(npc)) return;

        switchStrike.markBossHit(targetRef);
        cmd.run(s -> s.putComponent(attackerRef, SwitchStrikeComponent.getComponentType(), switchStrike));
    }

    private boolean isBoss(NPCEntity npc) {
        if (npc == null) return false;
        String roleId = npc.getNPCTypeId();
        return roleId != null && BOSS_ROLE_IDS.contains(roleId);
    }
}
