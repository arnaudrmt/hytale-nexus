package fr.arnaud.nexus.item.weapon.enchantment.handlers;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.entities.ProjectileComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.item.weapon.enchantment.*;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class ChainProjectileEnchantmentHandler implements EnchantmentHandler {

    private static final String CHAIN_PROJECTILE_ASSET = "Nexus_Projectile_Chain";
    private static final float CHAIN_SEARCH_RADIUS = 12.0f;

    @Override
    public void onActivate(Ref<EntityStore> playerRef, int level, Store<EntityStore> store, CommandBuffer<EntityStore> cmd) {
    }

    @Override
    public void onDeactivate(Ref<EntityStore> playerRef, int level, Store<EntityStore> store, CommandBuffer<EntityStore> cmd) {
    }

    public void execute(
        Ref<EntityStore> attackerRef,
        Ref<EntityStore> primaryTargetRef,
        int level,
        CommandBuffer<EntityStore> cmd
    ) {
        EnchantmentDefinition def = EnchantmentRegistry.get().getDefinition("Enchant_ChainShot_" + level);
        if (def == null) return;
        EnchantmentLevelData data = def.getDataForLevel(level);
        if (data == null) return;

        int chainCount = (int) data.get("ChainCount", 1);
        float damageFraction = data.get("DamageFraction", 0.5f);
        float baseDamage = TriggerStatRegistry.get().resolveWeaponDamage(attackerRef, cmd);

        TransformComponent primaryTransform = cmd.getComponent(primaryTargetRef, TransformComponent.getComponentType());
        if (primaryTransform == null) return;

        List<Ref<EntityStore>> candidates = EnchantmentSpatialUtil.getEntitiesInRadius(primaryTargetRef, CHAIN_SEARCH_RADIUS, cmd);
        List<Ref<EntityStore>> chainTargets = candidates.stream()
                                                        .filter(ref -> !EnchantmentSpatialUtil.isSameRef(ref, attackerRef))
                                                        .filter(ref -> !EnchantmentSpatialUtil.isSameRef(ref, primaryTargetRef))
                                                        .sorted(Comparator.comparingDouble(ref ->
                                                            EnchantmentSpatialUtil.distanceBetween(primaryTargetRef, ref, cmd)))
                                                        .limit(chainCount)
                                                        .toList();

        TimeResource time = cmd.getResource(TimeResource.getResourceType());
        UUID attackerUuid = resolveUuid(attackerRef, cmd);

        for (Ref<EntityStore> chainTarget : chainTargets) {
            spawnChainProjectile(primaryTransform.getPosition(), chainTarget, baseDamage * damageFraction, time, attackerUuid, cmd);
        }
    }

    private void spawnChainProjectile(Vector3d origin, Ref<EntityStore> targetRef, float damage, TimeResource time, UUID creatorUuid, CommandBuffer<EntityStore> cmd) {
        TransformComponent targetTransform = cmd.getComponent(targetRef, TransformComponent.getComponentType());
        if (targetTransform == null) return;

        Vector3d direction = targetTransform.getPosition().clone();
        direction.subtract(origin);

        float yaw = (float) Math.toDegrees(Math.atan2(direction.x, direction.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(direction.y, Math.sqrt(direction.x * direction.x + direction.z * direction.z)));

        Holder<EntityStore> projectile = ProjectileComponent.assembleDefaultProjectile(time, CHAIN_PROJECTILE_ASSET, origin.clone(), new Vector3f(yaw, pitch, 0f));
        ProjectileComponent proj = projectile.getComponent(ProjectileComponent.getComponentType());
        if (proj != null) {
            proj.shoot(projectile, creatorUuid, origin.x, origin.y, origin.z, yaw, pitch);
        }
        cmd.addEntity(projectile, AddReason.LOAD);
    }

    private UUID resolveUuid(Ref<EntityStore> ref, CommandBuffer<EntityStore> cmd) {
        com.hypixel.hytale.server.core.entity.UUIDComponent uuidComp = cmd.getComponent(ref, com.hypixel.hytale.server.core.entity.UUIDComponent.getComponentType());
        return uuidComp != null ? uuidComp.getUuid() : UUID.randomUUID();
    }
}
