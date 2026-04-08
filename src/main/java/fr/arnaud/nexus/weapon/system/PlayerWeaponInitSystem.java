package fr.arnaud.nexus.weapon.system;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.Nexus;
import fr.arnaud.nexus.weapon.component.PlayerWeaponStateComponent;
import fr.arnaud.nexus.weapon.data.WeaponRarity;
import fr.arnaud.nexus.weapon.data.WeaponTag;
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
            ref, PlayerWeaponStateComponent.getComponentType()
        );

        if (existing != null && existing.meleeDocument != null) {
            restoreActiveWeapon(ref, existing, store);
            return;
        }

        PlayerWeaponStateComponent state = new PlayerWeaponStateComponent();
        state.meleeDocument = Nexus.get().getWeaponGenerator()
                                   .generate("Nexus_Weapon_Sword_Default", WeaponTag.MELEE, WeaponRarity.COMMON);
        state.rangedDocument = Nexus.get().getWeaponGenerator()
                                    .generate("Nexus_Weapon_Staff_Default", WeaponTag.RANGED, WeaponRarity.COMMON);
        state.activeTag = WeaponTag.MELEE;

        cmd.run(s -> s.putComponent(ref, PlayerWeaponStateComponent.getComponentType(), state));

        placeWeaponInSlot(ref, "Nexus_Weapon_Sword_Default", state.meleeDocument, store);
        equipSystem.onWeaponEquipped(
            ref,
            new ItemStack("Nexus_Weapon_Sword_Default", 1, state.meleeDocument),
            store
        );
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

        String archetypeId = state.activeTag == WeaponTag.MELEE
            ? "Nexus_Weapon_Sword_Default"
            : "Nexus_Weapon_Staff_Default";

        placeWeaponInSlot(ref, archetypeId, activeDoc, store);
        equipSystem.onWeaponEquipped(
            ref,
            new ItemStack(archetypeId, 1, activeDoc),
            store
        );
    }

    private void placeWeaponInSlot(
        Ref<EntityStore> ref,
        String archetypeId,
        BsonDocument doc,
        Store<EntityStore> store
    ) {
        InventoryComponent.Hotbar hotbar = store.getComponent(
            ref, InventoryComponent.Hotbar.getComponentType()
        );
        if (hotbar == null) return;
        hotbar.getInventory().setItemStackForSlot(
            (short) 0, new ItemStack(archetypeId, 1, doc)
        );
        hotbar.markDirty();
    }
}
