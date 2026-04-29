package fr.arnaud.nexus.item.weapon.system;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemQuality;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.item.weapon.component.WeaponInstanceComponent;
import fr.arnaud.nexus.item.weapon.data.WeaponBsonSchema;
import fr.arnaud.nexus.item.weapon.stats.WeaponStatCalculator;
import org.bson.BsonDocument;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class WeaponEquipSystem {

    public void onWeaponEquipped(@Nonnull Ref<EntityStore> playerRef,
                                 @Nullable ItemStack incomingStack,
                                 @Nonnull Store<EntityStore> store) {
        tearDownWeapon(playerRef, store);

        if (incomingStack == null || !isItemNexusWeapon(incomingStack)) return;
        if (!playerRef.isValid()) return;

        BsonDocument doc = incomingStack.getMetadata();
        WeaponInstanceComponent instance = buildWeaponInstance(doc);

        store.getExternalData().getWorld().execute(() -> {
            if (!playerRef.isValid()) return;
            store.putComponent(playerRef, WeaponInstanceComponent.getComponentType(), instance);
            WeaponPassiveApplicator.apply(playerRef, store, instance);
        });
    }

    public void onWeaponUnequipped(@Nonnull Ref<EntityStore> playerRef,
                                   @Nonnull Store<EntityStore> store) {
        tearDownWeapon(playerRef, store);
    }

    private void tearDownWeapon(@Nonnull Ref<EntityStore> playerRef,
                                @Nonnull Store<EntityStore> store) {
        if (!playerRef.isValid()) return;

        WeaponInstanceComponent current = store.getComponent(
            playerRef, WeaponInstanceComponent.getComponentType());
        if (current == null) return;

        WeaponPassiveApplicator.remove(playerRef, store, current);

        store.getExternalData().getWorld().execute(() -> {
            if (playerRef.isValid()) {
                store.removeComponent(playerRef, WeaponInstanceComponent.getComponentType());
            }
        });
    }

    private WeaponInstanceComponent buildWeaponInstance(@Nonnull BsonDocument doc) {
        WeaponInstanceComponent instance = new WeaponInstanceComponent();
        instance.quality = ItemQuality.getAssetMap().getAsset(WeaponBsonSchema.readQuality(doc));
        instance.level = WeaponBsonSchema.readLevel(doc);
        instance.weaponTag = WeaponBsonSchema.readWeaponTag(doc);
        instance.enchantmentSlots = WeaponBsonSchema.readEnchantmentSlots(doc);
        instance.archetypeId = doc.containsKey("archetype_id")
            ? doc.getString("archetype_id").getValue() : "unknown";

        instance.damageMultiplierCurve = WeaponStatCalculator.calculateDamageMultiplier(doc);
        instance.healthBoostCurve = WeaponStatCalculator.calculateHealthBoost(doc);
        instance.movementSpeedCurve = WeaponStatCalculator.calculateMovementSpeedBoost(doc);

        return instance;
    }

    private boolean isItemNexusWeapon(@Nonnull ItemStack stack) {
        BsonDocument doc = stack.getMetadata();
        return doc != null && doc.containsKey("nexus_quality_value");
    }
}
