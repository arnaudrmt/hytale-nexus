package fr.arnaud.nexus.item.weapon.system;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.item.weapon.component.WeaponInstanceComponent;
import fr.arnaud.nexus.item.weapon.component.WeaponInstanceComponent.ActiveEnchantmentState;
import fr.arnaud.nexus.item.weapon.data.EnchantmentSlot;
import fr.arnaud.nexus.item.weapon.data.WeaponBsonSchema;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentDefinition;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentDefinitionLoader;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentHandler;
import fr.arnaud.nexus.item.weapon.enchantment.EnchantmentRegistry;
import org.bson.BsonDocument;

import java.util.ArrayList;
import java.util.List;

public final class WeaponEquipSystem {

    private static final String DAMAGE_SCALE_MODIFIER_KEY = "nexus_equipped_weapon_rarity_scale";

    private final EnchantmentDefinitionLoader loader;

    public WeaponEquipSystem(EnchantmentDefinitionLoader loader) {
        this.loader = loader;
    }

    public void onWeaponEquipped(
        Ref<EntityStore> playerRef,
        ItemStack incomingStack,
        Store<EntityStore> store
    ) {
        tearDownCurrentWeapon(playerRef, store);
        if (incomingStack == null || !isNexusWeapon(incomingStack)) return;

        BsonDocument doc = incomingStack.getMetadata();
        WeaponInstanceComponent instance = buildInstanceFromDocument(doc);
        applyDamageScaleModifier(playerRef, instance, store);
        initializeEnchantmentStates(instance);

        store.getExternalData().getWorld().execute(() ->
            store.putComponent(playerRef, WeaponInstanceComponent.getComponentType(), instance)
        );
    }

    public void onWeaponUnequipped(Ref<EntityStore> playerRef, Store<EntityStore> store) {
        tearDownCurrentWeapon(playerRef, store);
    }

    private void tearDownCurrentWeapon(Ref<EntityStore> playerRef, Store<EntityStore> store) {
        WeaponInstanceComponent current = store.getComponent(
            playerRef, WeaponInstanceComponent.getComponentType()
        );
        if (current == null) return;

        removeDamageScaleModifier(playerRef, store);
        deactivateAllEnchants(playerRef, current, store);

        store.getExternalData().getWorld().execute(() ->
            store.removeComponent(playerRef, WeaponInstanceComponent.getComponentType())
        );
    }

    private WeaponInstanceComponent buildInstanceFromDocument(BsonDocument doc) {
        WeaponInstanceComponent instance = new WeaponInstanceComponent();
        instance.rarity = WeaponBsonSchema.readRarity(doc);
        instance.weaponTag = WeaponBsonSchema.readWeaponTag(doc);
        instance.damageMultiplier = WeaponBsonSchema.readDamageMultiplier(doc);
        instance.enchantmentSlots = WeaponBsonSchema.readEnchantmentSlots(doc);
        instance.archetypeId = doc.containsKey("archetype_id")
            ? doc.getString("archetype_id").getValue()
            : "unknown";
        return instance;
    }

    private void applyDamageScaleModifier(
        Ref<EntityStore> playerRef,
        WeaponInstanceComponent instance,
        Store<EntityStore> store
    ) {
        if (!loader.isReady()) return;
        EntityStatMap stats = store.getComponent(playerRef, EntityStatMap.getComponentType());
        if (stats == null) return;

        StaticModifier modifier = new StaticModifier(
            Modifier.ModifierTarget.MAX,
            StaticModifier.CalculationType.MULTIPLICATIVE,
            instance.damageMultiplier
        );
        stats.putModifier(
            EntityStatMap.Predictable.NONE,
            loader.getWeaponDamageScaleIndex(),
            DAMAGE_SCALE_MODIFIER_KEY,
            modifier
        );
    }

    private void removeDamageScaleModifier(Ref<EntityStore> playerRef, Store<EntityStore> store) {
        if (!loader.isReady()) return;
        EntityStatMap stats = store.getComponent(playerRef, EntityStatMap.getComponentType());
        if (stats == null) return;

        stats.removeModifier(
            EntityStatMap.Predictable.NONE,
            loader.getWeaponDamageScaleIndex(),
            DAMAGE_SCALE_MODIFIER_KEY
        );
    }

    private void initializeEnchantmentStates(WeaponInstanceComponent instance) {
        List<ActiveEnchantmentState> states = new ArrayList<>();
        for (EnchantmentSlot slot : instance.enchantmentSlots) {
            if (!slot.isUnlocked()) continue;
            EnchantmentDefinition def = EnchantmentRegistry.get().getDefinition(slot.chosen());
            if (def == null) continue;
            states.add(new ActiveEnchantmentState(slot.chosen(), def.getLevel(), false));
        }
        instance.activeStates = states;
    }

    private void deactivateAllEnchants(
        Ref<EntityStore> playerRef,
        WeaponInstanceComponent instance,
        Store<EntityStore> store
    ) {
        for (WeaponInstanceComponent.ActiveEnchantmentState state : instance.activeStates) {
            if (!state.flowGateActive()) continue;
            EnchantmentHandler handler = EnchantmentRegistry.get().getHandler(state.enchantmentId());
            if (handler != null) {
                store.getExternalData().getWorld().execute(() ->
                    handler.onDeactivate(playerRef, state.level(), store, null)
                );
            }
        }
    }

    private boolean isNexusWeapon(ItemStack stack) {
        BsonDocument doc = stack.getMetadata();
        return doc != null && doc.containsKey("nexus_rarity");
    }
}
