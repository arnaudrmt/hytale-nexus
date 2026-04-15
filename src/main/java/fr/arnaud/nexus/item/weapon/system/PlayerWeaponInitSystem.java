package fr.arnaud.nexus.item.weapon.system;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.item.weapon.component.PlayerWeaponStateComponent;
import fr.arnaud.nexus.item.weapon.data.WeaponTag;
import org.bson.BsonDocument;

public final class PlayerWeaponInitSystem extends RefSystem<EntityStore> {

    private final WeaponEquipSystem equipSystem;

    public PlayerWeaponInitSystem(WeaponEquipSystem equipSystem) {
        this.equipSystem = equipSystem;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }

    @Override
    public void onEntityAdded(
        Ref<EntityStore> ref,
        AddReason reason,
        Store<EntityStore> store,
        CommandBuffer<EntityStore> cmd
    ) {
        if (reason != AddReason.LOAD) return;

        PlayerWeaponStateComponent existing = store.getComponent(
            ref, PlayerWeaponStateComponent.getComponentType());

        if (existing != null && existing.meleeDocument != null) {
            restoreActiveWeapon(ref, existing, store);
            return;
        }

        PlayerWeaponStateComponent state = new PlayerWeaponStateComponent();
        state.meleeDocument = generateDefaultWeapon("Nexus_Melee_Sword_Default");
        state.rangedDocument = generateDefaultWeapon("Nexus_Ranged_Staff_Default");
        state.activeTag = WeaponTag.MELEE;

        cmd.run(s -> s.putComponent(ref, PlayerWeaponStateComponent.getComponentType(), state));

        placeWeaponInSlot(ref, "Nexus_Melee_Sword_Default", state.meleeDocument, store);
        equipSystem.onWeaponEquipped(
            ref,
            new ItemStack("Nexus_Melee_Sword_Default", 1, state.meleeDocument),
            store);
    }

    @Override
    public void onEntityRemove(
        Ref<EntityStore> ref,
        RemoveReason reason,
        Store<EntityStore> store,
        CommandBuffer<EntityStore> cmd
    ) {
        equipSystem.onWeaponUnequipped(ref, store);
    }

    private void restoreActiveWeapon(
        Ref<EntityStore> ref,
        PlayerWeaponStateComponent state,
        Store<EntityStore> store
    ) {
        BsonDocument activeDoc = state.getActiveDocument();
        if (activeDoc == null) return;

        if (!activeDoc.containsKey("archetype_id")) return;
        String archetypeId = activeDoc.getString("archetype_id").getValue();

        placeWeaponInSlot(ref, archetypeId, activeDoc, store);
        equipSystem.onWeaponEquipped(
            ref,
            new ItemStack(archetypeId, 1, activeDoc),
            store);
    }

    private BsonDocument generateDefaultWeapon(String archetypeId) {
        Item item = Item.getAssetMap().getAsset(archetypeId);
        return Nexus.get().getWeaponGenerator().generateWeapon(item);
    }

    private void placeWeaponInSlot(
        Ref<EntityStore> ref,
        String archetypeId,
        BsonDocument doc,
        Store<EntityStore> store
    ) {
        InventoryComponent.Hotbar hotbar = store.getComponent(
            ref, InventoryComponent.Hotbar.getComponentType());
        if (hotbar == null) return;
        hotbar.getInventory().setItemStackForSlot(
            (short) 0, new ItemStack(archetypeId, 1, doc));
        hotbar.markDirty();
    }
}
