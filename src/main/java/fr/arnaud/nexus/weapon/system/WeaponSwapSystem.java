package fr.arnaud.nexus.weapon.system;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.weapon.component.PlayerWeaponStateComponent;
import org.bson.BsonDocument;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public final class WeaponSwapSystem {

    private final WeaponEquipSystem equipSystem;

    public WeaponSwapSystem(WeaponEquipSystem equipSystem) {
        this.equipSystem = equipSystem;
        PacketAdapters.registerInbound(this::interceptAbility2);
    }

    private boolean interceptAbility2(
        @NonNullDecl PlayerRef playerRef,
        @NonNullDecl Packet packet
    ) {
        if (!(packet instanceof SyncInteractionChains syncPacket)) return false;

        for (SyncInteractionChain chain : syncPacket.updates) {
            if (chain.data == null || !chain.initial) continue;
            if (chain.interactionType != InteractionType.Ability2) continue;

            Ref<EntityStore> ref = playerRef.getReference();
            if (ref == null || !ref.isValid()) return false;

            Store<EntityStore> store = ref.getStore();
            store.getExternalData().getWorld().execute(() -> performSwap(ref, store));

            return false;
        }

        return false;
    }

    private void performSwap(Ref<EntityStore> ref, Store<EntityStore> store) {
        PlayerWeaponStateComponent state = store.getComponent(
            ref, PlayerWeaponStateComponent.getComponentType()
        );
        if (state == null) return;

        BsonDocument incomingDoc = state.getInactiveDocument();
        if (incomingDoc == null) return;

        state.activeTag = state.getInactiveTag();

        InventoryComponent.Hotbar hotbar = store.getComponent(
            ref, InventoryComponent.Hotbar.getComponentType()
        );
        if (hotbar == null) return;

        hotbar.getInventory().setItemStackForSlot((short) 0,
            buildStack(state.activeTag, incomingDoc));
        hotbar.markDirty();

        store.getExternalData().getWorld().execute(() ->
            store.putComponent(ref, PlayerWeaponStateComponent.getComponentType(), state)
        );

        equipSystem.onWeaponEquipped(ref, buildStack(state.activeTag, incomingDoc), store);
    }

    private ItemStack buildStack(
        fr.arnaud.nexus.weapon.data.WeaponTag tag,
        BsonDocument doc
    ) {
        String archetypeId = tag == fr.arnaud.nexus.weapon.data.WeaponTag.MELEE
            ? "Nexus_Weapon_Sword_Default"
            : "Nexus_Weapon_Staff_Default";
        return new ItemStack(archetypeId, 1, doc);
    }
}
