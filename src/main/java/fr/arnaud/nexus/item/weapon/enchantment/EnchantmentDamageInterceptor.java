package fr.arnaud.nexus.item.weapon.enchantment;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.item.weapon.component.WeaponInstanceComponent;
import fr.arnaud.nexus.item.weapon.enchantment.event.NexusEnchantBus;
import fr.arnaud.nexus.item.weapon.enchantment.event.NexusEnchantEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class EnchantmentDamageInterceptor extends DamageEventSystem {

    private Query<EntityStore> query;

    private Query<EntityStore> query() {
        if (query == null) query = WeaponInstanceComponent.getComponentType();
        return query;
    }

    @Override
    @Nullable
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getInspectDamageGroup();
    }

    @Override
    @Nonnull
    public Query<EntityStore> getQuery() {
        return query();
    }

    @Override
    public void handle(
        int index,
        @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Damage damage
    ) {
        if (damage.isCancelled()) return;

        Damage.Source source = damage.getSource();
        if (!(source instanceof Damage.EntitySource entitySource)) return;

        Ref<EntityStore> attackerRef = entitySource.getRef();
        if (!attackerRef.isValid()) return;

        WeaponInstanceComponent weapon = commandBuffer.getComponent(
            attackerRef, WeaponInstanceComponent.getComponentType()
        );
        if (weapon == null) return;

        Ref<EntityStore> targetRef = archetypeChunk.getReferenceTo(index);

        NexusEnchantEvent event = new NexusEnchantEvent(
            NexusEnchantEvent.Type.ON_HIT,
            attackerRef,
            targetRef,
            damage.getAmount(),
            store,
            Nexus.get().getStatIndexResolver()
        );

        NexusEnchantBus.get().publish(event);
    }
}
