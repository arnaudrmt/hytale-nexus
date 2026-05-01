package fr.arnaud.nexus.ability.strike;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Intercepts Ability1 (Weapon Swap) and Ability2 (Weapon's Default Ability) packets on the Netty thread.
 */
public final class StrikeInteractionInterceptor {

    private final Map<InteractionType, BiConsumer<Ref<EntityStore>, Store<EntityStore>>> handlers;

    public StrikeInteractionInterceptor(@NonNullDecl SwitchStrikeAbility switchStrikeAbility) {
        this.handlers = buildHandlers(switchStrikeAbility);
        PacketAdapters.registerInbound(this::onPacket);
    }

    private Map<InteractionType, BiConsumer<Ref<EntityStore>, Store<EntityStore>>> buildHandlers(
        SwitchStrikeAbility switchStrikeAbility) {

        Map<InteractionType, BiConsumer<Ref<EntityStore>, Store<EntityStore>>> map =
            new EnumMap<>(InteractionType.class);

        map.put(InteractionType.Ability1, switchStrikeAbility::tryActivate);
        map.put(InteractionType.Ability2, this::putSwapConfirmedComponent);
        return map;
    }

    private boolean onPacket(@NonNullDecl PlayerRef playerRef, @NonNullDecl Packet packet) {
        if (!(packet instanceof SyncInteractionChains syncPacket)) return false;

        for (SyncInteractionChain chain : syncPacket.updates) {
            BiConsumer<Ref<EntityStore>, Store<EntityStore>> handler = handlers.get(chain.interactionType);
            if (handler == null || chain.data == null || !chain.initial) continue;
            scheduleOnWorldThread(playerRef, handler);
        }

        return false;
    }

    private void scheduleOnWorldThread(
        @NonNullDecl PlayerRef playerRef,
        BiConsumer<Ref<EntityStore>, Store<EntityStore>> handler) {

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) return;

        Store<EntityStore> store = ref.getStore();
        store.getExternalData().getWorld().execute(() -> {
            if (!ref.isValid()) return;
            handler.accept(ref, store);
        });
    }

    private void putSwapConfirmedComponent(
        @NonNullDecl Ref<EntityStore> ref,
        @NonNullDecl Store<EntityStore> store) {

        StrikeComponent strike = store.getComponent(ref, StrikeComponent.getComponentType());
        if (strike == null || strike.getState() != StrikeComponent.State.SWITCH_WINDOW) return;
        store.putComponent(ref, StrikeSwapConfirmedComponent.getComponentType(), new StrikeSwapConfirmedComponent());
    }
}
