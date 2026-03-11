package fr.arnaud.nexus.component;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;

/**
 * Owns the player's two-weapon loadout as hotbar slot indices and tracks
 * which slot is currently the active weapon.
 * <p>
 * Slot 0 in this component is the melee hotbar index, slot 1 is the ranged
 * hotbar index. The active index toggles between them on every swap.
 * Persisted so the player resumes with the same weapon drawn after a reconnect.
 */
public final class EquippedWeaponsComponent implements Component<EntityStore> {

    public static final byte DEFAULT_MELEE_HOTBAR_SLOT = 0;
    public static final byte DEFAULT_RANGED_HOTBAR_SLOT = 1;

    @Nullable
    private static ComponentType<EntityStore, EquippedWeaponsComponent> componentType;

    private byte meleeHotbarSlot;
    private byte rangedHotbarSlot;
    private boolean meleeActive;

    public EquippedWeaponsComponent() {
        meleeHotbarSlot = DEFAULT_MELEE_HOTBAR_SLOT;
        rangedHotbarSlot = DEFAULT_RANGED_HOTBAR_SLOT;
        meleeActive = true;
    }

    private EquippedWeaponsComponent(byte meleeHotbarSlot, byte rangedHotbarSlot, boolean meleeActive) {
        this.meleeHotbarSlot = meleeHotbarSlot;
        this.rangedHotbarSlot = rangedHotbarSlot;
        this.meleeActive = meleeActive;
    }

    public static final BuilderCodec<EquippedWeaponsComponent> CODEC = BuilderCodec
        .builder(EquippedWeaponsComponent.class, EquippedWeaponsComponent::new)
        .append(
            new KeyedCodec<>("MeleeHotbarSlot", Codec.BYTE),
            (c, v) -> c.meleeHotbarSlot = v,
            c -> c.meleeHotbarSlot
        ).add()
        .append(
            new KeyedCodec<>("RangedHotbarSlot", Codec.BYTE),
            (c, v) -> c.rangedHotbarSlot = v,
            c -> c.rangedHotbarSlot
        ).add()
        .append(
            new KeyedCodec<>("MeleeActive", Codec.BOOLEAN),
            (c, v) -> c.meleeActive = v,
            c -> c.meleeActive
        ).add()
        .build();

    /**
     * Toggles the active weapon slot. Returns the hotbar slot index of the
     * weapon that is now becoming active.
     */
    public byte swap() {
        meleeActive = !meleeActive;
        return getActiveHotbarSlot();
    }

    public byte getActiveHotbarSlot() {
        return meleeActive ? meleeHotbarSlot : rangedHotbarSlot;
    }

    public byte getInactiveHotbarSlot() {
        return meleeActive ? rangedHotbarSlot : meleeHotbarSlot;
    }

    public boolean isMeleeActive() {
        return meleeActive;
    }

    // --- ECS Boilerplate ---

    @NonNullDecl
    public static ComponentType<EntityStore, EquippedWeaponsComponent> getComponentType() {
        if (componentType == null) throw new IllegalStateException("EquippedWeaponsComponent not registered.");
        return componentType;
    }

    public static void setComponentType(@Nullable ComponentType<EntityStore, EquippedWeaponsComponent> type) {
        componentType = type;
    }

    @Override
    @NonNullDecl
    public EquippedWeaponsComponent clone() {
        return new EquippedWeaponsComponent(meleeHotbarSlot, rangedHotbarSlot, meleeActive);
    }
}
