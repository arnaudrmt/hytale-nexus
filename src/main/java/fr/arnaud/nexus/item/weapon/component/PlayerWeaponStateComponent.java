package fr.arnaud.nexus.item.weapon.component;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.item.weapon.data.WeaponTag;
import org.bson.BsonDocument;

import javax.annotation.Nullable;

public final class PlayerWeaponStateComponent implements Component<EntityStore> {

    public WeaponTag activeTag;
    @Nullable
    public BsonDocument meleeDocument;
    @Nullable
    public BsonDocument rangedDocument;

    private static ComponentType<EntityStore, PlayerWeaponStateComponent> componentType;

    public PlayerWeaponStateComponent() {
        activeTag = WeaponTag.MELEE;
    }

    @Nullable
    public BsonDocument getActiveDocument() {
        return activeTag == WeaponTag.MELEE ? meleeDocument : rangedDocument;
    }

    @Nullable
    public BsonDocument getInactiveDocument() {
        return activeTag == WeaponTag.MELEE ? rangedDocument : meleeDocument;
    }

    public WeaponTag getInactiveTag() {
        return activeTag == WeaponTag.MELEE ? WeaponTag.RANGED : WeaponTag.MELEE;
    }

    public void setDocument(WeaponTag tag, BsonDocument doc) {
        if (tag == WeaponTag.MELEE) meleeDocument = doc;
        else rangedDocument = doc;
    }

    public static final BuilderCodec<PlayerWeaponStateComponent> CODEC = BuilderCodec
        .builder(PlayerWeaponStateComponent.class, PlayerWeaponStateComponent::new)
        .append(
            new KeyedCodec<>("ActiveTag", Codec.STRING),
            (c, v) -> c.activeTag = WeaponTag.valueOf(v),
            c -> c.activeTag.name()
        )
        .add()
        .append(
            new KeyedCodec<>("MeleeDocument", Codec.BSON_DOCUMENT),
            (c, v) -> c.meleeDocument = v,
            c -> c.meleeDocument
        )
        .add()
        .append(
            new KeyedCodec<>("RangedDocument", Codec.BSON_DOCUMENT),
            (c, v) -> c.rangedDocument = v,
            c -> c.rangedDocument
        )
        .add()
        .build();

    public static ComponentType<EntityStore, PlayerWeaponStateComponent> getComponentType() {
        if (componentType == null) throw new IllegalStateException("PlayerWeaponStateComponent not registered.");
        return componentType;
    }

    public static void setComponentType(ComponentType<EntityStore, PlayerWeaponStateComponent> type) {
        componentType = type;
    }

    @Override
    public PlayerWeaponStateComponent clone() {
        PlayerWeaponStateComponent c = new PlayerWeaponStateComponent();
        c.activeTag = this.activeTag;
        c.meleeDocument = this.meleeDocument != null ? this.meleeDocument.clone() : null;
        c.rangedDocument = this.rangedDocument != null ? this.rangedDocument.clone() : null;
        return c;
    }
}
