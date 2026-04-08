package fr.arnaud.nexus.weapon.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.weapon.component.WeaponInstanceComponent;
import fr.arnaud.nexus.weapon.component.WeaponInstanceComponent.ActiveEnchantmentState;
import fr.arnaud.nexus.weapon.enchantment.handlers.BouncingProjectileEnchantmentHandler;
import fr.arnaud.nexus.weapon.enchantment.handlers.ChainProjectileEnchantmentHandler;
import fr.arnaud.nexus.weapon.enchantment.handlers.PiercingEnchantmentHandler;

public final class RangedEnchantmentDamageInterceptor extends DamageEventSystem {

    private final ChainProjectileEnchantmentHandler chainProjectile;
    private final PiercingEnchantmentHandler piercing;
    private final BouncingProjectileEnchantmentHandler bouncing;

    public RangedEnchantmentDamageInterceptor(
        ChainProjectileEnchantmentHandler chainProjectile,
        PiercingEnchantmentHandler piercing,
        BouncingProjectileEnchantmentHandler bouncing
    ) {
        this.chainProjectile = chainProjectile;
        this.piercing = piercing;
        this.bouncing = bouncing;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void handle(
        int index,
        ArchetypeChunk<EntityStore> chunk,
        Store<EntityStore> store,
        CommandBuffer<EntityStore> cmd,
        Damage damage
    ) {
        if (!(damage.getSource() instanceof Damage.EntitySource entitySource)) return;

        Ref<EntityStore> attackerRef = entitySource.getRef();
        if (!attackerRef.isValid()) return;

        WeaponInstanceComponent instance = store.getComponent(attackerRef, WeaponInstanceComponent.getComponentType());
        if (instance == null) return;

        Ref<EntityStore> targetRef = chunk.getReferenceTo(index);
        float damageAmount = damage.getAmount();

        for (ActiveEnchantmentState state : instance.activeStates) {
            if (!state.flowGateActive()) continue;
            dispatchRangedEnchant(state, attackerRef, targetRef, damageAmount, cmd);
        }
    }

    private void dispatchRangedEnchant(
        ActiveEnchantmentState state,
        Ref<EntityStore> attackerRef,
        Ref<EntityStore> targetRef,
        float damageAmount,
        CommandBuffer<EntityStore> cmd
    ) {
        String id = state.enchantmentId();
        int level = state.level();

        if (id.contains("ChainShot")) {
            chainProjectile.execute(attackerRef, targetRef, level, cmd);
        } else if (id.contains("Piercing")) {
            piercing.execute(attackerRef, targetRef, damageAmount, level, cmd);
        } else if (id.contains("Bouncing")) {
            bouncing.execute(attackerRef, targetRef, damageAmount, level, cmd);
        }
    }
}
