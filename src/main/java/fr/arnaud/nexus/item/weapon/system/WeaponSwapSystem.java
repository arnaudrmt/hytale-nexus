package fr.arnaud.nexus.item.weapon.system;

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
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.item.weapon.component.PlayerWeaponStateComponent;
import fr.arnaud.nexus.item.weapon.data.WeaponTag;
import org.bson.BsonDocument;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public final class WeaponSwapSystem {

    private static final String DEFAULT_MELEE_ARCHETYPE = "Nexus_Melee_Sword_Default";
    private static final String DEFAULT_RANGED_ARCHETYPE = "Nexus_Ranged_Staff_Default";

    private final WeaponEquipSystem equipSystem;

    public WeaponSwapSystem(WeaponEquipSystem equipSystem) {
        this.equipSystem = equipSystem;
        PacketAdapters.registerInbound(this::interceptAbility2);
    }

    private boolean interceptAbility2(@NonNullDecl PlayerRef playerRef, @NonNullDecl Packet packet) {
        if (!(packet instanceof SyncInteractionChains syncPacket)) return false;

        for (SyncInteractionChain chain : syncPacket.updates) {
            if (chain.data == null || !chain.initial) continue;
            if (chain.interactionType != InteractionType.Ability2) continue;

            Ref<EntityStore> ref = playerRef.getReference();
            if (ref == null) return false;

            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();
            world.execute(() -> {
                if (!ref.isValid()) return;
                performWeaponSwap(ref, store);
            });

            return false;
        }
        return false;
    }

    private void performWeaponSwap(Ref<EntityStore> ref, Store<EntityStore> store) {
        PlayerWeaponStateComponent state = store.getComponent(ref, PlayerWeaponStateComponent.getComponentType());
        if (state == null) return;

        BsonDocument incomingDoc = state.getInactiveDocument();
        if (incomingDoc == null) return;

        state.activeTag = state.getInactiveTag();

        InventoryComponent.Hotbar hotbar = store.getComponent(ref, InventoryComponent.Hotbar.getComponentType());
        if (hotbar == null) return;

        ItemStack incomingStack = buildItemStackForWeapon(state.activeTag, incomingDoc);
        hotbar.getInventory().setItemStackForSlot((short) 0, incomingStack);
        hotbar.markDirty();

        store.getExternalData().getWorld().execute(() ->
            store.putComponent(ref, PlayerWeaponStateComponent.getComponentType(), state)
        );

        equipSystem.onWeaponEquipped(ref, incomingStack, store);
    }

    static ItemStack buildItemStackForWeapon(WeaponTag tag, BsonDocument doc) {
        String archetypeId = doc.containsKey("archetype_id")
            ? doc.getString("archetype_id").getValue()
            : defaultArchetypeFor(tag);
        return new ItemStack(archetypeId, 1, doc);
    }

    private static String defaultArchetypeFor(WeaponTag tag) {
        return tag == WeaponTag.MELEE ? DEFAULT_MELEE_ARCHETYPE : DEFAULT_RANGED_ARCHETYPE;
    }
}
