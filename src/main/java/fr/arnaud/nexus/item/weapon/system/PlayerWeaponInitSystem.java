package fr.arnaud.nexus.item.weapon.system;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.core.Nexus;
import fr.arnaud.nexus.item.weapon.component.PlayerWeaponStateComponent;
import fr.arnaud.nexus.item.weapon.data.WeaponTag;
import org.bson.BsonDocument;
import org.jetbrains.annotations.NotNull;

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
    public void onEntityAdded(@NotNull Ref<EntityStore> ref, @NotNull AddReason reason,
                              @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> cmd) {
        if (reason != AddReason.LOAD) return;

        PlayerWeaponStateComponent existing = store.getComponent(ref, PlayerWeaponStateComponent.getComponentType());

        if (existing != null && existing.meleeDocument != null) {
            restoreEquippedWeapon(ref, existing, store);
            return;
        }

        PlayerWeaponStateComponent state = new PlayerWeaponStateComponent();
        state.meleeDocument = generateDefaultWeapon("Nexus_Melee_Sword_Default");
        state.rangedDocument = generateDefaultWeapon("Nexus_Ranged_Staff_Default");
        state.activeTag = WeaponTag.MELEE;

        cmd.run(s -> s.putComponent(ref, PlayerWeaponStateComponent.getComponentType(), state));

        placeWeaponInHotbarSlot(ref, WeaponTag.MELEE, state.meleeDocument, store);
    }

    @Override
    public void onEntityRemove(@NotNull Ref<EntityStore> ref, @NotNull RemoveReason reason,
                               @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> cmd) {
        equipSystem.onWeaponUnequipped(ref, store);
    }

    private void restoreEquippedWeapon(Ref<EntityStore> playerRef,
                                       PlayerWeaponStateComponent state,
                                       Store<EntityStore> store) {
        BsonDocument activeDoc = state.getActiveDocument();
        if (activeDoc == null || !activeDoc.containsKey("archetype_id")) return;
        placeWeaponInHotbarSlot(playerRef, state.activeTag, activeDoc, store);
    }

    private BsonDocument generateDefaultWeapon(String archetypeId) {
        Item item = Item.getAssetMap().getAsset(archetypeId);
        if (item == null) return null;
        return Nexus.getInstance().getWeaponGenerator().generateWeapon(item);
    }

    private void placeWeaponInHotbarSlot(Ref<EntityStore> playerRef, WeaponTag tag,
                                         BsonDocument doc, Store<EntityStore> store) {
        InventoryComponent.Hotbar hotbar = store.getComponent(playerRef, InventoryComponent.Hotbar.getComponentType());
        if (hotbar == null) return;
        hotbar.getInventory().setItemStackForSlot((short) 0, WeaponSwapSystem.buildItemStackForWeapon(tag, doc));
        hotbar.markDirty();
    }
}
