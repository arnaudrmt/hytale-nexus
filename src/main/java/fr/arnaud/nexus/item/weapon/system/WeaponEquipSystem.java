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

    public void onWeaponEquipped(@Nonnull Ref<EntityStore> playerRef, @Nullable ItemStack incomingStack,
                                 @Nonnull Store<EntityStore> store) {
        if (!playerRef.isValid()) return;

        BsonDocument doc = resolveNexusWeaponDoc(incomingStack);

        store.getExternalData().getWorld().execute(() -> {
            if (!playerRef.isValid()) return;

            removeWeaponComponentIfPresent(playerRef, store);
            WeaponPassiveApplicator.remove(playerRef, store);

            if (doc == null) return;

            WeaponInstanceComponent instance = buildWeaponInstance(doc);
            store.putComponent(playerRef, WeaponInstanceComponent.getComponentType(), instance);
            WeaponPassiveApplicator.apply(playerRef, store, instance);
        });
    }

    public void onWeaponUnequipped(@Nonnull Ref<EntityStore> playerRef,
                                   @Nonnull Store<EntityStore> store) {
        if (!playerRef.isValid()) return;

        store.getExternalData().getWorld().execute(() -> {
            if (!playerRef.isValid()) return;
            removeWeaponComponentIfPresent(playerRef, store);
            WeaponPassiveApplicator.remove(playerRef, store);
        });
    }

    private @Nullable BsonDocument resolveNexusWeaponDoc(@Nullable ItemStack stack) {
        if (stack == null) return null;
        BsonDocument doc = stack.getMetadata();
        return WeaponBsonSchema.isNexusWeapon(doc) ? doc : null;
    }

    private void removeWeaponComponentIfPresent(@Nonnull Ref<EntityStore> playerRef,
                                                @Nonnull Store<EntityStore> store) {
        if (store.getComponent(playerRef, WeaponInstanceComponent.getComponentType()) != null) {
            store.removeComponent(playerRef, WeaponInstanceComponent.getComponentType());
        }
    }

    private WeaponInstanceComponent buildWeaponInstance(@Nonnull BsonDocument doc) {
        WeaponInstanceComponent instance = new WeaponInstanceComponent();
        instance.quality = ItemQuality.getAssetMap().getAsset(WeaponBsonSchema.readQualityValue(doc));
        instance.level = WeaponBsonSchema.readLevel(doc);
        instance.weaponTag = WeaponBsonSchema.readWeaponTag(doc);
        instance.enchantmentSlots = WeaponBsonSchema.readEnchantmentSlots(doc);
        instance.archetypeId = doc.containsKey("archetype_id")
            ? doc.getString("archetype_id").getValue() : "Nexus_Default_Melee_Sword";

        instance.damageMultiplier = WeaponStatCalculator.calculateDamageMultiplier(doc);
        instance.healthBonus = WeaponStatCalculator.calculateHealthBonus(doc);
        instance.movementSpeedBonus = WeaponStatCalculator.calculateMovementSpeedBonus(doc);

        return instance;
    }
}
