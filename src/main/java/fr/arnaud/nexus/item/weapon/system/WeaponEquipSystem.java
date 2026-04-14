package fr.arnaud.nexus.item.weapon.system;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemQuality;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.item.weapon.component.WeaponInstanceComponent;
import fr.arnaud.nexus.item.weapon.data.WeaponBsonSchema;
import fr.arnaud.nexus.item.weapon.level.WeaponConfigCalculator;
import fr.arnaud.nexus.item.weapon.stats.WeaponStatApplicator;
import fr.arnaud.nexus.item.weapon.stats.WeaponStatBag;
import fr.arnaud.nexus.item.weapon.stats.WeaponStatBagBuilder;
import org.bson.BsonDocument;

public final class WeaponEquipSystem {

    private final StatIndexResolver statResolver;
    private final WeaponStatApplicator statApplicator;

    public WeaponEquipSystem(StatIndexResolver statResolver) {
        this.statResolver = statResolver;
        this.statApplicator = new WeaponStatApplicator(statResolver);
    }

    public void onWeaponEquipped(
        Ref<EntityStore> playerRef,
        ItemStack incomingStack,
        Store<EntityStore> store
    ) {
        tearDownCurrentWeapon(playerRef, store);
        if (incomingStack == null || !isNexusWeapon(incomingStack)) return;
        if (!playerRef.isValid()) return;

        BsonDocument doc = incomingStack.getMetadata();
        WeaponInstanceComponent instance = buildInstanceFromDocument(doc);

        WeaponStatBag bag = WeaponStatBagBuilder.build(instance);
        statApplicator.apply(playerRef, bag, store);

        store.getExternalData().getWorld().execute(() -> {
            if (playerRef.isValid()) {
                store.putComponent(playerRef, WeaponInstanceComponent.getComponentType(), instance);
            }
        });
    }

    public void onWeaponUnequipped(Ref<EntityStore> playerRef, Store<EntityStore> store) {
        tearDownCurrentWeapon(playerRef, store);
    }

    /**
     * Called when an enchantment is unlocked or upgraded mid-session.
     * Rebuilds and reapplies the stat bag so changes take effect immediately.
     */
    public void refreshStats(Ref<EntityStore> playerRef, Store<EntityStore> store) {
        if (!playerRef.isValid()) return;
        WeaponInstanceComponent instance = store.getComponent(
            playerRef, WeaponInstanceComponent.getComponentType()
        );
        if (instance == null) return;

        WeaponStatBag bag = WeaponStatBagBuilder.build(instance);
        statApplicator.apply(playerRef, bag, store);
    }

    private void tearDownCurrentWeapon(Ref<EntityStore> playerRef, Store<EntityStore> store) {
        if (!playerRef.isValid()) return;

        WeaponInstanceComponent current = store.getComponent(
            playerRef, WeaponInstanceComponent.getComponentType()
        );
        if (current == null) return;

        statApplicator.remove(playerRef, store);

        store.getExternalData().getWorld().execute(() -> {
            if (playerRef.isValid()) {
                store.removeComponent(playerRef, WeaponInstanceComponent.getComponentType());
            }
        });
    }

    private WeaponInstanceComponent buildInstanceFromDocument(BsonDocument doc) {
        WeaponInstanceComponent instance = new WeaponInstanceComponent();
        instance.quality = ItemQuality.getAssetMap().getAsset(WeaponBsonSchema.readQuality(doc));
        instance.level = WeaponBsonSchema.readLevel(doc);
        instance.weaponTag = WeaponBsonSchema.readWeaponTag(doc);
        instance.enchantmentSlots = WeaponBsonSchema.readEnchantmentSlots(doc);
        instance.archetypeId = doc.containsKey("archetype_id")
            ? doc.getString("archetype_id").getValue()
            : "unknown";

        instance.damageMultiplierCurve = WeaponConfigCalculator.calculateDamageMultiplier(doc);
        instance.healthBoostCurve = WeaponConfigCalculator.calculateHealthBoost(doc);
        instance.movementSpeedCurve = WeaponConfigCalculator.calculateMovementSpeedBoost(doc);

        return instance;
    }

    private boolean isNexusWeapon(ItemStack stack) {
        BsonDocument doc = stack.getMetadata();
        return doc != null && doc.containsKey("nexus_quality_id");
    }
}
