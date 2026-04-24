package fr.arnaud.nexus.ability;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Persistent component tracking which {@link CoreAbility} instances the player
 * has unlocked, and which one is currently equipped (null = empty slot).
 *
 * <p>Serialization strategy:
 * <ul>
 *   <li>{@code EquippedCoreId} — empty string encodes null (no Core equipped).
 *   <li>{@code UnlockedCoreIds} — comma-separated ids, empty string encodes empty set.
 * </ul>
 */
public final class ActiveCoreComponent implements Component<EntityStore> {

    private static final String SEPARATOR = ",";
    private static final String EMPTY_SENTINEL = "";

    @Nullable
    private static ComponentType<EntityStore, ActiveCoreComponent> componentType;

    @Nullable
    private String equippedCoreId;

    private final Set<CoreAbility> unlockedCores = EnumSet.noneOf(CoreAbility.class);

    public ActiveCoreComponent() {
    }

    private ActiveCoreComponent(@Nullable String equippedCoreId, Set<CoreAbility> unlockedCores) {
        this.equippedCoreId = equippedCoreId;
        this.unlockedCores.addAll(unlockedCores);
    }

    public static final BuilderCodec<ActiveCoreComponent> CODEC = BuilderCodec
        .builder(ActiveCoreComponent.class, ActiveCoreComponent::new)
        .append(
            new KeyedCodec<>("EquippedCoreId", Codec.STRING),
            (component, value) -> component.equippedCoreId = value.isEmpty() ? null : value,
            component -> component.equippedCoreId != null ? component.equippedCoreId : EMPTY_SENTINEL
        )
        .add()
        .append(
            new KeyedCodec<>("UnlockedCoreIds", Codec.STRING),
            (component, value) -> {
                component.unlockedCores.clear();
                if (!value.isEmpty()) {
                    Arrays.stream(value.split(SEPARATOR))
                          .map(CoreAbility::fromId)
                          .filter(a -> a != null)
                          .forEach(component.unlockedCores::add);
                }
            },
            component -> component.unlockedCores.stream()
                                                .map(CoreAbility::getId)
                                                .collect(Collectors.joining(SEPARATOR))
        )
        .add()
        .build();

    // --- Unlock API ---

    /**
     * Unlocks a Core. Safe to call multiple times — idempotent.
     *
     * @return true if this was a new unlock, false if already unlocked.
     */
    public boolean unlock(@NonNullDecl CoreAbility ability) {
        return unlockedCores.add(ability);
    }

    public boolean isUnlocked(@NonNullDecl CoreAbility ability) {
        return unlockedCores.contains(ability);
    }

    @NonNullDecl
    public Set<CoreAbility> getUnlockedCores() {
        return Collections.unmodifiableSet(unlockedCores);
    }

    // --- Equip API ---

    /**
     * Equips a Core. The Core must be unlocked first.
     *
     * @return false if the ability is not unlocked.
     */
    public boolean equip(@NonNullDecl CoreAbility ability) {
        if (!unlockedCores.contains(ability)) return false;
        this.equippedCoreId = ability.getId();
        return true;
    }

    public void unequip() {
        this.equippedCoreId = null;
    }

    @Nullable
    public CoreAbility getEquippedCore() {
        return CoreAbility.fromId(equippedCoreId);
    }

    public boolean hasEquipped(@NonNullDecl CoreAbility ability) {
        return ability.getId().equals(equippedCoreId);
    }

    public boolean isEmpty() {
        return equippedCoreId == null;
    }

    // --- ECS boilerplate ---

    @NonNullDecl
    public static ComponentType<EntityStore, ActiveCoreComponent> getComponentType() {
        if (componentType == null) throw new IllegalStateException("ActiveCoreComponent not registered.");
        return componentType;
    }

    public static void setComponentType(@NonNullDecl ComponentType<EntityStore, ActiveCoreComponent> type) {
        componentType = type;
    }

    @Override
    @NonNullDecl
    public ActiveCoreComponent clone() {
        return new ActiveCoreComponent(this.equippedCoreId, this.unlockedCores);
    }
}
