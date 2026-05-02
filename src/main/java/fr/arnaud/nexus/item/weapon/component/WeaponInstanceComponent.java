package fr.arnaud.nexus.item.weapon.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemQuality;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.item.weapon.data.EnchantmentSlot;
import fr.arnaud.nexus.item.weapon.data.WeaponTag;

import java.util.ArrayList;
import java.util.List;

public final class WeaponInstanceComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, WeaponInstanceComponent> componentType;

    public String archetypeId;
    public ItemQuality quality;
    public int level;
    public WeaponTag weaponTag;
    public List<EnchantmentSlot> enchantmentSlots;

    public double damageMultiplier;
    public double healthBonus;
    public double movementSpeedBonus;

    public WeaponInstanceComponent() {
        enchantmentSlots = new ArrayList<>();
    }

    public static ComponentType<EntityStore, WeaponInstanceComponent> getComponentType() {
        if (componentType == null) throw new IllegalStateException("WeaponInstanceComponent not registered.");
        return componentType;
    }

    public static void setComponentType(ComponentType<EntityStore, WeaponInstanceComponent> type) {
        componentType = type;
    }

    @Override
    public WeaponInstanceComponent clone() {
        WeaponInstanceComponent c = new WeaponInstanceComponent();
        c.archetypeId = this.archetypeId;
        c.quality = this.quality;
        c.level = this.level;
        c.weaponTag = this.weaponTag;
        c.enchantmentSlots = new ArrayList<>(this.enchantmentSlots);
        c.damageMultiplier = this.damageMultiplier;
        c.healthBonus = this.healthBonus;
        c.movementSpeedBonus = this.movementSpeedBonus;
        return c;
    }
}
