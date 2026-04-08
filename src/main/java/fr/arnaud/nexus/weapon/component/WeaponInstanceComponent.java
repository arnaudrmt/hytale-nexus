package fr.arnaud.nexus.weapon.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.arnaud.nexus.weapon.data.EnchantmentSlot;
import fr.arnaud.nexus.weapon.data.WeaponRarity;
import fr.arnaud.nexus.weapon.data.WeaponTag;

import java.util.ArrayList;
import java.util.List;

public final class WeaponInstanceComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, WeaponInstanceComponent> componentType;

    public String archetypeId;
    public WeaponRarity rarity;
    public WeaponTag weaponTag;
    public float damageMultiplier;
    public List<EnchantmentSlot> enchantmentSlots;
    public List<ActiveEnchantmentState> activeStates;

    public WeaponInstanceComponent() {
        enchantmentSlots = new ArrayList<>();
        activeStates = new ArrayList<>();
    }

    public static ComponentType<EntityStore, WeaponInstanceComponent> getComponentType() {
        return componentType;
    }

    public static void setComponentType(ComponentType<EntityStore, WeaponInstanceComponent> type) {
        componentType = type;
    }

    @Override
    public WeaponInstanceComponent clone() {
        WeaponInstanceComponent c = new WeaponInstanceComponent();
        c.archetypeId = this.archetypeId;
        c.rarity = this.rarity;
        c.weaponTag = this.weaponTag;
        c.damageMultiplier = this.damageMultiplier;
        c.enchantmentSlots = new ArrayList<>(this.enchantmentSlots);
        c.activeStates = new ArrayList<>(this.activeStates);
        return c;
    }

    public record ActiveEnchantmentState(String enchantmentId, int level, boolean flowGateActive) {
        public ActiveEnchantmentState withGateActive(boolean active) {
            return new ActiveEnchantmentState(enchantmentId, level, active);
        }
    }
}
